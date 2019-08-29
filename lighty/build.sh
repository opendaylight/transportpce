#!/bin/bash

cd ..
mvn clean install -DskipTests
git clone https://github.com/PantheonTechnologies/lighty-core.git
cd lighty-core
git checkout 10.0.x
mvn clean install -DskipTests
cd ../lighty
mvn clean install
cd  target
unzip lighty-transportpce-10.0.1-SNAPSHOT-bin.zip
cd ..
# rm -rf ../lighty-core
