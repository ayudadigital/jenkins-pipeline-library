#!/bin/bash
agent_name=$1

cat <<EOF | java -Xmx512m -jar /tmp/jenkins-cli.jar -s http://localhost:8080 create-node ${agent_name}
<slave>
  <name>${agent_name}</name>
  <description>${agent_name}</description>
  <remoteFS>/home/jenkins/agent</remoteFS>
  <numExecutors>2</numExecutors>
  <mode>NORMAL</mode>
  <retentionStrategy class="hudson.slaves.RetentionStrategy$Always"/>
  <launcher class="hudson.slaves.JNLPLauncher">
    <workDirSettings>
      <disabled>false</disabled>
      <internalDir>remoting</internalDir>
      <failIfWorkDirIsMissing>false</failIfWorkDirIsMissing>
    </workDirSettings>
  </launcher>
  <label>agent</label>
  <nodeProperties/>
</slave>
EOF

echo 'println jenkins.model.Jenkins.instance.nodesObject.getNode("'${agent_name}'")?.computer?.jnlpMac' | java -Xmx512m -jar /tmp/jenkins-cli.jar -s http://localhost:8080 groovy =
