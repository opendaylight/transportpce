#!/bin/sh

DEVICE_VERSION=${1:-1.2.1}

if [ "$DEVICE_VERSION" = "1.2.1" ]; then
     IMAGE_LIST="30:xpdra-1.2.1:oper-XPDRA.xml
21:roadma-1.2.1:oper-ROADMA.xml
22:roadmb-1.2.1:oper-ROADMB.xml
23:roadmc-1.2.1:oper-ROADMC.xml
34:xpdrc-1.2.1:oper-XPDRC.xml
31:roadma-full-1.2.1:oper-ROADMA-full.xml
33:roadmc-full-1.2.1:oper-ROADMC-full.xml
"
else
    if [ "$DEVICE_VERSION" = "2.2.1" ]; then
        IMAGE_LIST="40:xpdra-2.2.1:oper-XPDRA.xml
41:roadma-2.2.1:oper-ROADMA.xml
42:roadmb-2.2.1:oper-ROADMB.xml
43:roadmc-2.2.1:oper-ROADMC.xml
44:xpdrc-2.2.1:oper-XPDRC.xml
45:spdrav1-2.2.1:oper-SPDRAv1.xml
46:spdrav2-2.2.1:oper-SPDRAv2.xml
"
    else echo "unsupported device version"
	exit 1
    fi
fi

DOCKER_CMD=docker
RESTCONF_USER=admin
RESTCONF_PASSWORD=admin
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


