#/bin/bash

sudo yum install -y java-11-openjdk java-11-openjdk-devel
last_installed_jdk11=$(ls -tr1 /usr/lib/jvm/ | grep java-11 | tail -1)
if [ -n "$last_installed_jdk11" ];then
  sudo alternatives --set java /usr/lib/jvm/$last_installed_jdk11/bin/java
  sudo alternatives --set javac /usr/lib/jvm/$last_installed_jdk11/bin/javac
else
  echo "No java11 available"
  exit 1
fi
echo 'export JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which javac)))))'>jdk_env.sh
echo 'export PATH=$PATH:$JAVA_HOME/bin' >>jdk_env.sh
