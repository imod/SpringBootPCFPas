
def updateDatabase(zipFile, credentialsId, connectionUrl) {
	withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: credentialsId, usernameVariable: 'DB_USER', passwordVariable: 'DB_PWD']]) {

	  env.URL = connectionUrl
    def workingdir = "db_update_scripts"
	  def fileName = getFileName(zipFile)

	  sh """
	    rm -rf $workingdir || true
	    mkdir $workingdir
		 	mv $zipFile $workingdir/$fileName
	  """

      dir(workingdir){

	      sh """
		     	pwd
	        ls -ltr
	        unzip $fileName
			 		ls -ltr
          mvn -f pom.xml process-resources -Pupdate-db -Dliquibase.promptOnNonLocalDatabase=false -Dliquibase.username=${env.DB_USER} -Dliquibase.password=${env.DB_PWD} -Dliquibase.url=${env.URL}
        """

	     def v = version('pom.xml')
	     echo "Updated DB to version: ${v}"
	  }
    }
}

def version(pom) {
  def matcher = readFile(pom) =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}

def getFileName(pathToFile) {
	//def index = pathToFile.lastIndexOf('/')
	//return pathToFile.substring(index + 1)
	def uniqueid = new Random().nextLong();
	def uniquename = ".filename${uniqueid}"
	sh "basename $pathToFile > ${uniquename}"
	def commandOut = readFile uniquename
	sh "rm ${uniquename}"
	return commandOut
}

def deployApp(appName, appFile, manifestFile, servicesToBind, springProfile) {
    sh "cf push $appName -p $appFile -f $manifestFile -t 180 --no-start"
    bindServices(appName, servicesToBind)
    setBasicEnvironment(appName, appName, springProfile, env.ENCRYPT_KEY, env.CONFIG_PASSWORD)
    sh "cf start $appName"
}

def blueGreenDeployment(mainCfDomain, springConfigPassword, springConfigEncryptionKey, cfHostName, springProfiles, cfManifest, appFile, publicDomains, servicesToBind, instancesToStart) {

	assert mainCfDomain != null : "CF domain must be given!"
	String cfDomain = mainCfDomain
	String services = servicesToBind ? servicesToBind : ""
	String instances = instancesToStart > 0 ? instancesToStart : 1
	String mainPublicDomain = getFirstToken(publicDomains)
	String blueApp = "${cfHostName}-blue"
	String greenApp = "${cfHostName}-green"

	String inactiveApp = ""
	def newInstallation = false;

	echo "checking route mapping to $mainPublicDomain"
	String activeApp = getAppForDomain(mainPublicDomain).trim()

	if (mainPublicDomain.equals(activeApp)) {
		echo "WARN: $mainPublicDomain used, but not assigned to any app"
		activeApp = ""
	}


	echo "active: $activeApp ${activeApp == activeApp}"
	echo "blue: $blueApp ${activeApp == blueApp}"
	echo "green: $greenApp ${activeApp == greenApp}"
	if (activeApp == blueApp) {
		inactiveApp = greenApp
	} else if (activeApp == greenApp) {
		inactiveApp = blueApp
	} else if (!activeApp) {
		inactiveApp = greenApp
		newInstallation = true
	} else {
		echo "ERROR: not able to detect current installation status! active app checked: $activeApp"
		return 1
	}

	echo """
	    spring profiles: $springProfiles
	    active: $activeApp
	    inactive: $inactiveApp
	    green: $greenApp, blue: $blueApp
	    pushing $appFile with $cfManifest as $inactiveApp in replacement for $activeApp
	    main pulic domain: $mainPublicDomain
	    pulic domains: $publicDomains
	    cf domain: $cfDomain, cf host: $cfHostName
	"""

	if (newInstallation) {
		echo "pushing new installation..."
		sh "cf push $inactiveApp -n $inactiveApp -p $appFile -f $cfManifest -i $instances -t 180 --no-start"
		bindServices(inactiveApp, services)
		setBasicEnvironment(inactiveApp, cfHostName, springProfiles, springConfigEncryptionKey, springConfigPassword)
		sh "cf start ${inactiveApp}"

		routeDomains(inactiveApp, publicDomains)
	    sh "cf map-route $inactiveApp $cfDomain -n $cfHostName"
	} else {
		echo "pushing update as $inactiveApp..."
		sh "cf push $inactiveApp -n $inactiveApp -p $appFile -f $cfManifest -i $instances -t 180 --no-start"
		
		// create a temp route we can use to test if app is online
		def tempRoute="${inactiveApp}-temp"
		sh "cf map-route $inactiveApp $cfDomain -n $tempRoute"
		
	    bindServices(inactiveApp, services)
	    setBasicEnvironment(inactiveApp, cfHostName, springProfiles, springConfigEncryptionKey, springConfigPassword)
	    sh "cf start ${inactiveApp}"

		// wait for the new app to start
	    def testUrl="https://${tempRoute}.${cfDomain}/api/v1.0/commons/version"
		  waitUntilOnline(testUrl)

	    // map public routes
	    // (this will make the new app available)
	    routeDomains(inactiveApp, publicDomains)
	    sh "cf map-route $inactiveApp $cfDomain -n $cfHostName"
	    // sh "cf delete-route $cfDomain -n $tempRoute -f" //do not delete temp route, because actuator integration to CF will not work without

	    // remove old app
	    // (also unmap routes, otherwise these get reassigned with new installation automatically)
	    sh "cf unmap-route $activeApp $cfDomain -n $cfHostName"
	    unrouteDomains(activeApp, publicDomains)
	    sh "cf delete $activeApp -f"
	    //sh 'cf delete-orphaned-routes'
	}

	echo "...done"
}

