#!/usr/bin/env perl
##############################################################################
#Copyright (c) 2024 Orange, Inc. and others.  All rights reserved.
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

my (
    $host,         $help,          $usage,  $proxy_port,
    $login,        $password,      $kidpid, $ssh_subsocket,
    $pid,          $ssh_handle,    $client, $server,
    $capabilities, $hello_message, $verbose
);

GetOptions(
    "h|help"           => \$help,
    "p|port=i"         => \$proxy_port,
    "v|verbose"        => \$verbose,
    "C|capabilities=s" => \$capabilities
);
$usage = "
USAGE: openconfig_hijackingproxy.pl [-h|--help] [-p|--port <port_number>] [-v|--verbose] [-C|--capabilities <custom_hello_file.xml>] <[login[:password]@]host[:port]> [login] [password]

Netconf SSH to TCP proxy to debug netconf exchanges and remove openconfig vendors proprietary
extensions from XML payload.
It listens to connections in clear TCP to the given port. When a TCP connection demand is received,
it establishes a netconf SSH encrypted connection to the host in argument. Netconf rpcs and replies
are then proxified between both ends.
By default, exchanges are altered according to the rules specified inside this script and easily
modifiable.
For more convenience, the server hello handshake can also alternatively be replaced by the content
of an external file instead of writing specific rules.

OPTIONS :

        -h or --help             print this help
        -p or --port             use the given port number for listening TCP clients, default=9000
        -v or --verbose          display exchanges to STDOUT
        -C or --capabilities     do not relay the real server hello message to the client
                                 but replace it by the one provided in the following file


";

if ($help) {
    print $usage;
    exit(0);
}

unless ( @ARGV >= 1 ) {
    print $usage;
    exit(0);
}

( $host, $login, $password ) = @ARGV;

#netconf default port is no 22 but 830
if ( $host !~ /:[0-9]+$/ ) { $host .= ':830'; }

if ( !defined($proxy_port) ) { $proxy_port = 9000; }

my $connection_string = $host;
if ($password) {
    $connection_string = $login . ":" . $password . "@" . $connection_string;
}
elsif ($login) {
    $connection_string = $login . "@" . $connection_string;
}

#retrieving hello custom file if any
if ( defined($capabilities) ) {
    open( CAPABILITIES, '<', $capabilities )
      or die("can not open $capabilities");
    while (<CAPABILITIES>) {
        $hello_message .= $_;
    }
    chop $hello_message;    # removing EOF
    $hello_message .= "]]>]]>";
    close(CAPABILITIES);
}

# the following regex are used to modify some part of the server messages relayed to the client
# you can adapt it to your needs, some examples have been commented.
my %regex_hash = (

    # this removes EXI capability from hello messages
    '<capability>urn:ietf:params:netconf:capability:exi:1.0<\/capability>' =>
      's@<capability>urn:ietf:params:netconf:capability:exi:1.0</capability>@@',

    # this removes some proprietary extensions capabilities from hello messages
    '<capability>http:\/\/nokia\.com\/[^<]*<\/capability>' =>
      's@<capability>http://nokia\.com/[^<]*</capability>@@g',
    '<capability>http:\/\/nec\.com/[^<]*<\/capability>' =>
      's@<capability>http://nec\.com/[^<]*</capability>@@g'
);

my %compiled_regex_hash = ();
foreach my $keyword ( keys %regex_hash ) {
    eval( '$compiled_regex_hash{$keyword}= qr/' . $keyword . '/;' );
}

my %regex_hash2 = (

 # this removes some proprietary extensions capabilities from server XML payload
    '\Q"http:\/\/nokia.com\/\E' => 's@="http://nokia\.com.*@>@g',
    '\Q"http:\/\/nec.com\/\E'   => 's@="http://nec\.com.*@>@g'
);

my %compiled_regex_hash2 = ();
foreach my $keyword ( keys %regex_hash2 ) {
    eval( '$compiled_regex_hash2{$keyword}= qr/' . $keyword . '/;' );
}

$server = IO::Socket::INET->new(
    Proto     => 'tcp',
    LocalPort => $proxy_port,
    Listen    => SOMAXCONN,
    Reuse     => 1
);
die "can't setup server" unless $server;
print STDERR "[Proxy server $0 accepting clients: Ctrl-C to stop]\n";

