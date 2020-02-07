#!/bin/bash

ORDMVERSION=${1:-2.1}

set -e

rm -rf honeynode/$ORDMVERSION/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
mkdir -p honeynode/$ORDMVERSION/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
#cp honeynode/$ORDMVERSION/honeynode-plugin-api/src/main/yang/* honeynode/$ORDMVERSION/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
find honeynode/$ORDMVERSION/honeynode-plugin-api/src/main/yang/ -name "*.yang" -type f -exec cp {} ./honeynode/$ORDMVERSION/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang \;
(cd honeynode/$ORDMVERSION/ && mvn clean install -DskipTests -Dcheckstyle.skip -s -Dmaven.javadoc.skip=true ../fd_io_honeycomb_settings.xml)
chmod +x ./honeynode/$ORDMVERSION/honeynode-distribution/target/honeynode-distribution-1.18.01-hc/honeynode-distribution-1.18.01/honeycomb-tpce

exit $?
