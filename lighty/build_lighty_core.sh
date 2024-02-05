#!/bin/sh

cd $(dirname "$0")
cd ..
git clone https://github.com/PANTHEONtech/lighty.git lighty-repo
cd lighty-repo
git checkout main
#git checkout 19.x
export JDK_JAVA_OPTIONS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED"
mvn clean install -B -U -q -DskipTests -s ../tests/odl_settings.xml -Dmaven.javadoc.skip=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
