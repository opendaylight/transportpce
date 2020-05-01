#/bin/bash

#to use unix socket, we need to bind the host and container directories
#since UID are different, we also need to change the directory permissions on the host system

sudo docker run --name mongodb0 -p 27017:27017 -v /opt/mongodb:/data/db mongo:latest
