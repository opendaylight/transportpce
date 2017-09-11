#!/bin/bash

# It may require to call git submodule update --init

set -e

yang="\
mdsal/model/iana/iana-afn-safi/src/main/yang/iana-afn-safi@2013-07-04.yang \
mdsal/model/ietf/ietf-inet-types-2013-07-15/src/main/yang/ietf-inet-types@2013-07-15.yang \
mdsal/model/ietf/ietf-yang-types-20130715/src/main/yang/ietf-yang-types@2013-07-15.yang \
netconf/netconf/models/ietf-netconf/src/main/yang/ietf-netconf@2011-06-01.yang \
netconf/netconf/models/ietf-netconf-notifications/src/main/yang/ietf-netconf-notifications@2012-02-06.yang \
netconf/netconf/models/ietf-netconf-notifications/src/main/yang/notifications@2008-07-14.yang"

rm -rf netconf mdsal && git submodule update --init
(cd netconf && patch -p1 < ../netconf.patch && patch -p1 < ../get_connection_port_trail.patch)

(cd netconf/netconf/tools/netconf-testtool && mvn clean install -DskipTests)
rm -rf schemas; mkdir -p schemas && cp -r ../ordmodels/src/main/yang/org-openroadm-* schemas
cp ${yang} schemas

exit $?
