#!/bin/sh

which yum || exit 0
grep 'CentOS Stream release 8' /etc/redhat-release || exit 0
cat /etc/yum.repos.d/CentOS-Stream-AppStream.repo
sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-* || exit 1
sed -i 's@#baseurl=http://mirror.centos.org@baseurl=http://vault.centos.org@g' /etc/yum.repos.d/CentOS-* || exit 1
cat /etc/yum.repos.d/CentOS-Stream-AppStream.repo
