#!groovy

@Library('github.com/ayudadigital/jenkins-pipeline-library@refactor') _

// Initialize global config
cfg = jplConfig('jpl', 'groovy', '', [email: env.CI_NOTIFY_EMAIL_TARGETS])

def publishDocumentation() {
    sh """
    git checkout ${env.BRANCH_NAME}
    make
    git add README.md vars/*.txt
    git config --local user.name 'Jenkins'
    git config --local user.email 'jenkins@ci'
    git commit -m 'Docs: Update README.md and Jenkins doc help files' || true
    git push -u origin ${env.BRANCH_NAME} || true
    """
}

pipeline {
    agent { label 'docker' }

    stages {
        stage ('Initialize') {
            steps  {
                jplStart(cfg)
            }
        }
        stage('Sonarqube Analysis') {
            when { expression { (env.BRANCH_NAME == 'develop') || env.BRANCH_NAME.startsWith('PR-') } }
            steps {
                echo "Temporary disabled"
                //jplSonarScanner(cfg)
            }
        }
        stage ('Test') {
            steps  {
                sh 'bin/test.sh'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'test/reports/**/*.*', fingerprint: true, allowEmptyArchive: true
                }
            }
        }
        stage ('Make release'){
            when { branch 'release/new' }
            steps {
                publishDocumentation()
                jplMakeRelease(cfg, true)
            }
        }
    }

    post {
        always {
            jplPostBuild(cfg)
        }
        failure {
            deleteDir() /* clean up workspace on failure */
        }
    }

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(artifactNumToKeepStr: '20',artifactDaysToKeepStr: '30'))
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }
}
