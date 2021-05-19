#!/bin/sh

cd $(dirname "$0")
cd ..
git clone https://github.com/PantheonTechnologies/lighty-core.git
cd lighty-core
#git checkout master
git checkout 14.0.x
export JDK_JAVA_OPTIONS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED"
mvn clean install -DskipTests -Dmaven.javadoc.skip=true
