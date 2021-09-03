#!/bin/sh

# more details at https://thesynack.com/posts/docker-compose-podman/

DOCKERCOMPOSE_CMD=docker-compose
if [ ! `which $DOCKERCOMPOSE_CMD` ]; then
    echo "no docker-compose command available" >&2
    exit 1
fi

if [ `which podman` ]; then
    if curl --unix-socket /run/podman/podman.sock http://localhost/_ping ; then
        echo
        echo "podman service socket found"
        DOCKER_HOST=unix:///run/podman/podman.sock docker-compose $@
        exit $?
    else
        if sudo curl --unix-socket /run/podman/podman.sock http://localhost/_ping ; then
            echo "podman executable detected but podman service socket only usable by root - using sudo" >&2
            sudo DOCKER_HOST=unix:///run/podman/podman.sock docker-compose $@
            exit $?
        else
            echo "podman executable detected but no podman service socket found" >&2
            exit 1
        fi
    fi
else
    docker-compose $@ || sudo docker-compose $@
fi

