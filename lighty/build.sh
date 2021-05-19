#!/bin/sh

cd $(dirname "$0")
export JDK_JAVA_OPTIONS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED"
#./build_lighty_core.sh
#mvn clean install -s tests/odl_settings.xml -DskipTests -Dmaven.javadoc.skip=true -Dodlparent.spotbugs.skip -Dodlparent.checkstyle.skip
mvn clean install -Dmaven.javadoc.skip=true
unzip -q target/tpce-bin.zip -d target
