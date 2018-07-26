#!/bin/bash

# Please see https://wiki.opendaylight.org/view/OpenDaylight_Controller:Netconf:Testtool
# It's configured to run 1 node listening to 17830 (ssh)
# You may put your configuration in config.xml

# opts="${opts} --device-count 2"
# opts="${opts} --debug true"
# opts="${opts} --ssh tcp"
# opts="${opts} --notification-file notif.xml"

java -jar ./netconf/netconf/tools/netconf-testtool/target/netconf-testtool-1.5.0-SNAPSHOT-executable.jar \
     --schemas-dir schemas \
     --initial-config-xml-file sample_configs/ord_1.2.1/sample-config-ROADM.xml \
     ${opts}

exit $?
