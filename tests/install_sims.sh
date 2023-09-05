#!/bin/sh

#set -x

cd $(dirname $0)

if [ "$USE_SIMS" = "honeynode" ];then
    ./install_honeynode.sh $@
else
    ./install_lightynode.sh
fi

exit
