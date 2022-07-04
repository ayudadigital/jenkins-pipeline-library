#!/bin/bash

# Use "bin/test.sh local test [testname]" to run only one specific test
# Example: "bin/test.sh local test jplStartTest"
# Use prefix "TAG=<tag>" to run an especific jenkins-dind image tag
# Example: "TAG=beta bin/test.sh"

# Functions
function runWithinDocker () {
    service=$1
    command=$2
    docker-compose exec -u jenkins -T $service bash -c "${command}"
    returnValue=$((returnValue + $?))
}

function executeJenkinsCommand () {
    command=$1
    docker-compose exec -u jenkins -T jenkins-dind bash -c "java -Xmx512m -jar /tmp/jenkins-cli.jar -s http://localhost:8080 ${command}"
    return $?
}

function runTest () {
    testName=$1
    echo "# Run ${testName} Test..."
    if [[ "$2" == "" ]]
    then
        expectedResult=0
    else
        expectedResult=$2
    fi
    if [[ "$uniqueTestName" != "" ]] && [[ "$uniqueTestName" != "$testName" ]]
    then
        echo -e "\t\t(Force pass with assert=true)"
        return 0
    fi
    executeJenkinsCommand "build ${testName} -s"
    if [[ "$?" -ne "${expectedResult}" ]]
    then
        returnValue=$((returnValue + 1))
    fi
    docker cp ${id}:/var/jenkins_home/jobs/${testName}/builds/1/log test/reports/${testName}.log
    returnValue=$((returnValue + $?))
}

# Configuration
cd "$(dirname $0)/.."
mkdir -p test/reports
rm -f test/reports/*
returnValue=0
doTests="false"
testName=""
uniqueTestName=""
if [[ "$1" == "local" ]]
then
    if [[ "$2" == "test" ]]
    then
        doTests="true"
        if [[ "$3" != "" ]]
        then
            uniqueTestName=$3
        fi
    fi
else
    doTests="true"
fi

# Main
echo "# Start jenkins as a docker-compose daemon"
if [ "$TAG" == "" ]; then
    export TAG="latest"
fi
if [[ "$1" == "local" ]]; then
    BUILDOPTIONS=""

else
    BUILDOPTIONS="--pull --no-cache"
fi
chmod 600 test/jenkins-dind/config/.ssh/*
chmod 700 test/jenkins-dind/config/.ssh
docker-compose build $BUILDOPTIONS
docker-compose up -d --force-recreate
returnValue=$((returnValue + $?))
id=$(docker-compose ps -q jenkins-dind)
returnValue=$((returnValue + $?))
echo "# Started platform with id ${id} and port $(docker-compose port jenkins-dind 8080)"

echo "# Prepare code for testing"
sleep 10
for item in jenkins-dind jenkins-agent1 jenkins-agent2; do
    docker-compose exec -u jenkins -T ${item} cp -Rp /opt/jpl-source/ /tmp/jenkins-pipeline-library/
    #docker-compose exec -u jenkins -T jenkins-agent1 cp -Rp /opt/jpl-source/ /tmp/jenkins-pipeline-library/
    #docker-compose exec -u jenkins -T jenkins-agent2 cp -Rp /opt/jpl-source/ /tmp/jenkins-pipeline-library/
    runWithinDocker ${item} "rm -f /tmp/jenkins-pipeline-library/.git/hooks/* && git config --global push.default simple && git config --global user.email 'jenkins@ci' && git config --global user.name 'Jenkins'"
    if [[ "$1" == "local" ]] && [[ "$(git status --porcelain)" != "" ]]
    then
        echo "# Local test requested: Commit local jpl changes in ${item}"
        runWithinDocker ${item} "cd /tmp/jenkins-pipeline-library && git add -A && git commit -m 'test within docker'"
    fi
    runWithinDocker ${item} "cd /tmp/jenkins-pipeline-library && git rev-parse --verify develop || git checkout -b develop"
    runWithinDocker ${item} "cd /tmp/jenkins-pipeline-library && git rev-parse --verify master || git checkout -b master"
    runWithinDocker ${item} "cd /tmp/jenkins-pipeline-library && git branch -D release/new || true"
    runWithinDocker ${item} "cd /tmp/jenkins-pipeline-library && git checkout -b 'release/v9.9.9' && git checkout -b 'hotfix/v9.9.9-hotfix-1' && git checkout -b 'jpl-test-promoted' && git checkout -b 'jpl-test' && git checkout -b 'release/new' && git checkout `git rev-parse HEAD` > /dev/null 2>&1"
done

echo "# Waiting for jenkins service to be initialized"
runWithinDocker jenkins-dind "sleep 10 && curl --max-time 50 --retry 10 --retry-delay 5 --retry-max-time 32 http://localhost:8080 -s > /dev/null; sleep 10"

echo "# Download jenkins cli"
runWithinDocker jenkins-dind "wget http://localhost:8080/jnlpJars/jenkins-cli.jar -O /tmp/jenkins-cli.jar -q > /dev/null"

echo "# Prepare agents"
for agent in agent1 agent2
do
    secret=$(docker-compose exec -u jenkins -T jenkins-dind /opt/jpl-source/bin/prepare_agent.sh ${agent})
    docker-compose exec -u jenkins -d -T jenkins-${agent} jenkins-slave -url http://jenkins-dind:8080 ${secret} ${agent}
done

echo "# Reload Jenkins configuration"
executeJenkinsCommand "reload-configuration"

# Run tests
if [[ ${doTests} == "true" ]]
then
    runTest "jplStartTest"
    runTest "jplDockerBuildTest"
    runTest "jplDockerPushTest"
    runTest "jplPromoteCodeHappyTest"
    runTest "jplPromoteBuildTest" 4
    [ "$1" == "local" ] && runTest "jplBuildAPKTest"
    runTest "jplBuildIPAHappyTest"
    runTest "jplMakeReleaseHappyTest"
    runTest "jplCloseReleaseTest"
    runTest "jplCloseHotfixHappyTest"
fi

# Remove compose
if [[ "$1" != "local" ]]
then
    echo "# Switch down and clear compose"
    docker-compose down -v
    returnValue=$((returnValue + $?))
fi

exit ${returnValue}
