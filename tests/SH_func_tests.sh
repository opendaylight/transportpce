#!/bin/bash

(cd .. && git apply tests/SH_stubs.diff && mvn clean install -DskipTests)
(cd .. && git checkout .)

exit $?
