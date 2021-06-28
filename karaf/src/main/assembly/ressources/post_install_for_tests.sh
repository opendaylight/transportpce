#!/bin/sh

cd $(dirname $0)
sed -i'_' -e '1 a\
\
. \$(dirname \$0)/\.\./\.\./\.\./\.\./tests/reflectwarn.sh\
mkdir -p \$(dirname \$0)/\.\./configuration/initial/\
if [ -z "$USE_ODL_ALT_RESTCONF_PORT" ]; then\
    RESTCONF_PORT=8181\
else\
    RESTCONF_PORT=$USE_ODL_ALT_RESTCONF_PORT\
fi\
sed -e "s/ODL_RESTCONF_PORT/$RESTCONF_PORT/" \$(dirname \$0)/\.\./ressources/org.ops4j.pax.web._template.cfg  >\$(dirname \$0)/\.\./etc/org.ops4j.pax.web.cfg\
sed -e "s/ODL_RESTCONF_PORT/$RESTCONF_PORT/" \$(dirname \$0)/\.\./ressources/jetty_template.xml  >\$(dirname \$0)/\.\./etc/jetty.xml\
if [ -z "$USE_ODL_ALT_AKKA_PORT" ]; then\
    AKKA_PORT=2550\
else\
    AKKA_PORT=$USE_ODL_ALT_AKKA_PORT\
fi\
sed -e "s/ODL_AKKA_PORT/$AKKA_PORT/" \$(dirname \$0)/\.\./ressources/akka-default_template.conf  >\$(dirname \$0)/\.\./configuration/initial/akka.conf\
' ../bin/karaf

sed 's/2550/ODL_AKKA_PORT/' ../system/org/opendaylight/controller/sal-clustering-config/*/sal-clustering-config-*-akkaconf.xml >akka-default_template.conf
sed 's/8181/ODL_RESTCONF_PORT/' ../etc/org.ops4j.pax.web.cfg > org.ops4j.pax.web._template.cfg
sed 's/8181/ODL_RESTCONF_PORT/' ../etc/jetty.xml > jetty_template.xml
