#!/bin/sh

cd $(dirname "$0")
cd ..
#mvn clean install -DskipTests
git clone https://github.com/PantheonTechnologies/lighty-core.git
cd lighty-core
git checkout 12.0.x
mvn clean install -DskipTests -Dmaven.javadoc.skip=true
cd ../lighty
mvn clean install -Dmaven.javadoc.skip=true
cd  target
unzip lighty-transportpce-12.0.1-SNAPSHOT-bin.zip
cd ..
rm -rf ../lighty-core
