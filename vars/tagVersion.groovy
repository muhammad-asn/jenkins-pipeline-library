#!/usr/bin/groovy

def call(version, sshCreds){
  def repoName = sh (
    returnStdout: true,
      script: """
        basename \$(git remote get-url origin) | sed -e "s/.git\$//"
      """
  )
  repoName = repoName.trim()
  sh "git config --global core.sshCommand 'ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no'"
  if (!isSnapshot(version) && !isTagged(version) && isMaster()) {
    sshagent([sshCreds]){
      sh """
        git status
        git config --global user.email "build@accelbyte.net"
        git config --global user.name "Jenkins Build"
        git tag -a ${version} `git log --pretty=format:'%h' -n 1` -m "Tagged by Jenkins"
        git push origin ${version}
      """
    }
  } else if (!isSnapshot(version) && isTagged(version)) {
    echo "Already Tagged"
  } else {
    echo "This is SNAPSHOT version"
  }
}

def isTagged(String version) {
  def tagged = sh (
    returnStdout: true,
    script: """
      if git rev-parse ${version} &> /dev/null ; \
      then echo true; else echo false; fi
    """
  )
  if (tagged.trim() == "true") return true
  return false
}

def isMaster() {
  def branch = sh (
    returnStdout: true,
    script: """
      git name-rev `git rev-parse --verify HEAD`
    """
  )
  if (branch.contains("master")) return true
  return false
}

def isSnapshot(String version) {
  if (version.contains("SNAPSHOT")) return true
  return false
}