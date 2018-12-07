#!/bin/bash

(cd .. && git apply tests/SH_stubs.diff && mvn clean install -s tests/odl_settings.xml -DskipTests -Dmaven.javadoc.skip=true)
(cd .. && git checkout .)

exit $?
