#!/bin/sh

cd $(dirname "$0")
export JDK_JAVA_OPTIONS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED"
# uncomment the following line when related artifacts are not avaible on mvn central yet
./build_lighty_core.sh
mvn clean install -B -U -q -Dmaven.javadoc.skip=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -s ../tests/odl_settings.xml -DskipTests
unzip -q target/tpce-bin.zip -d target
