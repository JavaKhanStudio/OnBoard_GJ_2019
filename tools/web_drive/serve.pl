#!/usr/bin/perl
# serve.pl — the drive's browser version on Mac and Linux (atelier r222). Serves the folder it sits in
# on http://127.0.0.1:47219/ and opens it in the browser. A browser refuses to run the game from a
# disk (file://), so double-clicking index.html can never work; a web server, even this one, can.
# Core Perl only: macOS ships Perl but no longer Python. Serve.ps1 is the same thing for Windows.
# The port is FIXED: the game keeps its options in localStorage, which belongs to the port.
use strict;
use warnings;
use IO::Socket::INET;
use File::Basename qw(dirname);
use Cwd qw(abs_path);

my $root = dirname(abs_path($0));
my $port = 47219;
my %type = (html => 'text/html; charset=utf-8', js => 'text/javascript', css => 'text/css',
	png => 'image/png', jpg => 'image/jpeg', gif => 'image/gif', ico => 'image/x-icon',
	mp3 => 'audio/mpeg', ttf => 'font/ttf', json => 'application/json', txt => 'text/plain; charset=utf-8');

my $server;
for my $p ($port .. $port + 9) {
	$server = IO::Socket::INET->new(LocalAddr => '127.0.0.1', LocalPort => $p, Listen => 32, ReuseAddr => 1, Proto => 'tcp');
	if ($server) { $port = $p; last; }
}
die "On Board: no free port from 47219 to 47228\n" unless $server;

my $url = "http://127.0.0.1:$port/";
print "On Board : $url\n\n";
print "Laissez cette fenetre ouverte pendant que vous jouez. Fermez-la pour arreter.\n";
print "Leave this window open while you play. Close it to stop.\n";
my $open = $^O eq 'darwin' ? 'open' : 'xdg-open';
system("$open '$url' >/dev/null 2>&1 &") unless $ENV{ONBOARD_NO_BROWSER};

$SIG{CHLD} = 'IGNORE';
while (my $c = $server->accept) {
	my $pid = fork;
	if (!defined $pid || $pid) { close $c; next; }
	close $server;
	serve($c);
	exit 0;
}

sub serve {
	my ($c) = @_;
	local $SIG{ALRM} = sub { exit 0 };    # a browser's speculative connection that never asks
	alarm 30;
	my $line = <$c> // return;
	while (my $h = <$c>) { last if $h =~ /^\r?\n$/ }
	alarm 0;
	my ($method, $path) = $line =~ m{^(GET|HEAD) (\S+)} or return reply($c, 405, 'text/plain', 'GET only');
	$path =~ s/[?#].*//;
	$path =~ s/%([0-9A-Fa-f]{2})/chr hex $1/ge;
	$path .= 'index.html' if $path =~ m{/$};
	return reply($c, 404, 'text/plain', 'not found') if $path =~ m{(^|/)\.\.(/|$)};
	my $file = "$root$path";
	open my $fh, '<:raw', $file or return reply($c, 404, 'text/plain', 'not found');
	local $/;
	my $body = <$fh>;
	my ($ext) = $file =~ /\.([^.\/]+)$/;
	reply($c, 200, $type{lc($ext // '')} // 'application/octet-stream', $method eq 'HEAD' ? '' : $body, length $body);
}

sub reply {
	my ($c, $code, $type, $body, $len) = @_;
	$len //= length $body;
	print $c "HTTP/1.1 $code " . ($code == 200 ? "OK" : "No") . "\r\nContent-Type: $type\r\nContent-Length: $len\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n", $body;
	close $c;
}
