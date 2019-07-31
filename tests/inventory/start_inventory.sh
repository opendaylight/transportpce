#/bin/bash

#to use unix socket, we need to bind the host and container directories
#since UID are different, we also need to change the directory permissions on the host system

#sudo service mysql stop
sudo mkdir -p /var/run/mysqld/
sudo chmod 777 /var/run/mysqld/
sudo docker run --name inventory0 -p 3306:3306 -v /var/run/mysqld/:/var/run/mysqld/ tpce/inventory
#sudo chmod 755 /var/run/mysqld/
