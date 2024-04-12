Disclaimer
----------

This software is provided w/o any guarantee and for experimentation or debugging
purposes only. It is not intended to be used for production.

Description
-----------

It aims to proxify on a raw TPC socket the client messages sent to a netconf
server embedded in an openconfig device and to remove from answers any
proprietary extensions found in the hello message and in the XML payload.
As most devices use EXI compression to communicate, it also removes this
capability from both client and sever hello handshakes so that messages can be
parsed in clear text mode.
Note that if the server supports netconf-monitoring, proprietary models will
still be retrieved by get-schema and will appear in the logs.
Note also that server key verifications has been disabled in the perl code call
to OpenSSH.
It can be reestablished by removing or modifying the corresponding option.

Dependencies
------------

You will need Net::OpenSSH and IO::Socket and Net::hostent to run it.
To install them on a brand new debian 12 system, you can simply run as root:

# apt-get install libnet-openssh-perl libio-pty-perl

Raw TCP connections tutorial
----------------------------

On OpenDaylight, you can mount directly a raw TCP netconf server w/o any tweaks.
(parameter 'netconf-node-topology:tcp-only" set to true at device mount-time)
For example, in Calcium, this can be done with the following commands .

Launch OpenDaylight Karaf on a terminal

$ ./karaf/target/assembly/bin/karaf

and in another terminal run the proxy

$ ./debug_tools/openconfig_hijackingproxy.pl login1:passwd1@ipaddress1

by assuming login1, passwd1 and ipaddress1 are the credentails and IP address of
the openconfig device.
In a third terminal, then you can mount this device with the following requests:

$ curl -X PUT http://admin:admin@localhost:8181/rests/data/network-topology:network-topology/topology=topology-netconf/node=nodeopenconfig1
$ curl -X POST http://admin:admin@localhost:8181/rests/data/network-topology:network-topology/topology=topology-netconf \
  -H 'Content-type: application/json' \
  -d'{
      "node": [
          {
              "node-id": "nodeopenconfig1",
              "netconf-node-topology:port": 9000,
              "netconf-node-topology:reconnect-on-changed-schema": false,
              "netconf-node-topology:connection-timeout-millis": 20000,
              "netconf-node-topology:tcp-only": true,
              "netconf-node-topology:max-connection-attempts": 0,
              "netconf-node-topology:login-password-unencrypted": {
                 "netconf-node-topology:username": "login1",
                 "netconf-node-topology:password": "passwd1"
              },
              "netconf-node-topology:host": "127.0.0.1",
              "netconf-node-topology:min-backoff-millis": 2000,
              "netconf-node-topology:max-backoff-millis": 1800000,
              "netconf-node-topology:backoff-multiplier": 1.5,
              "netconf-node-topology:keepalive-delay": 120
          }
      ]
  }'

SSH connections tutorial
------------------------

If you do not want to use raw TCP or if your controller or netconf client does
not support netconf over raw TCP, you can use openssh server subsystems config
and netcat to redirect and decrypt SSH connections on some specific ports to one
or several proxy instances.
Though, you may need to manually stop and restart proxies between each
connection.

For example on a brand new debian 12 system, this set-up can be performed by
running as root the following commands:

# apt install netcat-traditional openssh-server
# adduser proxy_netconf users
[... we assume 'passwd0' was given as a password ]
# echo '
Subsystem    netconf    /usr/bin/nc localhost 9000
Port 830
Port 831
Match LocalPort 22
    DenyUsers    proxy_netconf
Match LocalPort 830
    X11Forwarding no
    AllowTcpForwarding no
    ForceCommand     /usr/bin/nc localhost 9000
Match LocalPort 831
    X11Forwarding no
    AllowTcpForwarding no
    ForceCommand     /usr/bin/nc localhost 9001' >>/etc/ssh/sshd_config
# service ssh restart

And by launching two instances of the proxy on 2 separate (non-root) terminals

$ ./openconfig_hijackingproxy.pl login1:passwd1@ipaddress1


$ ./openconfig_hijackingproxy.pl -p 9001 login2:passwd2@ipaddress2


You can then mount the devices in OpenDaylight Calcium without setting
"netconf-node-topology:tcp-only" parameter to true

$ ./karaf/target/assembly/bin/karaf

$ curl -X PUT http://admin:admin@localhost:8181/rests/data/network-topology:network-topology/topology=topology-netconf/node=nodeopenconfig1
$ curl -X POST http://admin:admin@localhost:8181/rests/data/network-topology:network-topology/topology=topology-netconf -H 'Content-type: application/json' -d'{
      "node": [
          {
              "node-id": "nodeopenconfig1",
              "netconf-node-topology:port": 830,
              "netconf-node-topology:reconnect-on-changed-schema": false,
              "netconf-node-topology:connection-timeout-millis": 20000,
              "netconf-node-topology:tcp-only": false,
              "netconf-node-topology:max-connection-attempts": 0,
              "netconf-node-topology:login-password-unencrypted": {
                 "netconf-node-topology:username": "proxy_netconf",
                 "netconf-node-topology:password": "passwd0"
              },
              "netconf-node-topology:host": "127.0.0.1",
              "netconf-node-topology:min-backoff-millis": 2000,
              "netconf-node-topology:max-backoff-millis": 1800000,
              "netconf-node-topology:backoff-multiplier": 1.5,
              "netconf-node-topology:keepalive-delay": 120
          }
      ]
  }'
$ curl -X PUT http://admin:admin@localhost:8181/rests/data/network-topology:network-topology/topology=topology-netconf/node=nodeopenconfig2
$ curl -X POST http://admin:admin@localhost:8181/rests/data/network-topology:network-topology/topology=topology-netconf -H 'Content-type: application/json' -d'{
      "node": [
          {
              "node-id": "nodeopenconfig2",
              "netconf-node-topology:port": 831,
              "netconf-node-topology:reconnect-on-changed-schema": false,
              "netconf-node-topology:connection-timeout-millis": 20000,
              "netconf-node-topology:tcp-only": false,
              "netconf-node-topology:max-connection-attempts": 0,
              "netconf-node-topology:login-password-unencrypted": {
                 "netconf-node-topology:username": "proxy_netconf",
                 "netconf-node-topology:password": "passwd0"
              },
              "netconf-node-topology:host": "127.0.0.1",
              "netconf-node-topology:min-backoff-millis": 2000,
              "netconf-node-topology:max-backoff-millis": 1800000,
              "netconf-node-topology:backoff-multiplier": 1.5,
              "netconf-node-topology:keepalive-delay": 120
          }
      ]
  }'
