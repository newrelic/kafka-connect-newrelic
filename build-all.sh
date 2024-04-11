#!/bin/bash
set -ex

# Use Java 1.8, to match release script
JAVA_VERSION=1.8

JAVA_HOME=$(/usr/libexec/java_home -F -v ${JAVA_VERSION}) || {
	echo "Could not find Java Version $JAVA_VERSION"
	exit 1
}
export JAVA_HOME

echo "BUILDING WITH ${JAVA_HOME}"
java -version

MVN_ARGS="${@:-package}"

for project in `find . -name pom.xml | xargs dirname`; do
	echo
	echo "****************************************"
	echo $project
	echo "****************************************"
	echo
	(cd $project && mvn ${MVN_ARGS})
done
