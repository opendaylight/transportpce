#!/bin/sh 
GROUP=docker
DOCKER_CMD=docker
if id -nG "$USER" | grep -qw "$GROUP"; then
    echo $USER belongs to $GROUP group
else
    echo $USER does not belong to $GROUP group
    DOCKER_CMD="sudo docker"
fi

for docker_image in xpdra-1.2.1  roadma-1.2.1 roadmb-1.2.1  roadmc-1.2.1 xpdrc-1.2.1 roadma-full-1.2.1 roadmc-full-1.2.1;do
    if [ ! "$(${DOCKER_CMD} ps -q -f name=${docker_image})" ];then
       if [  "$(${DOCKER_CMD} ps -aq -f status=exited -f name=${docker_image})" ];then
          ${DOCKER_CMD} rm ${docker_image}
       fi
    elif [  "$(${DOCKER_CMD} ps -q -f status=running -f name=${docker_image})" ];then
          ${DOCKER_CMD} stop ${docker_image} 
    fi
done

echo `${DOCKER_CMD} run --rm  -p 17830:8080 --name xpdra-1.2.1 -e DEVICE_VERSION=1.2.1 -e DEVICE_FILE=oper-XPDRA.xml  -dit  honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17821:8080 --name roadma-1.2.1 -e DEVICE_VERSION=1.2.1 -e DEVICE_FILE=oper-ROADMA.xml -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17822:8080 --name roadmb-1.2.1 -e DEVICE_VERSION=1.2.1 -e DEVICE_FILE=oper-ROADMB.xml  -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17823:8080 --name roadmc-1.2.1 -e DEVICE_VERSION=1.2.1 -e DEVICE_FILE=oper-ROADMC.xml -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17834:8080 --name xpdrc-1.2.1 -e DEVICE_VERSION=1.2.1 -e DEVICE_FILE=oper-XPDRC.xml  -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17831:8080 --name roadma-full-1.2.1 -e DEVICE_VERSION=1.2.1 -e DEVICE_FILE=oper-ROADMA-full.xml -dit honeynode:alpine-jre11`
echo `${DOCKER_CMD} run --rm  -p 17833:8080 --name roadmc-full-1.2.1 -e DEVICE_VERSION=1.2.1 -e DEVICE_FILE=oper-ROADMC-full.xml -dit honeynode:alpine-jre11`

