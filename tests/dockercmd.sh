#!/bin/sh

DOCKER_CMD=docker
if [ `which podman` ]
then DOCKER_CMD=podman
else
    if [ ! `which $DOCKER_CMD` ]
    then  echo "no docker command available" >&2
        exit 1
    fi
    #if "docker ps" cannot be run without error, prepend sudo
    if ( ! $DOCKER_CMD ps >/dev/null 2>&1 );then
        echo "docker command only usable as root, using sudo" >&2
        DOCKER_CMD="sudo docker"
    fi
fi

$DOCKER_CMD $@
