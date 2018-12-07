#/bin/bash

sudo yum install -y java-1.8.0-openjdk-devel
wget -nv https://www-us.apache.org/dist/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.tar.gz -P /tmp
sudo mkdir -p /opt
sudo tar xf /tmp/apache-maven-3.6.0-bin.tar.gz -C /opt
sudo ln -s /opt/apache-maven-3.6.0 /opt/maven
sudo ln -s /opt/maven/bin/mvn /usr/bin/mvn
