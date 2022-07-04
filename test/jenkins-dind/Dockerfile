ARG TAG=2.346.1
FROM ayudadigital/jenkins-dind:${TAG}
RUN apk add --no-cache rsync openjdk8 jq
COPY --chown=jenkins:jenkins config /var/jenkins_home/
# Jenkins plugins install
RUN cd /tmp && \
    wget https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/2.12.3/jenkins-plugin-manager-2.12.3.jar && \
    java -jar jenkins-plugin-manager-2.12.3.jar --plugin-file /var/jenkins_home/plugins.txt
RUN wget https://raw.githubusercontent.com/ayudadigital/docker-command-launcher/master/kd.sh -O /usr/sbin/kd -q && \
    chmod +x /usr/sbin/kd && \
    curl https://raw.githubusercontent.com/ayudadigital/devcontrol/master/install.sh | bash
