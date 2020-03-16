#!/bin/sh -x
if id -nG "$USER" | grep -qw "docker"; then
    echo "$USER belongs to docker group"
    docker build --no-cache --tag=honeynode:alpine-jre11 .
else
    echo "$USER does not belong to docker group"
    sudo docker build --no-cache --tag=honeynode:alpine-jre11 .
fi
