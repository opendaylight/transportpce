#!/bin/sh

DEVICE_VERSION=${1:-all}

# this file centralizes inb one place parameters needed by docker scripts,
# more especially the image lists for 1.2.1 and 2.2.1 devices.
# device versions supported as scripts argument are 1.2.1 and 2.2.1 and "all"

#IMAGE_LIST format is suffix_port : container/image name : config file to fill netconf operational datastore

IMAGE121_LIST="30:xpdra-1.2.1:1.2.1/oper-XPDRA.xml
31:roadma-1.2.1:1.2.1/oper-ROADMA.xml
32:roadmb-1.2.1:1.2.1/oper-ROADMB.xml
33:roadmc-1.2.1:1.2.1/oper-ROADMC.xml
34:xpdrc-1.2.1:1.2.1/oper-XPDRC.xml
21:roadma-full-1.2.1:1.2.1/oper-ROADMA-full.xml
23:roadmc-full-1.2.1:1.2.1/oper-ROADMC-full.xml
"

IMAGE221_LIST="40:xpdra-2.2.1:2.2.1/oper-XPDRA.xml
41:roadma-2.2.1:2.2.1/oper-ROADMA.xml
42:roadmb-2.2.1:2.2.1/oper-ROADMB.xml
43:roadmc-2.2.1:2.2.1/oper-ROADMC.xml
47:roadmd-2.2.1:2.2.1/oper-ROADMD.xml
44:xpdrc-2.2.1:2.2.1/oper-XPDRC.xml
45:spdra-2.2.1:2.2.1/oper-SPDRA.xml
46:spdrc-2.2.1:2.2.1/oper-SPDRC.xml
25:spdra_no_interface-2.2.1:2.2.1/oper-SPDRA_no_interface.xml
"

case "$DEVICE_VERSION" in
    "1.2.1") IMAGE_LIST=$IMAGE121_LIST
    ;;
    "2.2.1") IMAGE_LIST=$IMAGE221_LIST
    ;;
    "all") IMAGE_LIST=$IMAGE121_LIST" "$IMAGE221_LIST
    ;;
    *) echo "unsupported device version"
    exit 1
    ;;
esac

DOCKER_CMD=docker
DOCKER_IMAGE=honeynode:alpine-jre11
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


