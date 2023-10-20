#!/bin/sh

if [ "$USE_LIGHTY" = "True" ]; then
    echo "USE_LIGHTY set to True - no need to build karaf"
    exit
fi

if [ -z "$USE_ODL_ALT_KARAF_INSTALL_DIR" ]; then
    exit
fi

cd $(dirname $0)

if [ -z "$USE_ODL_ALT_KARAF_ENV" ]; then
    exit
fi

. $USE_ODL_ALT_KARAF_ENV
. ./reflectwarn.sh
cd  ../$USE_ODL_ALT_KARAF_INSTALL_DIR
mvn clean install -B -q -s ../tests/odl_settings.xml -Pq
./target/assembly/ressources/post_install_for_tests.sh
