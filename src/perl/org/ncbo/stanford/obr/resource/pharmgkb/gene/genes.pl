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
	# set of (key, value) pairs := searchGene (PharmGKB accession id)

	my $argcount = scalar (@ARGV);

	if ($argcount != 1) {
		print "usage: genes.pl <PharmGKB accession id>\n";
		exit -1;
	}

	# make a web services call to server
	my $gp = SOAP::Lite
		-> readable (1)
		-> uri('PharmGKBItem')
		-> proxy('http://www.pharmgkb.org/services/PharmGKBItem')
		-> searchGene ($ARGV[0])
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
		if ($key eq "geneRelatedPathways") {
			my $val = $out{$key};
			my @pathways = @$val;
			print "geneRelatedPathways:\n";
			for (my $i = 0; $i < scalar (@pathways); $i += 2) {
				print "\t" . $pathways[$i] . ": " . $pathways[$i + 1] . "\n";
			}
		} elsif ($key eq "geneRelatedDiseases") {
			my $val = $out{$key};
			print "geneRelatedDiseases:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "geneAlternateNames") {
			my $val = $out{$key};
			print "geneAlternateNames:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "geneAlternateSymbols") {
			my $val = $out{$key};
			print "geneAlternateSymbols:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "geneRelatedDrugs") {
			my $val = $out{$key};
			print "geneRelatedDrugs:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "geneRelatedPhenotypeDatasets") {
			my $val = $out{$key};
			print "geneRelatedPhenotypeDatasets:\n";
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