while ( $client = $server->accept() ) {
    $client->autoflush(1);
    my $hostinfo = gethostbyaddr( $client->peeraddr );
    printf STDERR "[Incoming connection from %s]\n",
      $hostinfo->name || $client->peerhost;

    print STDERR "[relaying to " . $connection_string . "]\n";

    $ssh_handle = Net::OpenSSH->new(
        $connection_string,
        master_opts => [
            -o => 'StrictHostKeyChecking=no',

            # force to disable server key verification
            -o => 'UserKnownHostsFile=/dev/null'
        ],
        timeout             => 500,
        kill_ssh_on_timeout => 500
    );

    #netconf requires a specific socket
    ( $ssh_subsocket, $pid ) =
      $ssh_handle->open2socket( { ssh_opts => '-s' }, 'netconf' );
    die "can't establish connection: exiting\n" unless defined($ssh_subsocket);

    print STDERR "[Connected]\n";

    # split the program into two processes, identical twins
    die "can't fork: $!" unless defined( $kidpid = fork() );

    $| = 1;

# the if{} block runs only in the parent process (server output relayed to the client)
    if ( !$kidpid ) {

        # copy the socket to standard output
        my $buf;
        ## hello message
        if ( defined($hello_message) ) {

            #retrieve the server hello but do not relay it
            while ( my $nread = sysread( $ssh_subsocket, $buf, 400 ) ) {
                $ssh_subsocket->flush();
                if ( $buf =~ /]]>]]>/ ) { last; }
            }

            #send a custom hello message instead
            print $client $hello_message;
            $ssh_subsocket->flush();
            if ( defined($verbose) ) { print STDOUT $hello_message; }
        }
        else {
#while (<$ssh_subsocket>) {
#buffer seems not totally flushed when using the usual syntax above (nor when using autoflush)
            my $buf2 = '';
            while ( my $nread0 = sysread( $ssh_subsocket, $buf, 400 ) ) {
                $buf2 .= $buf;
                if ( $buf =~ /]]>]]>/ ) { last; }
            }
            foreach my $keyword ( keys %regex_hash ) {
                if ( $buf2 =~ $compiled_regex_hash{$keyword} ) {
                    print STDERR 'found regex '
                      . $keyword
                      . ": replacing '\n"
                      . $buf2
                      . "\n' by '\n";
                    eval( '$buf2 =~ ' . $regex_hash{$keyword} . ';' );
                    print STDERR $buf2 . "\n'\n";
                }
            }
            print $client $buf2;
            $ssh_subsocket->flush();
            if ( defined($verbose) ) { print STDOUT $buf2; }
        }
        ## XML payload
        while ( my $nread = sysread( $ssh_subsocket, $buf, 400 ) ) {
            my $filtered_out = $buf;
            foreach my $keyword ( keys %regex_hash2 ) {
                my $markin = $filtered_out;

#FIXME sysread buffer size might split $markin or $markout and generate an infinite loop...
                if ( $filtered_out =~ $compiled_regex_hash2{$keyword} ) {
                    print STDERR "found pattern " . $keyword . "\n";
                    eval( '$markin =~ ' . $regex_hash2{$keyword} . ';' );
                    $markin =~ s@.*<@<@g;
                    $markin =~ s@ .*>@@g;
                    $markin =~ s/^\s+|\s+$//g;
                    my $markout  = $markin;
                    my $marksubs = $markin;
                    $marksubs =~ s@<@</@g;
                    $marksubs .= ">";
                    $markout =~ s@<@<\\/@g;
                    $markout .= ">";
                    $markin  .= " ";
                    my $markout_regex = qr//;
                    eval( '$markout_regex= qr/\Q' . $markout . '\E/;' );

                    if ( $filtered_out =~ /$markout_regex/ ) {
                        print STDERR "found pattern out " . $markout . "\n";
                        eval(   '$filtered_out =~ s@\Q'
                              . $markin
                              . '\E.*\Q'
                              . $marksubs
                              . '\E@@g;' );
                        next;
                    }
                    print STDERR
                      "not found a pattern out - increasing bufffer size\n";
                    eval( '$filtered_out =~ s@\Q' . $markin . '\E.*@@g;' );
                    my $buf2;
                    while ( my $nread2 = sysread( $ssh_subsocket, $buf2, 400 ) )
                    {
                        $buf2 = $_;

          # send nothing until markout is found to remove proprietary extensions
                        if ( $buf2 =~ /$markout_regex/ ) {
                            print STDERR "found pattern out " . $markout . "\n";
                            eval( '$buf2 =~ s@.*\Q' . $marksubs . '\E@@g;' );
                            $filtered_out .= $buf2;
                            last;
                        }
                    }
                }
            }
            print $client $filtered_out;
            $ssh_subsocket->flush();
            if ( defined($verbose) ) { print STDOUT $filtered_out; }
        }
        kill( "TERM", $kidpid );    # send SIGTERM to child
    }

# the else{} block runs only in the child process (client input relayed to the server)
    else {

        $ssh_subsocket->autoflush(1);
        sleep 1;    # wait needed for ensuring STDOUT buffer is not melt
        my $buf;

        #while (defined (my $buf = <$client>)) {
        #usual syntax above used in verbose mode results into flush problems

        while ( my $nread0 = sysread( $client, $buf, 400 ) ) {
            foreach my $keyword ( keys %regex_hash ) {
                if ( $buf =~ $compiled_regex_hash{$keyword} ) {
                    print STDERR 'found regex '
                      . $keyword
                      . ": replacing '\n"
                      . $buf
                      . "\n' by '\n";
                    eval( '$buf =~ ' . $regex_hash{$keyword} . ';' );
                    print STDERR $buf . "\n'\n";
                }
            }
            print $ssh_subsocket $buf;
            if ( defined($verbose) ) { print STDOUT $buf; }
            if ( $buf =~ /]]>]]>/ )  { last; }
        }
        while ( my $nread = sysread( $client, $buf, 400 ) ) {
            print $ssh_subsocket $buf;
            $client->flush();
            if ( defined($verbose) ) { print STDOUT $buf; }
        }
        continue { }
        close $client;
    }

    $| = 0;

    sleep 2;
    kill( "TERM", $kidpid );    # send SIGTERM to child

}

exit;
