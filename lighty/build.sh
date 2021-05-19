#!/bin/sh

cd $(dirname "$0")
export JDK_JAVA_OPTIONS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED"
#./build_lighty_core.sh
mvn clean install -B -U -Dmaven.javadoc.skip=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
unzip -q target/tpce-bin.zip -d target
