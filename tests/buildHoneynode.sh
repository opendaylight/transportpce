#!/bin/bash

ORDMVERSION=${1:-1.2.1}

set -e
. reflectwarn.sh
cd honeynode/$ORDMVERSION
mvn clean install -DskipTests -Dcheckstyle.skip -Dmaven.javadoc.skip=true -s ../fd_io_honeycomb_settings.xml -q -V
chmod +x ./honeynode-distribution/target/honeynode-distribution-1.19.04-hc/honeynode-distribution-1.19.04/honeycomb-tpce
exit $?
