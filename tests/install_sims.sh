#!/bin/sh

#set -x

cd $(dirname $0)

if [ "$USE_SIMS" = "lightynode" ];then
    ./install_lightynode.sh
else
    ./install_honeynode.sh $@
fi

exit
