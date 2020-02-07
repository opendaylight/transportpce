#/bin/bash

#sudo yum remove -y java-1.8.0-openjdk-devel java-1.8.0-openjdk
#sudo yum check-update
sudo yum install -y java-11-openjdk java-11-openjdk-devel
last_installed_jdk11=$(ls -tr1 /usr/lib/jvm/ | grep java-11-openjdk-11 | tail -1)
if [ -n "$last_installed_jdk11" ];then
  sudo alternatives --set java /usr/lib/jvm/$last_installed_jdk11/bin/java
  sudo alternatives --set javac /usr/lib/jvm/$last_installed_jdk11/bin/javac
else
  echo "No java11 available"
  exit 1
fi
