#!/bin/sh

. $(dirname $0)/dockercmd_profile.sh

DOCKER_IMAGE=honeynode:alpine-jre11

DOCKER_OPTIONS="-e USER=${RESTCONF_USER} -e PASSWORD=${RESTCONF_PASSWORD} -e DEVICE_VERSION=${DEVICE_VERSION} -dit ${DOCKER_IMAGE}"

for image in $IMAGE_LIST;do
    suffix_port=`echo -n $image| cut -d: -f1` 
    image_name=`echo -n $image| cut -d: -f2`
    device_file=`echo -n $image| cut -d: -f3`
    echo "$image_name $suffix_port $device_file"
    if [ ! "$(${DOCKER_CMD} ps -q -f name=${image_name})" ];then
       if [  "$(${DOCKER_CMD} ps -aq -f status=exited -f name=${image_name})" ];then
          ${DOCKER_CMD} rm ${image_name}
       fi
    elif [  "$(${DOCKER_CMD} ps -q -f status=running -f name=${image_name})" ];then
          ${DOCKER_CMD} stop ${image_name}
    fi
echo "
    ${DOCKER_CMD} run --rm -p 178$suffix_port:1830 -p 81$suffix_port:8130 --name ${image_name} -e DEVICE_FILE=${device_file} ${DOCKER_OPTIONS}
    "
done

exit

