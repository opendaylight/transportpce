#!/bin/sh

cd $(dirname $0)

sed 's/2550/ODL_AKKA_PORT/' ../system/org/opendaylight/controller/sal-clustering-config/*/sal-clustering-config-*-akkaconf.xml >akka-default_template.conf
sed 's/8181/ODL_RESTCONF_PORT/' ../etc/org.ops4j.pax.web.cfg > org.ops4j.pax.web._template.cfg
sed 's/8181/ODL_RESTCONF_PORT/' ../etc/jetty.xml > jetty_template.xml
sed 's/8101/ODL_SHELL_PORT/' ../etc/org.apache.karaf.shell.cfg > org.apache.karaf.shell._template.cfg
sed -e 's/1099/ODL_RMI_REGISTRY_PORT/' -e 's/44444/ODL_RMI_SERVER_PORT/' ../etc/org.apache.karaf.management.cfg > org.apache.karaf.management._template.cfg
sed 's/^[#|]websocket-port=8185/websocket-port=ODL_WEBSOCKET_PORT/' ../system/org/opendaylight/netconf/sal-rest-connector-config/[0-9.]*/sal-rest-connector-config-[0-9.]*-restconf.cfg >org.opendaylight.restconf._template.cfg

cp ../bin/karaf  ../bin/karaf_
cat karaf_pre_launch.sh ../bin/karaf_ > ../bin/karaf
chmod +x  ../bin/karaf

