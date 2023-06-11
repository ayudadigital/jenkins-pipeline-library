[![Build Status](https://jenkins.ticparabien.org/buildStatus/icon?job=ayudadigital%2Fjenkins-pipeline-library%2Fdevelop)](https://jenkins.ticparabien.org/job/ayudadigital/job/jenkins-pipeline-library/job/develop/)

## Description

Library with a set of helpers to be used in Jenkins Scripted or Declarative Pipelines

This helpers are designed to be used in "Multibranch Pipeline" Jenkins job type, with "git flow" release cycle and at least with the following branches:

* develop
* master

## Usage

Add this line at the top of your Jenkinsfile

    @Library('github.com/ayudadigital/jenkins-pipeline-library') _

Then you can use the helpers in your script

* Scripted Pipeline Example

TBD

* Declarative Pipeline example (backend)

```groovy
#!groovy

@Library('github.com/ayudadigital/jenkins-pipeline-library') _

// Initialize cfg
cfg = jplConfig('project-alias', 'backend', 'JIRAPROJECTKEY', [slack:'#the-project,#integrations', email:'the-project@example.com,dev-team@example.com,qa-team@example.com'])

// The pipeline
pipeline {

    agent none

    stages {
        stage ('Initialize') {
            agent { label 'docker' }
            steps  {
                jplStart(cfg)
            }
        }
        stage ('Docker push') {
            agent { label 'docker' }
            steps  {
                jplDockerPush(cfg, 'the-project/docker-image', 'https://registry.hub.docker.com', 'dockerhub-credentials', 'dockerfile-path')
            }
        }
        stage('Test') {
            agent { label 'docker' }
            when { expression { (env.BRANCH_NAME == 'develop') || env.BRANCH_NAME.startsWith('PR-') } }
            steps {
                jplSonarScanner(cfg)
            }
        }
        stage ('Make release') {
            when { branch 'release/new' }
            steps {
                jplMakeRelease(cfg)
            }
        }
        stage ('PR Clean') {
            agent { label 'docker' }
            when { branch 'PR-*' }
            steps {
                deleteDir()
            }
        }
    }

    post {
        always {
            jplPostBuild(cfg)
        }
    }

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(artifactNumToKeepStr: '20',artifactDaysToKeepStr: '30'))
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'DAYS')
    }
}
```

## Helpers set

### jplBuildChangelog

  Build changelog file based on the commit messages

  Parameters:

  * cfg jplConfig class object
  * String format Changelog format: "md" or "html"          (defaults to "md")
  * String filename Changelog file name                     (defaults to "CHANGELOG.md")

  cfg usage:

  * cfg.BRNACH_NAME

  This function need the installation of "kd" script of docker-command-launcher
  Review https://github.com/ayudadigital/docker-command-launcher project


### jplConfig

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


### jplDockerBuild

Docker image build

Parameters:

* cfg jplConfig class object
* String dockerImageName Name of the docker image, defaults to cfg.projectName
* String dockerImageTag Tag of the docker image, defaults to "latest"
* String dockerfilePath The path where the Dockerfile is placed, default to the root path of the repository

cfg usage:

* cfg.projectName

### jplDockerPush

Docker image build & push to registry

Parameters:

* cfg jplConfig class object
* String dockerImageName Name of the docker image, defaults to cfg.projectName
* String dockerImageTag Tag of the docker image, defaults to "latest"
* String dockerfilePath The path where the Dockerfile is placed, default to the root path of the repository
* String dockerRegistryURL The URL of the docker registry. Defaults to https://registry.hub.docker.com
* String dockerRegistryJenkinsCredentials Jenkins credentials for the docker registry

cfg usage:

* cfg.projectName

### jplGetNextReleaseNumber


Calculate the next release tag using "get-next-release-number" docker command https://github.com/ayudadigital/dc-get-next-release-number

Parameters:
* cfg jplConfig class object

cfg usage:

* cfg.makeReleaseCredentialsID

### jplJIRA

JIRA management

Parameters:

* cfg jplConfig class object



/*
### jplMakeRelease


Make new release automatically

The function will:

- Calculate the next release tag using "get-next-release-number" docker command https://github.com/ayudadigital/dc-get-next-release-number
- Build the changelog
- Append a new line in "jpl-makeRelease.log" file with the release information (tag name and timestamp)
- Publish the changes to the repository (`git push`) on the develop branch
- Publish the release tag to the repository using Jenkins SSH credentials

Abort the build if:

- The repository is on the "develop" branch. Or...
- The promoteBuild.enabled flag is not true (DEPRECATED: always promote build)

You can use this function with a branch named "release/next", so it will do all the job for you when you do a push to the repository.

Aditionally, the function will append a line to the "jpl-makeRelease.log" file with the release tag and the curremt timestam

Parameters:
* cfg jplConfig class object

cfg usage:

* cfg.makeReleaseCredentialsID
* cfg.notify
* cfg.recipients

### jplNotify

Notify using multiple methods: slack, email

Parameters:

* cfg jplConfig class object
* String summary The summary of the message (blank to use defaults)
* String message The message itself (blank to use defaults)

cfg usage:

* cfg.recipients.*

### jplPostBuild

Post build tasks

Parameters:

* cfg jplConfig class object

cfg usage:

* cfg.targetPlatform
* cfg.notify
* cfg.jiraProjectKey

Place the jplPostBuild(cfg) line into the "post" block of the pipeline like this

    post {
        always {
            jplPostBuild(cfg)
        }
    }

### jplSonarScanner

Launch SonarQube scanner

Parameters:

* cfg jplConfig class object

cfg usage:

* cfg.sonar.*

To use the jplSonarScanner() tool:

* Configure Jenkins with SonarQube >= 6.2
* Configure a webhook in Sonar to your jenkins URL <your-jenkins-instance>/sonarqube-webhook/ (https://jenkins.io/doc/pipeline/steps/sonar/#waitforqualitygate-wait-for-sonarqube-analysis-to-be-completed-and-return-quality-gate-status)

### jplStart

Start library activities

This helper should be executed as first step of the pipeline.

* Execute for the jplValidateCommitMessages on Pull Request, breaking the build if the messages don't complaint with the parse rules
* Execute jplBuildChangelog and attach the CHANGELOG.html as artifact of the build

Parameters:

* cfg jplConfig class object

jpl usage:

* jplBuildChangeLog
* jplValidateCommitMessages

cfg usage:

* cfg.targetPlatform
* cfg.flags.isJplStarted

### jplValidateCommitMessages

Validate commit messages on PR's using https://github.com/willsoto/validate-commit project

- Check a concrete quantity of commits on the actual PR on the code repository
- Breaks the build if any commit don'w follow the preset rules

Parameters:

* cfg jplConfig class object
* int quantity Number of commits to check
* String preset Preset to use in validation
  Should be one of the supported presets of the willsoto validate commit project:
  - angular
  - atom
  - eslint
  - ember
  - jquery
  - jshint

cfg usage:

* cfg.commitValidation.*

## Dependencies

You should consider the following configurations:

### Jenkins service

* Install Java "jre" and "jdk"

```console
$ sudo apt-get install default-jre default-jdk

[...]

```

* Configure "git" to be able to make push to the repositories.
  * Configure git credentials (username, password) <https://git-scm.com/docs/git-credential-store> for use with "https" remote repositories and set "store" as global config option, with global user name and global email

    ```console
    $ git config --global credential.helper store
    $ git config --global user.name "User Name"
    $ git config --global user.email "your@email.com"
    $ cat .gitconfig
    [user]
        email = your@email.com
        name = User Name
    [push]
        default = simple
    [credential]
        helper = store
    $ cat ~/.git-credentials
    https://your%40email.com:fake-password@github.com
    ```

  * Configure ssh public key for use with "ssh" remote repositories.

  ```console

    $ ssh-keygen
    Generating public/private rsa key pair.
    Enter file in which to save the key (/var/lib/jenkins/.ssh/id_rsa): 
    Enter passphrase (empty for no passphrase):
    Enter same passphrase again:
    Your identification has been saved in /var/lib/jenkins/.ssh/id_rsa.
    Your public key has been saved in /var/lib/jenkins/.ssh/id_rsa.pub.
    The key fingerprint is:
    SHA256:+IKayZj7m06twLgUSWyb2jLINPjzp6uQeeyA8nmYjqk server@jenkins
    The key's randomart image is:
    +---[RSA 2048]----+
    |                 |
    |.                |
    | +               |
    |+ +    .         |
    |.B    . S        |
    |O*o. . .         |
    |#+Boo . .        |
    |o^oX. ..         |
    |E=^+++           |
    +----[SHA256]-----+
    jenkins@server:~$ cat .ssh/id_rsa.pub
    ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC72EdmruDtEoqF3BK7JPjgVGMfL7hnPVymdUEt76gk1U/sSaYsijbqxyhSbdp/8W7l1dwGA1Vs7cAn15qVzbUoJzmmM1rm7wPOBU7oBH1//oopA5U1XauXRuKWFQ8LDbjdaHBriBP4IyIG9fS+afgRwDlwlxx2mKuWhuYlHbBAxGwwDpxtTnvJ9JAnWG5eJ+8cXJ2PaIBlhc8jkjWkvLOnAWx729LdFQqWrikY5YwtNKw0CnU5XGBP96GcyR+k7PPkdr8LcVCewE042n6pw43e3H4GRlWU2w/nj/JniF6Tyx76hxSX9UMFiCKVXqM8blftqn9H7WGStt0b1pPhwtGT server@jenkins
  ```

* Install docker and enable Jenkins syste user to use the docker daemon.
* Install this plugins:
  * AnsiColor
  * Bitbucket Branch Source
  * Bitbucket Plugin
  * Blue Ocean
  * Copy Artifact Plugin
  * File Operations
  * Github Branch Source
  * Github Plugin
  * Git Plugin
  * HTML Publisher
  * JIRA Pipeline Steps, if you want to use a JIRA project
  * Pipeline
  * Slack Notification, if you want to use Slack as notification channel
  * SonarQube Scanner, if you want to use SonerQube as quality gate with jplSonarScanner
  * Timestamper
* Setup Jeknins in "Configuration" main menu option
  * Enable the checkbox "Environment Variables" and add the following environment variables with each integration key:
    * JIRA_SITE
  * Put the correct Slack and JIRA credentials in their place (read the howto's of the related Jenkins plugins)
