#!/bin/bash

cp Dockerfile.orig.221 Dockerfile
sudo docker build --tag=alpine_honeynode:2.2.1 .
for conf_file in `(cd ../.. && ls sample_configs/openroadm/2.2.1/)`
do  DOCKNAME=honeynode_`echo $conf_file |cut -d\. -f1 |tr '[:upper:]' '[:lower:]'`
        echo "generating docker image '$DOCKNAME'"
        echo -e "FROM alpine_honeynode:2.2.1\n\nCMD honeynode 830 sample_configs/openroadm/2.2.1/$conf_file\n" >Dockerfile
        sudo docker build --tag=$DOCKNAME:2.2.1 .
done
