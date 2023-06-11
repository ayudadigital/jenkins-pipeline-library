/**
  Global config variables

  Parameters:
  * String projectName
  * String targetPlatform
  * String jiraProjectKey
  * HashMap recipients

  ---------------
  cfg definitions
  ---------------
  * String  projectName             Project alias / codename (with no spaces)       (default: "project")
  * String  BRANCH_NAME             Branch name                                     (default: env.BRANCH_NAME)
  * String  headBranch              Head branch name                                (default: "master")
  * String  targetPlatform          Target platform, one of these                   (default: "any")
  * boolean notify                  Automatically send notifications                (default: true)
  * String  archivePattern          Atifacts archive pattern
  * String  releaseTag              Release tag for branches like "release/vX.Y.Z"  (default: related tag or "" on non-release branches)
                                    The releaseTag for this case is "vX.Y.Z"
  * String releaseTagNumber         Release tag for branches like "release/vX.Y.Z"  (default: related tag or "" on non-release branches)
                                    only the number part. Refers to "X.Y.Z" without the starting "v"
  * String makeReleaseCredentialsID ID of the credentials that makeRelease function (default: 'jpl-ssh-credentials')
                                    will use. Should be SSH credentials

  * Hashmap repository: repository parametes. You can use it for non-multibranch repository
        String url                  URL                                             (default: '')
        String branch               branch                                          (default: '')

  * Hashmap recipients: Recipients used in notifications
        String recipients.slack     List of slack channels, comma separated         (default: "")
        String recipients.email     List of email address, comma separated          (default: "")

  * HashMap sonar: Sonar scanner configuration
        String sonar.toolName                 => Tool name configured in Jenkins    (default: "SonarQube")
        String sonar.abortIfQualityGateFails  => Tool name configured in Jenkins    (default: true)

  * HashMap jira: JIRA configuration
        String jira.projectKey      JIRA project key                                (default: "")
        object jira.projectData     JIRA project data                               (default: "")
  
  * Hashmap commitValidation: Commit message validation configuration on PR's, using project https://github.com/willsoto/validate-commit
        boolean enabled             Commit validation enabled status                (default: true)
        String preset               One of the willsoto validate commit presets     (default: 'eslint')
        int quantity                Number of commits to be checked                 (default: 1)

  * Hashmap changelog: Changelog building configuration
        boolean enabled             Automatically build changelog file              (default: false)
                                    * Archive as artifact build on every commit

*/
def call (projectName = 'project', targetPlatform = 'any', jiraProjectKey = '', recipients = [slack:'', email:''], headBranch = "master") {
    cfg = [:]
    //
    if (env.BRANCH_NAME == null) {
        cfg.BRANCH_NAME = 'develop'
    }
    else {
        cfg.BRANCH_NAME = env.BRANCH_NAME
    }
    cfg.headBranch                                  = headBranch
    cfg.projectName                                 = projectName
    cfg.targetPlatform                              = targetPlatform
    cfg.releaseTag                                  = (cfg.BRANCH_NAME.startsWith('release/v') || cfg.BRANCH_NAME.startsWith('hotfix/v')) ? cfg.BRANCH_NAME.tokenize("/")[1] : ""
    cfg.releaseTagNumber                            = (cfg.BRANCH_NAME.startsWith('release/v') || cfg.BRANCH_NAME.startsWith('hotfix/v')) ? cfg.BRANCH_NAME.tokenize("/")[1].substring(1) : ""
    cfg.makeReleaseCredentialsID                    = "jpl-ssh-credentials"

    //
    cfg.repository = [:]
        cfg.repository.url = ''
        cfg.repository.branch = ''

    //
    cfg.notify                                      = true
    cfg.recipients                                  = recipients
    cfg.recipients.slack = cfg.recipients.slack ?: ''
    cfg.recipients.email = cfg.recipients.email ?: ''

    //
    cfg.sonar = [:]
        cfg.sonar.toolName                          = "SonarQube"
        cfg.sonar.abortIfQualityGateFails           = true

    //
    cfg.jira = [:]
        cfg.jira.projectKey                         = jiraProjectKey
        cfg.jira.projectData                        = ''

    //
    cfg.commitValidation                            = [:]
        cfg.commitValidation.enabled                = true
        cfg.commitValidation.preset                 = "eslint"
        cfg.commitValidation.quantity               = 1

    //
    cfg.changelog                                   = [:]
        cfg.changelog.enabled                       = false

    //-----------------------------------------//

    //
    cfg.dockerFunctionPrefix                        = "docker run -i --rm "

    //
    cfg.flags = [:]
        cfg.flags.isJplConfigured                   = true
        cfg.flags.isJplStarted                      = false
        cfg.flags.wereScriptsDownloaded             = false

    //-----------------------------------------//

    // Do some checks
    jplJIRA.checkProjectExists(cfg)

    // Return config HashMap
    return cfg
}

// Get jpl-scripts
def downloadScripts(cfg) {
    if (cfg.flags.wereScriptsDownloaded) {
        if (!fileExists('ci-scripts/.jpl-scripts/README.md')) {
            unstash 'jpl-scripts'
        }
    }
    else {
        sh "rm -rf ci-scripts/.jpl-scripts && mkdir -p ci-scripts/.temp && cd ci-scripts/.temp/ && wget -q -O - https://github.com/red-panda-ci/jpl-scripts/archive/master.zip | jar xvf /dev/stdin > /dev/null && chmod +x jpl-scripts-master/bin/*.sh && mv jpl-scripts-master ../.jpl-scripts"
        stash includes: 'ci-scripts/.jpl-scripts/**/*', name: 'jpl-scripts'
        cfg.flags.wereScriptsDownloaded = true
    }
}

def checkInitializationStatus(cfg) {
    if (!cfg.flags.isJplConfigured) {
        error ("ERROR: You should call to jplConfig first")
    }
}


def getBuildTimeout(cfg, String project = 'generic') {
    switch (action) {
        case 'GetRequestInfo':
            job = "/Operations/operation_catalog/tools/sm/get"
            break
        case 'GetRequestsByGroup':
            job = "/Operations/operation_catalog/tools/sm/get_group_requests"
            break
        case 'AssignRequestedOperator':
            job = "/Operations/operation_catalog/tools/sm/assign_requested_operator"
            break
        case 'ResolveRequest':
            job = "/Operations/operation_catalog/tools/sm/resolve_request"
            break
    }
}

/**
  Return result status of current build
  Return "SUCCESS" on unknown case
*/
def resultStatus() {
    return (currentBuild.result == null ? 'SUCCESS' : currentBuild.result)
}
/**
  Return branch info of current job (blank if none)
  Used in issues and notifications
 */
def branchInfo() {
    return (env.BRANCH_NAME == null ? '' : " (branch ${env.BRANCH_NAME})")
}
/**
  Summary field for issues and notifications
 */
def summary(summary = '') {
    return (summary == '' ? "Job [${env.JOB_NAME}] [#${env.BUILD_NUMBER}] finished with ${this.resultStatus()}${this.branchInfo()}" : summary)
}
/**
  Description text for issues and notifications
 */
def description(description = '') {
    return (description == '' ? "View details on ${env.BUILD_URL}console" : description)
}
