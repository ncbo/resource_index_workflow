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
	# set of (key, value) pairs := searchDrug (object type, PharmGKB accession id)

	my $argcount = scalar (@ARGV);

	if ($argcount != 1) {
		print "usage: drugs.pl <PharmGKB accession id>\n";
		exit -1;
	}

	# make a web services call to server
	my $gp = SOAP::Lite
		-> readable (1)
		-> uri('PharmGKBItem')
		-> proxy('http://www.pharmgkb.org/services/PharmGKBItem')
		-> searchDrug ($ARGV[0])
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
		if ($key eq "drugGenericNames") {
			my $val = $out{$key};
			print "drugGenericNames:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "drugTradeNames") {
			my $val = $out{$key};
			print "drugTradeNames:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "drugCategory") {
			my $val = $out{$key};
			$val =~ s/<.*>//g;
			print "drugCategory: $val\n";
		} elsif ($key eq "drugVaClassifications") {
			my $val = $out{$key};
			print "drugVaClassifications:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "drugRelatedPathways") {
			my $val = $out{$key};
			my @pathways = @$val;
			print "drugRelatedPathways:\n";
			for (my $i = 0; $i < scalar (@pathways); $i += 2) {
				print "\t" . $pathways[$i] . ": " . $pathways[$i + 1] . "\n";
			}
		} elsif ($key eq "drugRelatedGenes") {
			my $val = $out{$key};
			print "drugRelatedGenes:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "drugRelatedDiseases") {
			my $val = $out{$key};
			print "drugRelatedDiseases:\n";
			foreach my $item (@$val) {
				print "\t$item\n";
			}
		} elsif ($key eq "drugRelatedPhenotypeDatasets") {
			my $val = $out{$key};
			print "drugRelatedPhenotypeDatasets:\n";
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
