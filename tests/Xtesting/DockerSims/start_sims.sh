#!/bin/sh

echo `sudo docker run -d -p 17830:830  honeynode_oper-roadma:2.1`>>sims.pid
echo `sudo docker run -d -p 17840:830  honeynode_oper-roadma-full:2.1`>>sims.pid
echo `sudo docker run -d -p 17831:830  honeynode_oper-xpdra:2.1`>>sims.pid
echo `sudo docker run -d -p 17833:830  honeynode_oper-roadmc:2.1`>>sims.pid
echo `sudo docker run -d -p 17843:830  honeynode_oper-roadmc-full:2.1`>>sims.pid
echo `sudo docker run -d -p 17834:830  honeynode_oper-xpdrc:2.1`>>sims.pid

echo -n "#!/bin/sh\n\nsudo docker container kill "`cat sims.pid`" \n" >kill_sims.sh
chmod +x kill_sims.sh
