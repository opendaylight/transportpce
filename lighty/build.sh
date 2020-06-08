#!/bin/sh

cd $(dirname "$0")
#mvn clean install -s tests/odl_settings.xml -DskipTests -Dmaven.javadoc.skip=true -Dodlparent.spotbugs.skip -Dodlparent.checkstyle.skip
mvn clean install -Dmaven.javadoc.skip=true
unzip target/tpce-bin.zip -d target
