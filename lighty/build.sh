#!/bin/sh

cd $(dirname "$0")
cd ..
#mvn clean install -s tests/odl_settings.xml -DskipTests -Dmaven.javadoc.skip=true -Dodlparent.spotbugs.skip -Dodlparent.checkstyle.skip
git clone https://github.com/PantheonTechnologies/lighty-core.git
cd lighty-core
git checkout 12.1.x
mvn clean install -DskipTests -Dmaven.javadoc.skip=true
cd ../lighty
mvn clean install -Dmaven.javadoc.skip=true
cd  target
unzip lighty-transportpce-12.1.0-SNAPSHOT-bin.zip
cd ..
rm -rf ../lighty-core
