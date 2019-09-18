#!/bin/bash

cd ..
#mvn clean install -DskipTests
git clone https://github.com/PantheonTechnologies/lighty-core.git
cd lighty-core
git checkout master
mvn clean install -DskipTests
cd ../lighty
mvn clean install
cd  target
unzip lighty-transportpce-12.0.0-SNAPSHOT-bin.zip
cd ..
rm -rf ../lighty-core
