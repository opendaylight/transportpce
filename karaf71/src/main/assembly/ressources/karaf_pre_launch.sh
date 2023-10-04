#!/bin/sh

mkdir -p $(dirname $0)/../configuration/initial/
if [ -z "$USE_ODL_ALT_RESTCONF_PORT" ]; then
    RESTCONF_PORT=8181
else
    RESTCONF_PORT=$USE_ODL_ALT_RESTCONF_PORT
fi
sed -e "s/ODL_RESTCONF_PORT/$RESTCONF_PORT/" $(dirname $0)/../ressources/org.ops4j.pax.web._template.cfg  >$(dirname $0)/../etc/org.ops4j.pax.web.cfg
sed -e "s/ODL_RESTCONF_PORT/$RESTCONF_PORT/" $(dirname $0)/../ressources/jetty_template.xml  >$(dirname $0)/../etc/jetty.xml
if [ -z "$USE_ODL_ALT_AKKA_PORT" ]; then
    AKKA_PORT=2550
else
    AKKA_PORT=$USE_ODL_ALT_AKKA_PORT
fi
sed -e "s/ODL_AKKA_PORT/$AKKA_PORT/" $(dirname $0)/../ressources/akka-default_template.conf  >$(dirname $0)/../configuration/initial/akka.conf
if [ -z "$USE_ODL_ALT_SHELL_PORT" ]; then
    SHELL_PORT=8101
else
    SHELL_PORT=$USE_ODL_ALT_SHELL_PORT
fi
sed -e "s/ODL_SHELL_PORT/$SHELL_PORT/" $(dirname $0)/../ressources/org.apache.karaf.shell._template.cfg  >$(dirname $0)/../etc/org.apache.karaf.shell.cfg
if [ -z "$USE_ODL_ALT_RMI_REGISTRY_PORT" ]; then
    RMI_REGISTRY_PORT=1099
else
    RMI_REGISTRY_PORT=$USE_ODL_ALT_RMI_REGISTRY_PORT
fi
if [ -z "$USE_ODL_ALT_RMI_SERVER_PORT" ]; then
    RMI_SERVER_PORT=44444
else
    RMI_SERVER_PORT=$USE_ODL_ALT_RMI_SERVER_PORT
fi
sed -e "s/ODL_RMI_REGISTRY_PORT/$RMI_REGISTRY_PORT/" -e "s/ODL_RMI_SERVER_PORT/$RMI_SERVER_PORT/" $(dirname $0)/../ressources/org.apache.karaf.management._template.cfg  >$(dirname $0)/../etc/org.apache.karaf.management.cfg
if [ -z "$USE_ODL_ALT_WEBSOCKET_PORT" ]; then
    WEBSOCKET_PORT=8185
else
    WEBSOCKET_PORT=$USE_ODL_ALT_WEBSOCKET_PORT
fi
sed -e "s/ODL_WEBSOCKET_PORT/$WEBSOCKET_PORT/" $(dirname $0)/../ressources/org.opendaylight.restconf._template.cfg  >$(dirname $0)/../etc/org.opendaylight.restconf.cfg

touch $(dirname $0)/../ressources/karaf_configured
