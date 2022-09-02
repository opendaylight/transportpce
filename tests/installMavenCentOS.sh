#!/bin/bash

set -x

#check Java version install if any
JAVA_CMD="java"
[ -n "$JAVA_HOME" ] && JAVA_CMD="$JAVA_HOME/bin/java"
JAVA_VER=$("$JAVA_CMD" -version 2>&1 | sed -n ';s/.* version "\(.*\)\.\(.*\)\..*".*$/\1\2/p;')
echo $JAVA_VER
JAVAC_CMD="javac"
[ -n "$JAVA_HOME" ] && JAVAC_CMD="$JAVA_HOME/bin/javac"
JAVAC_VER=$("$JAVAC_CMD" -version 2>&1 |  sed -n ';s/javac \(.*\)\.\(.*\)\..*.*$/\1\2/p;')
echo $JAVAC_VER
if [ "$JAVA_VER" -ge 110 -a "$JAVAC_VER" -ge 110 ];then
        echo "ok, java is 17 or newer"
else
    #java 11 installation for CentOS (releng OS image target)
        echo "install java 17"
        sudo yum install -y java-17-openjdk java-17-openjdk-devel
        last_installed_jdk17=$(ls -tr1 /usr/lib/jvm/ | grep java-17-openjdk | tail -1)
        if [ -n "$last_installed_jdk17" ];then
            sudo alternatives --set java /usr/lib/jvm/$last_installed_jdk17/bin/java
            sudo alternatives --set javac /usr/lib/jvm/$last_installed_jdk17/bin/javac
        else
           echo "No java17 available"
           exit 1
        fi
fi

#download maven image 3.8.6 and install it
wget -nv https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz -P /tmp
sudo mkdir -p /opt
sudo tar xf /tmp/apache-maven-3.8.6-bin.tar.gz -C /opt
sudo ln -s /opt/apache-maven-3.8.6 /opt/maven
sudo ln -s /opt/maven/bin/mvn /usr/bin/mvn
