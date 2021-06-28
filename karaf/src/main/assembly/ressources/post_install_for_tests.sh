#!/bin/sh

cd $(dirname $0)
sed -i'_' -e '1 a\
\
. \$(dirname \$0)/\.\./\.\./\.\./\.\./tests/reflectwarn.sh' ../bin/karaf
