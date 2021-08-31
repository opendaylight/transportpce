#!/bin/sh

set -e

current_dir=$PWD

cd $(dirname $0)

#install maven and JDK11 on the Gate since they are not there by default
which mvn >/dev/null || ./installMavenCentOS.sh
cd ../

if [ "$USE_LIGHTY" != "True" ]; then
    for suffix in 121 221 71; do
        rm -rf "karaf$suffix"
        cp -r karaf "karaf$suffix"
    done
fi

#build controller, source JDK_JAVA_OPTIONS to remove illegal reflective acces warnings introduced by Java11
. "$current_dir"/reflectwarn.sh
mvn clean install -B -q -s tests/odl_settings.xml -DskipTests -Dmaven.javadoc.skip=true -Dodlparent.spotbugs.skip -Dodlparent.checkstyle.skip

#patch Karaf exec for the same reason at runtime and also to have the possibility to use alternative ports
./karaf/target/assembly/ressources/post_install_for_tests.sh

#build Lighty if needed
if [ "$USE_LIGHTY" = "True" ]; then
    cd lighty
    ./build.sh
fi
