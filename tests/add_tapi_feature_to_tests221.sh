#!/bin/sh

if [ "$USE_LIGHTY" = "True" ]; then
    exit 0;
fi

cd $(dirname $0)
cd ..

if [ ! -d karaf221/ ]; then
    echo 'no target directory' >&2
    cd $(dirname $0)
    exit 1
fi

featreference=$(grep '^featuresBoot =' karaf221/target/assembly/etc/org.apache.karaf.features.cfg | cut -d= -f2 | sed 's/^ *//')
xmlfeatconfigfile=$(grep -lR $featreference karaf221/target/assembly/etc/*.xml)

if [ -f "$xmlfeatconfigfile"_ ]; then
    echo "nothing to do : $xmlfeatconfigfile already modified" >&2
    cd $(dirname $0)
    exit 0
fi

newconfigline=$(grep 'odl-transportpce</feature>' $xmlfeatconfigfile | sed 's@odl-transportpce</feature>@odl-transportpce-tapi</feature>@g')
sed -i_ "/odl-transportpce<\/feature>/a $newconfigline" $xmlfeatconfigfile

cd $(dirname $0)
