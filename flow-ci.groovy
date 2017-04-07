
def branch = 'develop'

def config = [cfApiEndpoint: "https://api.run.pivotal.io",
              cfOrg: "yooture",
              cfSpace: "ci",
              springProfile: "ci",
              mainCfDomain: "cfapps.io",
              apiAppName: "yoo-actuator-test",
              apiPublicDomains: 'ci-actuator.yooture.info',
              apiInstances: 1,
              apiServicesToBind: 'api-logs',
              ]

def deploy
def wrapper

node() {

  stage 'Checkout'
  git branch: branch, url: 'git@bitbucket.org:yooture/yoo-actuator-test.git'

  sh """
      git checkout $branch
      git clean -f -n
      git clean -f
      git pull origin $branch
      git status
  """

  stage 'Build'
  deploy = load 'deploy.groovy'
  wrapper = load 'deploy_wrapper.groovy'
  installTools()

  sh 'mvn clean install -B -Dmaven.test.failure.ignore'

  // install on CI
  stage name: 'Install CI', concurrency: 1

  wrapper.installApps(deploy, config)

  // inform team about a failed execution
  if(currentBuild.result != null && !"SUCCESS".equals(currentBuild.result)) {
      hipchatSend color: 'RED', room: 'yooture', notify: true, message: "@all please fix!!! ${currentBuild.displayName}: ${currentBuild.result} - ${currentBuild.absoluteUrl}"
  } else {
      hipchatSend color: 'GREEN', room: 'yooture', notify: false, message: "CI installation finished, triggering integration tests..."
      build job: 'yooture.it', parameters: [[$class: 'StringParameterValue', name: 'TEST_SCRIPT_TO_RUN', value: 'test-ci']], wait: false
  }

}

def installTools() {
    env.JAVA_HOME="${tool 'Oracle JDK 1.8 (latest)'}"
    env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
    sh 'java -version'
    env.PATH="${tool 'Maven (latest)'}/bin:${env.PATH}"
    env.PATH="${tool 'CF_CLI_6'}:${env.PATH}"
}
