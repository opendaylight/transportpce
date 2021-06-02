#!/usr/bin/env perl
##############################################################################
#Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
##############################################################################
#
# debian dependencies: apt-get install libnet-openssh-perl libio-pty-perl
#

use strict;
use warnings;
#use diagnostics;  #uncomment this line for more details when encountering warnings
use Net::OpenSSH;
use FileHandle;
use Getopt::Long qw(:config no_ignore_case bundling);

use IO::Socket;
use Net::hostent;

my ($host, $help, $usage,  $proxy_port, $login, $password, $kidpid, $ssh_subsocket, $simpleproxy,
    $pid, $ssh_handle, $client, $server, $capabilities, $hello_message, $verbose);

GetOptions (
    "h|help" =>\$help,
    "p|port=i"=>\$proxy_port,
    "s|simpleproxy" =>\$simpleproxy,
    "v|verbose" =>\$verbose,
    "C|capabilities=s"=>\$capabilities
);
$usage = "
USAGE: netconf_TCP_SSH_hijackproxy.pl [-h|--help] [-p|--port <port_number>] [-s|--simpleproxy] [-v|--verbose] [-C|--capabilities <custom_hello_file.xml>] <[login[:password]@]host[:port]> [login] [password]

Netconf SSH to TCP proxy to debug netconf exchanges.
It listens to connections in clear TCP to the given port. When a TCP connection demand is received,
it establishes a netconf SSH encrypted connection to the host in argument. Netconf rpcs and replies
are then proxified between both ends.
By default, exchanges are altered according to the rules specified inside this script and easily
modifiable. This behaviour can be disabled with the '-s' option.
For more convenience, the server hello handshake can also alternatively be replaced by the content
of an external file instead of writing specific rules.

OPTIONS :

        -h or --help             print this help
        -p or --port             use the given port number for listening TCP clients, default=9000
        -s or --simpleproxy      simple proxy mode, do not alter any exchanges
        -v or --verbose          display exchanges to STDOUT
        -C or --capabilities     do not relay the real server hello message to the client
                                 but replace it by the one provided in the following file


";

if ($help) {
    print $usage;
    exit(0);
}

unless (@ARGV >= 1) {
    print $usage;
    exit(0);
}

($host, $login, $password) = @ARGV;

#netconf default port is no 22 but 830
if ($host !~ /:[0-9]+$/) { $host.=':830'; }

if (!defined($proxy_port)) { $proxy_port = 9000; }

my $connection_string=$host;
if ($password) {
   $connection_string=$login.":".$password."@".$connection_string;
} elsif ($login) {
   $connection_string=$login."@".$connection_string;
}

#retrieving hello custom file if any
if ((!defined ($simpleproxy))&&(defined ($capabilities))) {
    open(CAPABILITIES,'<',$capabilities) or die ("can not open $capabilities") ;
    while (<CAPABILITIES>) {
        $hello_message .= $_;
    }
    chop $hello_message; # removing EOF
    $hello_message.="]]>]]>";
    close(CAPABILITIES);
}

# the following regex are used to modify some part of the server messages relayed to the client
# you can adapt it to your needs, some examples have been commented.
my %regex_hash=(
# replace oo-device v1.2 by v1.2.1
#   'module=org-openroadm-device&amp;revision=2016-10-14.*<\/capability>'=>'s/&amp;revision=2016-10-14/&amp;revision=2017-02-06/',
#   '<schema><identifier>org-openroadm-device<\/identifier><version>2016-10-14'=>'s@<schema><identifier>org-openroadm-device</identifier><version>2016-10-14@<schema><identifier>org-openroadm-device</identifier><version>2017-02-06@',
# remove all deviations found
#   '&amp;deviations=.*<\/capability>'=>'s@&amp;deviations=.*</capability>@</capability>@',
# add the ietf-netconf capability to the hello handshake - without it, ODL netconf mountpoints can not work
#    '<\/capabilities>'=>'s@</capabilities>@\n<capability>urn:ietf:params:xml:ns:yang:ietf-netconf?module=ietf-netconf&amp;revision=2011-06-01</capability>\n</capabilities>@',
# add the right notifications capabilities to the hello handshake + provide another solution for the ietf-netconf capability
    '<\/capabilities>'=>'s@</capabilities>@\n<capability>urn:ietf:params:xml:ns:netmod:notification?module=nc-notifications&amp;revision=2008-07-14</capability>\n<capability>urn:ietf:params:xml:ns:netconf:notification:1.0?module=notifications&amp;revision=2008-07-14</capability>\n<capability>urn:ietf:params:xml:ns:netconf:base:1.0?module=ietf-netconf&amp;revision=2011-06-01</capability>\n</capabilities>@'
);

