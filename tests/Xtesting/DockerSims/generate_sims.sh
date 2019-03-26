#!/bin/bash

cp Dockerfile.orig Dockerfile
sudo docker build --tag=alpine_honeynode:2.1 .
for conf_file in `(cd ../.. && ls sample_configs/openroadm/2.1/)`
do  DOCKNAME=honeynode_`echo $conf_file |cut -d\. -f1 |tr '[:upper:]' '[:lower:]'`
        echo "generating docker image '$DOCKNAME'"
        echo -e "FROM alpine_honeynode:2.1\n\nCMD honeynode 830 sample_configs/openroadm/2.1/$conf_file\n" >Dockerfile
        sudo docker build --tag=$DOCKNAME:2.1 .
done
cp Dockerfile.orig.testtool Dockerfile
sudo docker build --tag=alpine_testtool:1.2.1 .
for conf_file in `(cd ../../ && ls sample_configs/openroadm/1.2.1/)`
do  DOCKNAME=testtool_`echo $conf_file |cut -d\. -f1  |cut -d\- -f3|tr '[:upper:]' '[:lower:]'`
        echo "generating docker image '$DOCKNAME'"
        echo -e "FROM alpine_testtool:1.2.1\n\nCMD testtool --schemas-dir schemas --starting-port 17830 --initial-config-xml sample_configs/openroadm/1.2.1/$conf_file\n" >Dockerfile
        sudo docker build --tag=$DOCKNAME:1.2.1 .
done
