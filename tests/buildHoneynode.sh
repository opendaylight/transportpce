#!/bin/bash

ORDMVERSION=${1:-2.1}

set -e
cd honeynode/$ORDMVERSION
mvn clean install -DskipTests -Dcheckstyle.skip -Dmaven.javadoc.skip=true -s .m2/settings.xml
chmod +x ./honeynode-distribution/target/honeynode-distribution-1.19.04-hc/honeynode-distribution-1.19.04/honeycomb-tpce

exit $?
