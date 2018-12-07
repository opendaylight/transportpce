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
(cd netconf && patch -p1 < ../netconf.patch )
(cd netconf/netconf/tools/netconf-testtool && mvn clean install -s ../../../../odl_settings.xml -DskipTests -Dmaven.javadoc.skip=true)
rm -rf schemas && mkdir -p schemas
cp ordmodels_1.2.1/org-openroadm-* schemas
cp ${yang} schemas
mkdir -p transportpce_tests/log/

exit $?
