#!/bin/bash
#
# Build script for NETCONF Proxy
#

set -e

echo "=================================================="
echo "  Building NETCONF Proxy"
echo "=================================================="

# Check Java version
echo ""
echo "Checking Java version..."
java -version 2>&1 | head -1

# Check Maven
echo ""
echo "Checking Maven..."
mvn -version | head -1

# Clean and build
echo ""
echo "Building project..."
mvn clean package -DskipTests

# Check if build succeeded
if [ -f target/netconf-proxy-1.0.0-SNAPSHOT.jar ]; then
    echo ""
    echo "=================================================="
    echo "  Build Successful!"
    echo "=================================================="
    echo ""
    echo "JAR file created: target/netconf-proxy-1.0.0-SNAPSHOT.jar"
    echo ""
    echo "To run:"
    echo "  java -jar target/netconf-proxy-1.0.0-SNAPSHOT.jar"
    echo ""
    echo "To run with custom config:"
    echo "  java -jar target/netconf-proxy-1.0.0-SNAPSHOT.jar /path/to/config.conf"
    echo ""
else
    echo ""
    echo "=================================================="
    echo "  Build Failed!"
    echo "=================================================="
    exit 1
fi
