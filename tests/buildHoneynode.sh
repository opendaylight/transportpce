#!/bin/bash

set -e

(cd honeynode && mvn clean install -DskipTests -s fd_io_honeycomb_settings.xml)
chmod +x ./honeynode/honeynode-distribution/target/honeynode-distribution-1.18.01-hc/honeynode-distribution-1.18.01/honeycomb-tpce

exit $?
