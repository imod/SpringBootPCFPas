def installApps(deployScript, config) {

   withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'SPRING_CLOUD_CONFIG_USER', usernameVariable: 'CONFIG_USERNAME', passwordVariable: 'CONFIG_PASSWORD'],
                    [$class: 'StringBinding', credentialsId: 'SPRING_CLOUD_CONFIG_ENCRYPT_KEY', variable: 'ENCRYPT_KEY'],
                    [$class: 'UsernamePasswordMultiBinding', credentialsId: 'CLOUDFOUNDRY_USER', usernameVariable: 'CF_USERNAME', passwordVariable: 'CF_PASSWORD']]) {


    sh """
        cf login -a ${config.cfApiEndpoint} -u $env.CF_USERNAME -p $env.CF_PASSWORD -o ${config.cfOrg} -s ${config.cfSpace}
        cf apps
    """

    // make sure all public domains are on CF available
    def allPublicDomains = deployScript.join(',', config.apiPublicDomains, config.hooksPublicDomains)
    deployScript.createNotExistingDomains(config.cfOrg, allPublicDomains)

    def allServiceToBind = deployScript.join(',', config.apiServicesToBind, config.hooksServicesToBind, config.batchServicesToBind, config.adminServicesToBind)
    deployScript.checkAllServiceExist(allServiceToBind)

    parallel (
      "yoo-actuator-test": {
        // API app needs to be up 24/7, therefore do a green blue deployment
        deployScript.blueGreenDeployment(config.mainCfDomain, env.CONFIG_PASSWORD, env.ENCRYPT_KEY, config.apiAppName, config.springProfile, 'manifest.yml', './target/springbootpcfpas-0.0.1-SNAPSHOT.jar', config.apiPublicDomains, config.apiServicesToBind, config.apiInstances)
      },
      failFast: true
    )

    sh """
      cf delete-orphaned-routes -f
      echo '==============================='
      cat TODO.md
      echo '==============================='
    """
   }
}


// make sure the scrit itself can be referenced
return this;
