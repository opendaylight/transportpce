#/bin/bash

#sudo yum remove -y java-1.8.0-openjdk-devel java-1.8.0-openjdk
#sudo yum check-update
sudo yum install -y java-11-openjdk java-11-openjdk-devel
sudo alternatives --set java /usr/lib/jvm/java-11-openjdk-11.0.6.10-1.el7_7.x86_64/bin/java
sudo alternatives --set javac /usr/lib/jvm/java-11-openjdk-11.0.6.10-1.el7_7.x86_64/bin/javac
echo 'export JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which javac)))))'>jdk_env.sh
echo 'export PATH=$PATH:$JAVA_HOME/bin' >>jdk_env.sh
