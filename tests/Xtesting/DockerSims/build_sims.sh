#!/bin/sh -x

. $(dirname $0)/dockercmd_profile.sh
${DOCKER_CMD} build --no-cache --tag=honeynode:alpine-jre11 .
