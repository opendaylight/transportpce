#!/bin/sh
GROUP=docker
DOCKER_CMD=docker
RESTCONF_USER=admin
RESTCONF_PASSWORD=admin
DEVICE_VERSION=1.2.1
DOCKER_IMAGE=honeynode:alpine-jre11
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

${DOCKER_CMD} run --rm  -p 17830:1830 -p 18830:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name xpdra-1.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-XPDRA.xml  -dit  ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17821:1830 -p 18821:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name roadma-1.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-ROADMA.xml -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17822:1830 -p 18822:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name roadmb-1.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-ROADMB.xml  -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17823:1830 -p 18823:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name roadmc-1.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-ROADMC.xml -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17834:1830 -p 18834:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name xpdrc-1.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-XPDRC.xml  -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17831:1830 -p 18831:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name roadma-full-1.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-ROADMA-full.xml -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17833:1830 -p 18833:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name roadmc-full-1.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-ROADMC-full.xml -dit ${DOCKER_IMAGE}

