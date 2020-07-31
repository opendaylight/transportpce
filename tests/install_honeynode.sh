#/bin/sh

#set -x

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

#clean honeynode directories

if [ -d "$(dirname $0)/honeynode/1.2.1/honeynode-simulator" ];then
    echo "Removing $(dirname $0)/honeynode/1.2.1/honeynode-simulator directory"
    rm -rf $(dirname $0)/honeynode/1.2.1/honeynode-simulator
fi
if [ -d "$(dirname $0)/honeynode/2.2.1/honeynode-simulator" ];then
    echo "Removing $(dirname $0)/honeynode/2.2.1/honeynode-simulator directory"
    rm -rf $(dirname $0)/honeynode/2.2.1/honeynode-simulator
fi
#download honeynode for 1.2.1 devices and install it
#complete source code can be found at https://gitlab.com/Orange-OpenSource/lfn/odl/honeynode-simulator.git
echo "Installing honeynode for 1.2.1 devices to $(dirname $0)/honeynode/1.2.1/honeynode-simulator directory "
curl --location --request GET "https://gitlab.com/api/v4/projects/17518226/jobs/artifacts/honeynode-plugin-aggregator-1.0.3/download?job=mvn-build" -o $(dirname $0)/honeynode/1.2.1/artifact.zip
unzip $(dirname $0)/honeynode/1.2.1/artifact.zip -d $(dirname $0)/honeynode/1.2.1
rm -f $(dirname $0)/honeynode/1.2.1/artifact.zip
#download honeynode for 2.2.1 devices and install it
echo "Installing honeynode for 2.2.1 devices to $(dirname $0)/honeynode/2.2.1/honeynode-simulator directory "
curl --location --request GET "https://gitlab.com/api/v4/projects/17518226/jobs/artifacts/honeynode-plugin-aggregator-2.0.4/download?job=mvn-build" -o $(dirname $0)/honeynode/2.2.1/artifact.zip
unzip $(dirname $0)/honeynode/2.2.1/artifact.zip -d $(dirname $0)/honeynode/2.2.1
rm -f $(dirname $0)/honeynode/2.2.1/artifact.zip
