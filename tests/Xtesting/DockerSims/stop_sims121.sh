#!/bin/sh
GROUP=docker
DOCKER_CMD=docker
if id -nG "$USER" | grep -qw "$GROUP"; then
    echo $USER belongs to $GROUP group
else
    echo $USER does not belong to $GROUP group
    DOCKER_CMD="sudo docker"
fi

${DOCKER_CMD}  container stop xpdra-1.2.1 roadma-1.2.1 roadmb-1.2.1 roadmc-1.2.1 xpdrc-1.2.1 roadma-full-1.2.1 roadmc-full-1.2.1
