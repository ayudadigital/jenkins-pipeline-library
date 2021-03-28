/**

Calculate the next release tag using "get-next-release-number" docker command https://github.com/ayudadigital/dc-get-next-release-number

Parameters:
* cfg jplConfig class object

cfg usage:

* cfg.makeReleaseCredentialsID
*/
def call(cfg) {
    sshagent (credentials: [cfg.makeReleaseCredentialsID]) {

        sh """
        git clean -f -d
        git config --unset remote.origin.fetch
        git config --add remote.origin.fetch +refs/heads/*:refs/remotes/origin/*
        git fetch -p
        """
        nextReleaseNumber = sh (script: "kd get-next-release-number .", returnStdout: true).trim()
        return nextReleaseNumber
    }
}
