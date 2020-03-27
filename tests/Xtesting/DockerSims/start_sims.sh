#!/bin/sh

. $(dirname $0)/dockercmd_profile.sh

DOCKER_OPTIONS="-e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} -dit ${DOCKER_IMAGE}"

for image in $IMAGE_LIST;do
    suffix_port=`echo -n $image| cut -d: -f1`
    image_name=`echo -n $image| cut -d: -f2`
    device_file=`echo -n $image| cut -d: -f3`
    if [ "${DEVICE_VERSION}" = "all" ]
        then deviceversion=`echo -n $device_file| cut -d'/' -f1`
    else
        deviceversion=${DEVICE_VERSION}
    fi
    if [ ! "$(${DOCKER_CMD} ps -q -f name=${image_name})" ];then
       if [  "$(${DOCKER_CMD} ps -aq -f status=exited -f name=${image_name})" ];then
          ${DOCKER_CMD} rm ${image_name}
       fi
    elif [  "$(${DOCKER_CMD} ps -q -f status=running -f name=${image_name})" ];then
          ${DOCKER_CMD} stop ${image_name}
    fi
    echo ${image_name}
    ${DOCKER_CMD} run --rm -p 178$suffix_port:1830 -p 81$suffix_port:8130 --name ${image_name} -e DEVICE_VERSION=${deviceversion} -e DEVICE_FILE=${device_file} ${DOCKER_OPTIONS}
done

exit

