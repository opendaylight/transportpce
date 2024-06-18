#!/bin/sh

set -x

#list Java versions available
update-java-alternatives -l

#select JAVA-17
sudo update-java-alternatives -s java-1.17.0-openjdk-amd64
JAVA_VER=$(java -version 2>&1 | sed -n ';s/.* version "\(.*\)\.\(.*\)\..*".*$/\1/p;')
echo $JAVA_VER
JAVAC_VER=$(javac -version 2>&1 |  sed -n ';s/javac \(.*\)\.\(.*\)\..*.*$/\1/p;')
echo $JAVAC_VER
if [ "$JAVA_VER" -ge 17 -a "$JAVAC_VER" -ge 17 ];then
    echo "ok, java is 17 or newer"
else
   echo "No java17 available"
   exit 1
fi

#download maven image 3.8.8 and install it
wget -nv https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz -P /tmp
sudo mkdir -p /opt
sudo tar xf /tmp/apache-maven-3.8.8-bin.tar.gz -C /opt
sudo ln -s /opt/apache-maven-3.8.8 /opt/maven
sudo ln -s /opt/maven/bin/mvn /usr/bin/mvn

mvn --version
