#!/bin/sh
	
cd $(dirname "$0")
cd ..
git clone https://github.com/PantheonTechnologies/lighty-core.git
cd lighty-core
git checkout master
mvn clean install -DskipTests -Dmaven.javadoc.skip=true
