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

echo "build karaf in " $USE_ODL_ALT_KARAF_INSTALL_DIR "with " $USE_ODL_ALT_KARAF_ENV
. $USE_ODL_ALT_KARAF_ENV
. ./reflectwarn.sh
cd  ../$USE_ODL_ALT_KARAF_INSTALL_DIR
if [ -z "$ODL_SETTING" ]; then
    # No environment variable is set and uses default odl setting file
    mvn clean install -B -q -s tests/odl_settings.xml -Pq
else
    echo "Path for ODL setting file " $ODL_SETTING
    mvn clean install -B -q -s $ODL_SETTING -Pq
fi
./target/assembly/ressources/post_install_for_tests.sh
