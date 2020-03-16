#!/bin/sh -x

. $PWD/dockercmd_profile.sh
${DOCKER_CMD} build --no-cache --tag=honeynode:alpine-jre11 .
