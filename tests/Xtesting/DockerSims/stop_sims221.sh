#!/bin/sh
GROUP=docker
DOCKER_CMD=docker
if id -nG "$USER" | grep -qw "$GROUP"; then
    echo $USER belongs to $GROUP group
else
    echo $USER does not belong to $GROUP group
    DOCKER_CMD="sudo docker"
fi

${DOCKER_CMD} stop xpdra-2.2.1 roadma-2.2.1 roadmb-2.2.1 roadmc-2.2.1 xpdrc-2.2.1 spdrav1-2.2.1 spdrav2-2.2.1