def setBasicEnvironment(app, cfHostName, springProfiles, springConfigEncryptionKey, springConfigPassword) {
	sh """
      cf set-env $app NEW_RELIC_APP_NAME $cfHostName
      cf set-env $app SPRING_PROFILES_ACTIVE $springProfiles
      cf set-env $app ENCRYPT_KEY $springConfigEncryptionKey
      cf set-env $app SPRING_CLOUD_CONFIG_PASSWORD $springConfigPassword
    """
}

def waitUntilOnline(url) {
	// give it 180 seconds (60 x 3sec = 3min) time to start
  for(int i = 0; i < 60; i++){
		echo "check $url"
		def resp = sh(returnStdout: true, script: "curl -sL -XGET -w \"%{http_code}\" $url -o /dev/null")
		if(resp.equals("200")) {
			echo "$url is online"
			break
		} else {
			sleep time: 3, unit: 'SECONDS'
		}
	}
}

def routeDomains(appName, domains) {
    if (!"".equals(domains)) {
       def doms=domains.split(',')
       for(int i = 0; i < doms.length; i++){
       	  def dom = doms[i]
       	  def domandpath = dom.split('/')
       	  if(domandpath.length == 2) {
       	  	echo "map route ${domandpath[0]} with path ${domandpath[1]} to $appName"
       	  	sh "cf map-route $appName ${domandpath[0]} --path ${domandpath[1]}"
       	  } else {
          	echo "map route ${dom} to $appName"
          	sh "cf map-route $appName ${dom}"
          }
       }
    }
}

def unrouteDomains(appName, domains) {
    if (!"".equals(domains)) {
       def doms=domains.split(',')
	   for(int i = 0; i < doms.length; i++){
	   	  def dom = doms[i]
       	  def domandpath = dom.split('/')
       	  if(domandpath.length == 2) {
       	  	echo "unmap route ${domandpath[0]} with path ${domandpath[1]} from $appName"
       	  	sh "cf unmap-route $appName ${domandpath[0]} --path ${domandpath[1]}"
       	  } else {
            echo "unmap route ${dom} from $appName"
            sh "cf unmap-route $appName ${dom}"
          }
       }
    }
}

def bindServices(appName, services) {
    if (!"".equals(services)) {
       def srvs=services.split(',')
	   for(int i = 0; i < srvs.length; i++){
          echo "bind ${srvs[i]} to $appName"
          sh "cf bind-service $appName ${srvs[i]}"
       }
    }
}

String getFirstToken(stringValueWithCommas) {
	def tokens = stringValueWithCommas.split(",")
	return tokens[0]
}

String getAppForDomain(domain) {
	def commandOut = sh(returnStdout: true, script: 'cf routes')
  def line = getLineContaining(commandOut, domain)
  def appName = ""
  if(line) {
   String[] tokens = line.split(" |\t");
     appName = tokens[tokens.length - 1]
  }
	return appName
}

def checkAllServiceExist(services) {
    if (!"".equals(services)) {
		  def cfServices = sh(returnStdout: true, script: 'cf services')
      def srvs=services.split(',')
	    for(int i = 0; i < srvs.length; i++){
		 		def srvExists = textContainsTokenAtLineStart(cfServices, srvs[i])
				if(!srvExists) {
					currentBuild.result = 'FAILURE'
					error "service ${srvs[i]} does not exists!!!"
				}
      }
    }
}


def createNotExistingDomains(space, domains) {
    if (!"".equals(domains)) {
	  	def cfDomains = sh(returnStdout: true, script: 'cf domains')
      	def doms=domains.split(',')
		for(int i = 0; i < doms.length; i++){
			def dom = doms[i]
			def delimindex = dom.lastIndexOf('/')
			if(delimindex != -1) {
				dom = dom.substring(0, delimindex)
			} 
	   			
   			def domainExists = textContainsTokenAtLineStart(cfDomains, dom)
			if(domainExists){
				echo "domain ${dom} already exists"
			} else {
				sh "cf create-domain ${space} ${dom}"
			}
      	}
    }
}

def join(delimiter, String... strings) {
	def result = ""
	for(int i = 0; i < strings.length; i++){
		if(strings[i]){
			if(result){
				result = result + "," + strings[i]
			} else {
				result = strings[i]
			}
		}
	}
	return result
}

@NonCPS
String getLineContaining(text, token) {
    def matcher = text =~ "(.*${token}.*)"
    return matcher ? matcher[0][1] : null
}

@NonCPS
boolean textContainsTokenAtLineStart(text, token) {
    def matcher = text =~ /(?m)^(${token})[\n\r\s]/
    return matcher ? true : false
}

// make sure the scrit itself can be referenced
return this;
