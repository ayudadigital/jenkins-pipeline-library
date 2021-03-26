ARG TAG=2.249.1
FROM ayudadigital/jenkins-dind:${TAG}
RUN apk add --no-cache rsync openjdk8 jq
COPY --chown=jenkins:jenkins config /var/jenkins_home/
RUN chmod 600 /var/jenkins_home/.ssh/* && \
    chmod 700 /var/jenkins_home/.ssh
USER jenkins
RUN java -jar /usr/share/jenkins/jenkins.war --version > /var/jenkins_home/jenkins.install.UpgradeWizard.state
RUN install-plugins.sh < /var/jenkins_home/plugins.txt
USER root
RUN wget https://raw.githubusercontent.com/ayudadigital/docker-command-launcher/master/kd.sh -O /usr/sbin/kd -q && \
    chmod +x /usr/sbin/kd && \
    curl https://raw.githubusercontent.com/ayudadigital/devcontrol/master/install.sh | bash
