#/bin/bash

wget -nv https://www-us.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -P /tmp
sudo mkdir -p /opt
sudo tar xf /tmp/apache-maven-3.6.3-bin.tar.gz -C /opt
sudo ln -s /opt/apache-maven-3.6.3 /opt/maven
sudo ln -s /opt/maven/bin/mvn /usr/bin/mvn
touch FLAG_TPCE_RELENG
