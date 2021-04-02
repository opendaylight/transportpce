#!/bin/sh

cd $(dirname "$0")
#./build_lighty_core.sh
#mvn clean install -s tests/odl_settings.xml -DskipTests -Dmaven.javadoc.skip=true -Dodlparent.spotbugs.skip -Dodlparent.checkstyle.skip
mvn clean install -Dmaven.javadoc.skip=true
unzip -q target/tpce-bin.zip -d target
