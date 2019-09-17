#!/bin/bash

cd ..
patch -p0 <lighty/patch_ordm41_deviations.diff
mvn clean install -DskipTests
#git checkout -- ordmodels/network/src/main/yang/org-openroadm-network-topology@2018-11-30.yang ordmodels/network/src/main/yang/org-openroadm-network@2018-11-30.yang ordmodels/network/src/main/yang/org-openroadm-otn-network-topology@2018-11-30.yang
git clone https://github.com/PantheonTechnologies/lighty-core.git
cd lighty-core
git checkout master
mvn clean install -DskipTests
cd ../lighty
mvn clean install
cd  target
unzip lighty-transportpce-11.0.0-SNAPSHOT-bin.zip
cd ..
# rm -rf ../lighty-core
