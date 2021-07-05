#!/bin/sh

if [ -z "$USE_ODL_ALT_KARAF_INSTALL_DIR" ]; then
    exit
fi

cd $(dirname $0)

if [ -f ../"$USE_ODL_ALT_KARAF_INSTALL_DIR"/target/assembly/bin/karaf_ ]; then
    exit
fi

. ./reflectwarn.sh
cd  ../$USE_ODL_ALT_KARAF_INSTALL_DIR
mvn clean install -B -q -s ../tests/odl_settings.xml -DskipTests -Dmaven.javadoc.skip=true
./target/assembly/ressources/post_install_for_tests.sh
