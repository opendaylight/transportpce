#/bin/bash
set -x
JAVA_CMD="java"
[ -n "$JAVA_HOME" ] && JAVA_CMD="$JAVA_HOME/bin/java"
JAVA_VER=$("$JAVA_CMD" -version 2>&1 | sed -n ';s/.* version "\(.*\)\.\(.*\)\..*".*$/\1\2/p;')
echo $JAVA_VER
JAVAC_CMD="javac"
[ -n "$JAVA_HOME" ] && JAVAC_CMD="$JAVA_HOME/bin/javac"
JAVAC_VER=$("$JAVAC_CMD" -version 2>&1 |  sed -n ';s/javac \(.*\)\.\(.*\)\..*.*$/\1\2/p;')
echo $JAVAC_VER
if [ "$JAVA_VER" -ge 110 -a "$JAVAC_VER" -ge 110 ];then
       	echo "ok, java is 11 or newer" 
else
	echo "install java 11"
        sudo yum install -y java-11-openjdk java-11-openjdk-devel
        last_installed_jdk11=$(ls -tr1 /usr/lib/jvm/ | grep java-11-openjdk-11 | tail -1)
        if [ -n "$last_installed_jdk11" ];then
            sudo alternatives --set java /usr/lib/jvm/$last_installed_jdk11/bin/java
            sudo alternatives --set javac /usr/lib/jvm/$last_installed_jdk11/bin/javac
        else
           echo "No java11 available"
           exit 1
        fi
fi
echo 'export JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which javac)))))'>jdk_env.sh
echo 'export PATH=$PATH:$JAVA_HOME/bin' >>jdk_env.sh
chmod 755 jdk_env.sh
