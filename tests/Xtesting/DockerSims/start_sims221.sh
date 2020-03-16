#!/bin/sh

GROUP=docker
DOCKER_CMD=docker
if id -nG "$USER" | grep -qw "$GROUP"; then
    echo $USER belongs to $GROUP group
else
    echo $USER does not belong to $GROUP group
    DOCKER_CMD="sudo docker"
fi

for docker_image in xpdra-2.2.1  roadma-2.2.1 roadmb-2.2.1  roadmc-2.2.1 xpdrc-2.2.1 spdrav1-2.2.1 spdrav2-2.2.1;do
   if [ ! "$(${DOCKER_CMD} ps -q -f name=${docker_image})" ];then
      if [  "$(${DOCKER_CMD} ps -aq -f status=exited -f name=${docker_image})" ];then
         ${DOCKER_CMD} rm ${docker_image}
      fi
   elif [  "$(${DOCKER_CMD} ps -q -f status=running -f name=${docker_image})" ];then
      ${DOCKER_CMD} stop ${docker_image}
   fi
done

echo `${DOCKER_CMD} run --rm  -p 17840:8080 -p 18840:8180 --name xpdra-2.2.1 -e DEVICE_VERSION=2.2.1 -e DEVICE_FILE=oper-XPDRA.xml  -dit  honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17841:8080 -p 18841:8180 --name roadma-2.2.1 -e DEVICE_VERSION=2.2.1 -e DEVICE_FILE=oper-ROADMA.xml -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17842:8080 -p 18842:8180 --name roadmb-2.2.1 -e DEVICE_VERSION=2.2.1 -e DEVICE_FILE=oper-ROADMB.xml  -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17843:8080 -p 18843:8180 --name roadmc-2.2.1 -e DEVICE_VERSION=2.2.1 -e DEVICE_FILE=oper-ROADMC.xml -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17844:8080 -p 18844:8180 --name xpdrc-2.2.1 -e DEVICE_VERSION=2.2.1 -e DEVICE_FILE=oper-XPDRC.xml  -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17845:8080 -p 18845:8180 --name spdrav1-2.2.1 -e DEVICE_VERSION=2.2.1 -e DEVICE_FILE=oper-SPDRAv1.xml  -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17846:8080 -p 18846:8180 --name spdrav2-2.2.1 -e DEVICE_VERSION=2.2.1 -e DEVICE_FILE=oper-SPDRAv2.xml  -dit honeynode:alpine-jre11`

