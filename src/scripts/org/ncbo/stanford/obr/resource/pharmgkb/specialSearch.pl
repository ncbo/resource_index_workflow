#!/usr/bin/perl -w

use SOAP::Lite;
use English;
use Carp;
import SOAP::Data 'type';
use strict vars;


# author: Andrew MacBride (andrew@helix.stanford.edu)

sub main {
	# method signature is:
	# array of (PA#, type, name or title, isPhenotype, isGenotype, isRelationship, PharmGKB URL) = search (<text> <isAny> <isPhenotype> <isGenotype> <isRelationship>)
	# additional fields for genes: (symbol, array of alternate symbols, array of alternate names)
	# additional fields for publications: (PubMed ID, PubMed URL)
	# additional fields for submissions: (submission type)

	my $argcount = scalar (@ARGV);

	if ($argcount != 1) {
		print "usage: specialSearch <integer search type>\n";
		print "\t where search type is one of:\n";
		exit -1;
	}

	# make a web services call to server
	my $gp = SOAP::Lite
		-> readable (1)
		-> uri('SearchService')
		-> proxy('http://www.pharmgkb.org/services/SearchService')
		-> specialSearch (
			type('xsd:int' => $ARGV[0]))
		-> result;


	foreach my $f (@{$gp}) {
		#print scalar ($f) . "\n";
		my @list = @{$f};
		my $len = scalar (@list);
		for (my $i = 0; $i < $len; $i++)  {
			if ($i == ($len - 1)) {	
				print  $list[$i];
			} else {
				print  "$list[$i], ";
			}
		}
		print "\n";
	}
}

main();

exit 0;
