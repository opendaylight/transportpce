#!/bin/sh

#set -x

PLUGIN_VERSION=${1:-20.1.0.4}

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

if [ -n "$2" ];then
    echo "Using lighynode version $1 $2"
    FILE_NAME="lighty-openroadm-device-$(echo $1 | cut -d - -f 1)-$2-bin.zip"
    TARGET_URL="https://gitlab.com/api/v4/projects/36076125/packages/maven/io/lighty/transportpce/netconf/device/lighty-openroadm-device/$PLUGIN_VERSION/${FILE_NAME}"
else
    echo "Using lighynode version $PLUGIN_VERSION"
    TARGET_URL="https://gitlab.com/api/v4/projects/36076125/packages/maven/io/lighty/transportpce/netconf/device/lighty-openroadm-device/$PLUGIN_VERSION/lighty-openroadm-device-$PLUGIN_VERSION-bin.zip"
fi

TARGET_DIR=$(dirname $0)/lightynode
INSTALL_DIR=$TARGET_DIR/lightynode-openroadm-device
ARTIFACT_ZIPFILE=$TARGET_DIR/artifact.zip
if ! [ -d "$TARGET_DIR" ];then
    echo "Creating 'lightynode' directory."
    mkdir lightynode
fi

if [ -e $INSTALL_DIR/lighty-openroadm-device-$PLUGIN_VERSION.jar ];then
    echo "lightynode simulator in version $PLUGIN_VERSION is alreay installed"
    exit 0
fi

#clean lightynode install directory
if [ -d "$INSTALL_DIR" ];then
    echo "Removing $INSTALL_DIR directory"
    rm -rf $INSTALL_DIR
fi

#download lightynode  and install it
#complete source code can be found at https://gitlab.com/Orange-OpenSource/lfn/odl/lightynode-simulator.git
echo "Installing lightynode device to $INSTALL_DIR directory "
curl --retry-delay 10 --retry 3 -sS --fail --location --request GET $TARGET_URL -o $ARTIFACT_ZIPFILE || exit 2
unzip -q -o $ARTIFACT_ZIPFILE -d $TARGET_DIR
rm -f $ARTIFACT_ZIPFILE
mv $TARGET_DIR/lighty-openroadm-device-$PLUGIN_VERSION $INSTALL_DIR

#update 1.2.1 openroadm device configuration samples to be compliant with 1.2.1
#device models ("multi-wavelegnth" instead of "multi-wavelength" whose typo has been solved with 2.2.1 device models)
sed -i_ 's/multi-wavelength/multi-wavelegnth/g' $(dirname $0)/sample_configs/openroadm/1.2.1/oper-ROADM*.xml
