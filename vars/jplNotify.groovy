/**
Notify using multiple methods: slack, email

Parameters:

* cfg jplConfig class object
* String summary The summary of the message (blank to use defaults)
* String message The message itself (blank to use defaults)

cfg usage:

* cfg.recipients.*
*/
def call(cfg, String summary = '', String message = '') {
    jplConfig.checkInitializationStatus(cfg)
    switch (currentBuild.result) {
        case 'ABORTED':
            slackColor = 'warning'
            break;
        case 'UNSTABLE':
            slackColor = 'warning'
            break;
        case 'FAILURE':
            slackColor = 'danger'
            break;
        default: // SUCCESS and null
            slackColor = 'good'
            break;
    }
    summary = jplConfig.summary(summary)
    message = jplConfig.description(message)
    slackChannels = cfg.recipients.slack
    emailRecipients = cfg.recipients.email
    if (jplConfig.resultStatus() == 'SUCCESS') {
        slackChannels = ''
        emailRecipients = ''
    }
    if (slackChannels != "") {
        slackSend channel: slackChannels, color: slackColor, message: "${summary}\n${message}"
    }
    if (emailRecipients != "") {
        //def to = emailextrecipients([[$class: 'DevelopersRecipientProvider'],[$class: 'CulpritsRecipientProvider'],[$class: 'UpstreamComitterRecipientProvider'],[$class: 'FirstFailingBuildSuspectsRecipientProvider'],[$class: 'FailingTestSuspectsRecipientProvider']])
        //mail to: to, cc: emailRecippients, subject: summary, body: message
        emailext(body: message, mimeType: 'text/html',
            replyTo: '$DEFAULT_REPLYTO', subject: summary,
            to: emailRecipients, recipientProviders: [[$class: 'DevelopersRecipientProvider'],[$class: 'CulpritsRecipientProvider'],[$class: 'UpstreamComitterRecipientProvider'],[$class: 'FirstFailingBuildSuspectsRecipientProvider'],[$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'RequesterRecipientProvider']])
    }
}
