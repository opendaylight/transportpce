#!/bin/sh

echo "killing previous sims containers"
if [ -f kill_sims21.sh ]; then ./kill_sims21.sh; rm sims21.pid kill_sims21.sh; else echo "nothing to do"; fi

echo `sudo docker run -d -p 17830:830  honeynode_oper-xpdra:2.1`>>sims21.pid
echo `sudo docker run -d -p 17831:830  honeynode_oper-roadma:2.1`>>sims21.pid
echo `sudo docker run -d -p 17832:830  honeynode_oper-roadmb:2.1`>>sims21.pid
echo `sudo docker run -d -p 17833:830  honeynode_oper-roadmc:2.1`>>sims21.pid
echo `sudo docker run -d -p 17834:830  honeynode_oper-xpdrc:2.1`>>sims21.pid
echo `sudo docker run -d -p 17821:830  honeynode_oper-roadma-full:2.1`>>sims21.pid
echo `sudo docker run -d -p 17823:830  honeynode_oper-roadmc-full:2.1`>>sims21.pid

echo -n "#!/bin/sh\n\nsudo docker container kill "`cat sims21.pid`" \n" >kill_sims21.sh
chmod +x kill_sims21.sh
