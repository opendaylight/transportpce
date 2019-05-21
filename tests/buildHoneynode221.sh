#!/bin/bash

set -e

rm -rf honeynode221/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
mkdir -p honeynode221/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
cp honeynode221/honeynode-plugin-api/src/main/yang/common/* honeynode221/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
cp honeynode221/honeynode-plugin-api/src/main/yang/device221/* honeynode221/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
(cd honeynode221 && mvn clean install -DskipTests -Dcheckstyle.skip -s fd_io_honeycomb_settings.xml)
chmod +x ./honeynode221/honeynode-distribution/target/honeynode-distribution-1.18.01-hc/honeynode-distribution-1.18.01/honeycomb-tpce

exit $?