if (defined ($simpleproxy)) { %regex_hash=(); }

my %compiled_regex_hash;
foreach my $keyword (keys %regex_hash){
    eval ('$compiled_regex_hash{$keyword}= qr/'.$keyword.'/;');
}

$server = IO::Socket::INET->new( Proto     => 'tcp',
                                 LocalPort => $proxy_port,
                                 Listen    => SOMAXCONN,
                                 Reuse     => 1);
die "can't setup server" unless $server;
print STDERR "[Proxy server $0 accepting clients: Ctrl-C to stop]\n";


while ($client = $server->accept()) {
  $client->autoflush(1);
  my $hostinfo = gethostbyaddr($client->peeraddr);
  printf STDERR "[Incoming connection from %s]\n", $hostinfo->name || $client->peerhost;


print STDERR "[relaying to ".$connection_string."]\n";

$ssh_handle = Net::OpenSSH->new($connection_string,
                                master_opts => [-o => 'StrictHostKeyChecking=no'],
                                timeout => 500, kill_ssh_on_timeout => 500);

#netconf requires a specific socket
($ssh_subsocket, $pid) = $ssh_handle->open2socket({ssh_opts => '-s'}, 'netconf');
die "can't establish connection: exiting\n" unless defined($ssh_subsocket);

print STDERR "[Connected]\n";

# split the program into two processes, identical twins
die "can't fork: $!" unless defined($kidpid = fork());

$|=1;

# the if{} block runs only in the parent process (server output relayed to the client)
if (!$kidpid) {

    # copy the socket to standard output
    my $buf;

    if (defined ($hello_message)) {
        #retrieve the server hello but do not relay it
        while (my $nread = sysread($ssh_subsocket,$buf,400)) {
            $ssh_subsocket->flush();
            if ($buf =~ /]]>]]>/) { last };
        };
        #send a custom hello message instead
        print $client $hello_message;
        if (defined($verbose))  { print STDOUT  $hello_message; }
    }

    #while (<$ssh_subsocket>) {
    #buffer seems not totally flushed when using the usual syntax above (nor when using autoflush)
    while (my $nread = sysread($ssh_subsocket,$buf,400)) {
        foreach my $keyword (keys %regex_hash){
           if($buf =~ $compiled_regex_hash{$keyword}){
               print STDERR 'found regex '.$keyword.": replacing '\n".$buf."\n' by '\n";
               eval ('$buf =~ '.$regex_hash{$keyword}.';');
               print STDERR $buf."\n'\n";
           }
        }
        print $client $buf;
        $ssh_subsocket->flush();
        if (defined($verbose))  { print STDOUT  $buf; }

    };

    kill("TERM", $kidpid);              # send SIGTERM to child
}
# the else{} block runs only in the child process (client input relayed to the server)
else {

   $ssh_subsocket->autoflush(1);
   sleep 1;                             # wait needed for ensuring STDOUT buffer is not melt
   my $buf;

   #while (defined (my $buf = <$client>)) {
   #usual syntax above used in verbose mode results into flush problems
   while (my $nread = sysread($client,$buf,400)) {
      print $ssh_subsocket $buf;
      $client->flush();
      if (defined($verbose))  { print STDOUT  $buf; }
   }continue {}

   close $client;

}

$|=0;

sleep 2;
kill("TERM", $kidpid);                  # send SIGTERM to child

}

exit;
