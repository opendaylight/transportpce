#!/bin/bash

set -e

rm -rf honeynode/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
mkdir -p honeynode/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
cp honeynode/honeynode-plugin-api/src/main/yang/* honeynode/honeynode-plugin-impl/src/main/resources/honeycomb-minimal-resources/config/yang
(cd honeynode && mvn clean install -DskipTests -Dcheckstyle.skip -s fd_io_honeycomb_settings.xml)
chmod +x ./honeynode/honeynode-distribution/target/honeynode-distribution-1.18.01-hc/honeynode-distribution-1.18.01/honeycomb-tpce

exit $?
