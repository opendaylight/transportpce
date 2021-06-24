#!/bin/sh

BASEDIR=$(dirname "$0")
cd ${BASEDIR}

rm -rf cache
rm -rf target

# check if default ports must be overriden
if [ -z "$USE_ODL_ALT_RESTCONF_PORT" ]; then
    RESTCONF_PORT=8181
else
    RESTCONF_PORT=$USE_ODL_ALT_RESTCONF_PORT
fi
if [ -z "$USE_ODL_ALT_WEBSOCKET_PORT" ]; then
    WEBSOCKET_PORT=8185
else
    WEBSOCKET_PORT=$USE_ODL_ALT_WEBSOCKET_PORT
fi
if [ -z "$USE_ODL_ALT_AKKA_PORT" ]; then
    AKKA_PORT=2550
else
    AKKA_PORT=$USE_ODL_ALT_AKKA_PORT
fi
if [ -z "$USE_ODL_ALT_AKKA_MGT_PORT" ]; then
    AKKA_MGT_PORT=8558
else
    AKKA_MGT_PORT=$USE_ODL_ALT_AKKA_MGT_PORT
fi

# generate appropriate configuration files
cat config_template.json | sed -e "s/ODL_RESTCONF_PORT/$RESTCONF_PORT/" -e "s/ODL_WEBSOCKET_PORT/$WEBSOCKET_PORT/" >config.json
cat akka-default_template.conf | sed -e "s/ODL_AKKA_PORT/$AKKA_PORT/" -e "s/ODL_AKKA_MGT_PORT/$AKKA_MGT_PORT/" >singlenode/akka-default.conf

#start controller
java -ms128m -mx512m -XX:MaxMetaspaceSize=128m -jar tpce.jar -restconf config.json
