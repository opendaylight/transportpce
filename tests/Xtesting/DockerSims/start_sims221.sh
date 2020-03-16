#!/bin/sh

GROUP=docker
DOCKER_CMD=docker
RESTCONF_USER=admin
RESTCONF_PASSWORD=admin
DEVICE_VERSION=2.2.1
DOCKER_IMAGE=honeynode:alpine-jre11
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

${DOCKER_CMD} run --rm  -p 17840:1830 -p 18840:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name xpdra-2.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-XPDRA.xml  -dit  ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17841:1830 -p 18841:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name roadma-2.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-ROADMA.xml -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17842:1830 -p 18842:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name roadmb-2.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-ROADMB.xml  -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17843:1830 -p 18843:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name roadmc-2.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-ROADMC.xml -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17844:1830 -p 18844:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name xpdrc-2.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-XPDRC.xml  -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17845:1830 -p 18845:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name spdrav1-2.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-SPDRAv1.xml  -dit ${DOCKER_IMAGE}
${DOCKER_CMD} run --rm  -p 17846:1830 -p 18846:8130 -e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} --name spdrav2-2.2.1 -e DEVICE_VERSION=${DEVICE_VERSION} -e DEVICE_FILE=oper-SPDRAv2.xml  -dit ${DOCKER_IMAGE}

