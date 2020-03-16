#!/bin/sh

. $(dirname $0)/dockercmd_profile.sh

stop_list=""
for image in $IMAGE_LIST;do
    image_name=`echo -n $image| cut -d: -f2`
    stop_list=$stop_list" "$image_name
done
${DOCKER_CMD} container stop ${stop_list}
