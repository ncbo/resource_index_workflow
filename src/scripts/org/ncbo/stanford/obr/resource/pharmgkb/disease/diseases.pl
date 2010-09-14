#!/usr/bin/perl -w

use SOAP::Lite;
use English;
use Carp;
import SOAP::Data 'type';
use Data::Dumper;
use strict vars;


# author: Andrew MacBride (andrew@helix.stanford.edu)

sub main {
	# method signature is:
	# set of (key, value) pairs := searchDisease (object type, PharmGKB accession id)

	my $argcount = scalar (@ARGV);

	if ($argcount != 1) {
		print "usage: diseases.pl <PharmGKB accession id>\n";
		exit -1;
	}

	# make a web services call to server
	my $gp = SOAP::Lite
		-> readable (1)
		-> uri('PharmGKBItem')
		-> proxy('http://www.pharmgkb.org/services/PharmGKBItem')
		-> searchDisease ($ARGV[0])
		-> result;

	if (!defined ($gp)) {
		print "undefined result\n";
		exit;
	}

	my %out = %$gp;

	print "key list:\n";
	foreach my $key (keys %out) {
		print "\t$key\n";
	}

	# a big help with debugging: see what has actually been returned
	#print Dumper (%out);

	foreach my $key (keys %out) {
		if ($key eq "diseaseRelatedPathways") {
			my $val = $out{$key};
			my @pathways = @$val;
			print "diseaseRelatedPathways:\n";
			for (my $i = 0; $i < scalar (@pathways); $i += 2) {
				print "\t" . $pathways[$i] . ": " . $pathways[$i + 1] . "\n";
			}
		} elsif ($key eq "diseaseRelatedGenes") {
			my $val = $out{$key};
			print "diseaseRelatedGenes:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "diseaseRelatedDrugs") {
			my $val = $out{$key};
			print "diseaseRelatedDrugs:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "diseaseAlternateNames") {
			my $val = $out{$key};
			print "diseaseAlternateNames:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "diseaseRelatedPhenotypeDatasets") {
			my $val = $out{$key};
			print "diseaseRelatedPhenotypeDatasets:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} else {
			print "$key: $out{$key}\n";
		}
	}

	return;
}

main();

exit 0;
