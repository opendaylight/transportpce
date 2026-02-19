#!/bin/bash
#
# Run script for NETCONF Proxy
#

JAR_FILE="target/netconf-proxy-1.0.0-SNAPSHOT.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found: $JAR_FILE"
    echo "Please build the project first:"
    echo "  ./build.sh"
    exit 1
fi

# Check for config file argument
if [ $# -eq 1 ]; then
    CONFIG_FILE=$1
    if [ ! -f "$CONFIG_FILE" ]; then
        echo "Error: Config file not found: $CONFIG_FILE"
        exit 1
    fi
    echo "Using config file: $CONFIG_FILE"
    java -Dconfig.file="$CONFIG_FILE" -jar "$JAR_FILE"
else
    echo "Using default configuration"
    java -jar "$JAR_FILE"
fi
