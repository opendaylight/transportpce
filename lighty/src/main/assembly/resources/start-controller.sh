#!/bin/sh

BASEDIR=$(dirname "$0")
#echo "${BASEDIR}"
cd ${BASEDIR}

#start controller
java -ms128m -mx512m -XX:MaxMetaspaceSize=128m -jar tpce.jar
