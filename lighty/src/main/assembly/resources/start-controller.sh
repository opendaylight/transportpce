#!/bin/sh

BASEDIR=$(dirname "$0")
#echo "${BASEDIR}"
cd ${BASEDIR}

#start controller
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-transportpce-12.1.0-SNAPSHOT.jar
