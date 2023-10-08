ARG TAG=2.387.1
FROM ghcr.io/ayudadigital/jenkins-dind:${TAG}
RUN apk add --no-cache rsync openjdk8 jq
COPY --chown=jenkins:jenkins config /var/jenkins_home/
# Jenkins plugins install
RUN cd /tmp && \
    wget https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/2.12.11/jenkins-plugin-manager-2.12.11.jar && \
    java -jar jenkins-plugin-manager-2.12.11.jar --plugin-file /var/jenkins_home/plugins.txt
RUN wget https://raw.githubusercontent.com/ayudadigital/docker-command-launcher/main/kd.sh -O /usr/sbin/kd -q && \
    chmod +x /usr/sbin/kd && \
    curl https://raw.githubusercontent.com/ayudadigital/devcontrol/main/install.sh | bash
