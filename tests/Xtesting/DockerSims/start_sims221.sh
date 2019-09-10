#!/bin/sh

echo "killing previous sims containers"
if [ -f kill_sims221.sh ]; then ./kill_sims221.sh; rm sims221.pid kill_sims221.sh; else echo "nothing to do"; fi

echo `sudo docker run -d -p 17840:830  honeynode_oper-xpdra:2.2.1`>>sims221.pid
echo `sudo docker run -d -p 17841:830  honeynode_oper-roadma:2.2.1`>>sims221.pid
#echo `sudo docker run -d -p 17842:830  honeynode_oper-roadmb:2.2.1`>>sims221.pid
echo `sudo docker run -d -p 17843:830  honeynode_oper-roadmc:2.2.1`>>sims221.pid
echo `sudo docker run -d -p 17844:830  honeynode_oper-xpdrc:2.2.1`>>sims221.pid

echo -n "#!/bin/sh\n\nsudo docker container kill "`cat sims221.pid`" \n" >kill_sims221.sh
chmod +x kill_sims221.sh
