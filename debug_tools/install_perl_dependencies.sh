#!/bin/sh

set +x

#check if apt exists
if sudo apt-get install libnet-openssh-perl libio-pty-perl;then
   echo "installed."
   exit
fi
#check if yum exists
if sudo yum install perl-Net-OpenSSH perl-IO-Tty;then
    echo "yum-get is installed."
    sudo yum install perl-Net-OpenSSH perl-IO-Tty
    exit
fi
#check if cpanm exists
if [ -x "$(command -v cpanm)" ];then
    echo "cpanm is installed."
    cpanm IO::Pty
    cpanm Net::OpenSSH
    exit
else
    echo "cannot install dependencies: apt-get and yum and perlbrew/cpanm are not available." >&2
    exit 1
fi
