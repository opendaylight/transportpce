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

my ($host, $help, $usage,  $capabilities, $login, $password, $kidpid, $hello_message);


GetOptions (
    "h|help" =>\$help,
    "C|capabilities=s"=>\$capabilities
);
$usage = "
USAGE: netconf_terminal.pl [-h|--help] [-C|--capabilities <custom_hello_file.xml>] <[login[:password]@]host[:port]> [login] [password]

Simple netconf terminal client that can be used as an alternative to 'openssh [-p port] <[login@]host> -s netconf'.
The main difference is the built-in handshake phase with hello capabilties that can be loaded from an external file.
This is particularly useful to avoid timeouts.

OPTIONS :

        -C or --capabilities     use the given file to advertise a hello message with customized capabilities
        -h or --help             print this help

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
if ($host !~ /:[0-9]+$/) {
    $host.=':830';
}

my $connection_string=$host;
if ($password) {
   $connection_string=$login.":".$password."@".$connection_string;
} elsif ($login) {
   $connection_string=$login."@".$connection_string;
}

#retrieving hello custom file if any
if (defined ($capabilities)) {
    open(CAPABILITIES,'<',$capabilities) or die ("can not open $capabilities") ;
    while (<CAPABILITIES>) {
        $hello_message .= $_;
    }
    chop $hello_message; # removing EOF
    $hello_message.="\n]]>]]>\n";
    close(CAPABILITIES);
}
#otherwise using a basic hello message
#EXI extension is not advertised by default since difficult to handle manually
else{
    $hello_message='<?xml version="1.0" encoding="utf-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<capabilities>
<capability>urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring?module=ietf-netconf-monitoring&amp;revision=2010-10-04</capability>
<capability>urn:ietf:params:xml:ns:yang:ietf-netconf-notifications?module=ietf-netconf-notifications&amp;revision=2012-02-06</capability>
<capability>urn:ietf:params:xml:ns:netconf:base:1.0?module=ietf-netconf&amp;revision=2011-06-01</capability>
<capability>urn:ietf:params:xml:ns:yang:ietf-yang-types?module=ietf-yang-types&amp;revision=2013-07-15</capability>
<capability>urn:ietf:params:netconf:capability:candidate:1.0</capability>
<capability>urn:ietf:params:xml:ns:netconf:notification:1.0?module=notifications&amp;revision=2008-07-14</capability>
<capability>urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring-extension?module=ietf-netconf-monitoring-extension&amp;revision=2013-12-10</capability>
<capability>urn:ietf:params:netconf:base:1.0</capability>
<capability>urn:ietf:params:xml:ns:yang:iana-afn-safi?module=iana-afn-safi&amp;revision=2013-07-04</capability>
<capability>urn:ietf:params:xml:ns:yang:ietf-inet-types?module=ietf-inet-types&amp;revision=2013-07-15</capability>
</capabilities>
</hello>';
    $hello_message.="\n]]>]]>\n";
}


print STDERR "connecting to ".$connection_string."\n";

my $ssh_handle= Net::OpenSSH->new($connection_string,
                                  master_opts => [-o => 'StrictHostKeyChecking=no'],
                                  timeout => 500, kill_ssh_on_timeout => 500);

#netconf requires a specific socket
my ($ssh_subsocket, $pid) = $ssh_handle->open2socket({ssh_opts => '-s'}, 'netconf');
die "can't establish connection: exiting\n" unless defined($ssh_subsocket);

print STDERR "[Connected]\n";

# split the program into two processes, identical twins
die "can't fork: $!" unless defined($kidpid = fork());


# the if{} block runs only in the parent process (terminal output)
if (!$kidpid) {

    $|=1;

    # copy the socket to standard output
    my $buf;
    my $nread;
    #while (<$ssh_subsocket>) {
    #buffer seems not totally flushed when using the syntax above (nor when using autoflush)
    while ($nread = sysread($ssh_subsocket,$buf,150)) {
        print $buf;
        $ssh_subsocket->flush();
    };

    print;
    kill("TERM", $kidpid);                  # send SIGTERM to child
}
# the else{} block runs only in the child process (terminal input)
else {

   $ssh_subsocket->autoflush(1);
   sleep 1;                                 # wait needed for ensuring STDOUT buffer is not melt

   if (defined ($hello_message)) {
       print $ssh_subsocket $hello_message;
       sleep 1;
   }

   while (defined (my $line = <STDIN>)) {
      print $ssh_subsocket $line;
   }

}

sleep 2;
kill("TERM", $kidpid);                      # send SIGTERM to child
exit;
