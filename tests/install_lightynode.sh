#!/bin/sh

#set -x

VERSIONS_LIST=${@:-"1.2.1 2.2.1 7.1"}

#check if curl exists
if ! [ -x "$(command -v curl)" ];then
    echo "curl is not installed." >&2
    exit 1
fi
#check if unzip exists
if ! [ -x "$(command -v unzip)" ];then
    echo "unzip is not installed." >&2
    exit 1
fi

PLUGIN_VERSION=18.1.0.7
TARGET_DIR=$(dirname $0)/lightynode
INSTALL_DIR=$TARGET_DIR/lightynode-simulator
ARTIFACT_ZIPFILE=$TARGET_DIR/artifact.zip
TARGET_URL="https://gitlab.com/api/v4/projects/36076125/packages/maven/io/lighty/transportpce/netconf/device/lighty-openroadm-device/$PLUGIN_VERSION/lighty-openroadm-device-$PLUGIN_VERSION-bin.zip"

#clean lightynode install directory
if [ -d "$INSTALL_DIR" ];then
    echo "Removing $INSTALL_DIR directory"
    rm -rf $INSTALL_DIR
fi

#download lightynode  and install it
#complete source code can be found at https://gitlab.com/Orange-OpenSource/lfn/odl/lightynode-simulator.git
echo "Installing lightynode device to $INSTALL_DIR directory "
curl --retry-delay 10 --retry 3 -sS --location --request GET $TARGET_URL -o $ARTIFACT_ZIPFILE || exit 2
unzip -q $ARTIFACT_ZIPFILE -d $TARGET_DIR
rm -f $ARTIFACT_ZIPFILE
mv $TARGET_DIR/lighty-openroadm-device-$PLUGIN_VERSION $TARGET_DIR/lighty-openroadm-device
