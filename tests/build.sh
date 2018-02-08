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

(cp testtool_pom.xml netconf/netconf/tools/netconf-testtool/pom.xml  && cd netconf/netconf/tools/netconf-testtool &&  mvn clean install -DskipTests)
rm -rf schemas && mkdir -p schemas
#sample config for ordm 2.x are not yet ready
#cp -r ../ordmodels/common/src/main/yang/org-openroadm-* schemas
#cp -r ../ordmodels/device/src/main/yang/org-openroadm-* schemas
#rm schemas/org-openroadm-otn-common-types@2016-10-14.yang
#we expect that the ODL instance compiled with ordm 2.x models will be compatible with ordm 1.2.1 devices
cp ordmodels_1.2.1/org-openroadm-* schemas
cp ${yang} schemas

exit $?
