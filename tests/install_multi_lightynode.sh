#!/bin/sh

#set -x

# Default LightyNode version
LIGHTY_VERSION=${1:-"23.1.1.1"}

# Map each device to its version folder
# Format: device-name|version-folder
# The LightyNode version is passed as the first argument or defaults to 23.1.1.1

if [ $# -le 1 ];then
    # Use all devices
    DEVICES_LIST="lighty-openroadm-device-121|1.2.1 lighty-openroadm-device-221|2.2.1 lighty-openroadm-device-71|7.1 lighty-openconfig-device-oc560|oc560 lighty-openconfig-device-oc200|oc200"
else
    # Use custom devices
    shift
    DEVICES_LIST="$@"
fi

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

for DEVICE_CONFIG in $DEVICES_LIST
do
    # Parse device configuration (format: device-name|version-folder)
    DEVICE_NAME=$(echo "$DEVICE_CONFIG" | cut -d'|' -f1)
    VERSION_FOLDER=$(echo "$DEVICE_CONFIG" | cut -d'|' -f2)

    TARGET_DIR=$(dirname $0)/lightynode/$VERSION_FOLDER
    EXTRACTED_DIR=$TARGET_DIR/$DEVICE_NAME-$LIGHTY_VERSION-bin
    ARTIFACT_ZIPFILE=$TARGET_DIR/artifact-$DEVICE_NAME.zip

    # Create target directory if it doesn't exist
    if ! [ -d "$TARGET_DIR" ];then
        echo "Creating 'lightynode/$VERSION_FOLDER' directory."
        mkdir -p $TARGET_DIR
    fi

    # Check if already installed
    if [ -e $TARGET_DIR/lib ] || [ -e $TARGET_DIR/start-device.sh ];then
        echo "$DEVICE_NAME in version $LIGHTY_VERSION is already installed in $VERSION_FOLDER"
        continue
    fi

    # Clean existing extracted directory
    if [ -d "$EXTRACTED_DIR" ];then
        echo "Removing $EXTRACTED_DIR directory"
        rm -rf $EXTRACTED_DIR
    fi

    # Build the download URL for package registry
    TARGET_URL="https://gitlab.com/api/v4/projects/Orange-OpenSource%2Flfn%2Fodl%2FLightynode-simulator/packages/maven/io/lighty/transportpce/netconf/device/$DEVICE_NAME/$LIGHTY_VERSION/$DEVICE_NAME-$LIGHTY_VERSION-bin.zip"

    # Download and install lightynode device
    # complete source code can be found at https://gitlab.com/Orange-OpenSource/lfn/odl/Lightynode-simulator.git
    echo "Installing $DEVICE_NAME version $LIGHTY_VERSION to $TARGET_DIR directory"
    curl --retry-delay 10 --retry 3 -sS --fail --location --request GET "$TARGET_URL" -o $ARTIFACT_ZIPFILE || { echo "Failed to download $DEVICE_NAME version $LIGHTY_VERSION"; continue; }
    unzip -q -o $ARTIFACT_ZIPFILE -d $TARGET_DIR
    rm -f $ARTIFACT_ZIPFILE

    # Move extracted contents to parent directory (remove intermediate folder)
    EXTRACTED_DIR=$TARGET_DIR/$DEVICE_NAME-$LIGHTY_VERSION
    if [ -d "$EXTRACTED_DIR" ];then
        for file in "$EXTRACTED_DIR"/*; do
            mv "$file" "$TARGET_DIR/"
        done
        rmdir "$EXTRACTED_DIR"
    fi

    echo "Successfully installed $DEVICE_NAME version $LIGHTY_VERSION in $VERSION_FOLDER"
done

echo "Installation of all lightynodes complete!"
exit 0
