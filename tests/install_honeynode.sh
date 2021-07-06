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

for VERSION in $VERSIONS_LIST
do
    case "$VERSION" in
        "1.2.1") PLUGIN_VERSION=1.0.7
        ;;
        "2.2.1") PLUGIN_VERSION=2.0.7
        ;;
        "7.1") PLUGIN_VERSION=7.1.4
        ;;
        *) echo "unsupported device version" >&2
        continue
        ;;
    esac
    TARGET_DIR=$(dirname $0)/honeynode/$VERSION
    INSTALL_DIR=$TARGET_DIR/honeynode-simulator
    ARTIFACT_ZIPFILE=$TARGET_DIR/artifact.zip
    TARGET_URL="https://gitlab.com/api/v4/projects/17518226/jobs/artifacts/honeynode-plugin-aggregator-$PLUGIN_VERSION/download?job=mvn-build"

    #clean honeynode install directory

    if [ -d "$INSTALL_DIR" ];then
        echo "Removing $INSTALL_DIR directory"
        rm -rf $INSTALL_DIR
    fi

    #download honeynode  and install it
    #complete source code can be found at https://gitlab.com/Orange-OpenSource/lfn/odl/honeynode-simulator.git

    echo "Installing honeynode for $VERSION devices to $INSTALL_DIR directory "
    curl --retry-delay 10 --retry 3 -sS --location --request GET $TARGET_URL -o $ARTIFACT_ZIPFILE || exit 2
    unzip -q $ARTIFACT_ZIPFILE -d $TARGET_DIR
    rm -f $ARTIFACT_ZIPFILE

done
exit
