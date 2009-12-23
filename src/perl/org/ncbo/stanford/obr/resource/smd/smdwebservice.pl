#!/usr/bin/perl -w

#
# This perl script is used to access Stanford Microarray Database 
# web service. It provides following functionality : 
#       1.Get map for organisms		 
#		2.Get details for experiment
#
# author: kyadav
# 
use strict;
use SOAP::Lite;

#
# Executing main method. 
#
&main();

#
# Main method for smd web service
#
sub main
{
	no warnings;
	my $argcount = scalar (@ARGV);
	if ($argcount lt 1) {
		&printUsage();
		exit -1;
	}
	# Call methods as per arguments.	
	if($ARGV[0]eq 'organisms')
	{
		&getOrganisms();
	}
	elsif($ARGV[0]eq 'detail')
	{
		my $index=1;
		while ( $index < $argcount ) {  
			print "Start\n";  
			&getExperimentDetails($ARGV[$index]);	
			print "End\n";		
		    $index = $index + 1;		    
		}
		
	}else{
		print "Wrong arguments. Please see usage.\n";
		&printUsage();
		exit -1; 
	}
}

#
#  This method prints usage text for this script.
#
sub printUsage
{
	print "Usage:\n"; 
	print "1. Organism list      :\tsmdwebservice.pl organisms\n";	 
	print "2. Experiment Detail  :\tsmdwebservice.pl detail <experiment id>\n";
	
}

# Getting Organism Map with id. 
sub getOrganisms{
	
	my $organismHash = SOAP::Lite
	-> uri('http://genome-www5.stanford.edu/listData')
	-> proxy('http://genome-www5.stanford.edu/cgi-bin/tools/webservices/listData.pl')
	-> Organism
	-> result;	 
	foreach my $organism (keys %{$organismHash}){
		print "$organism\t$$organismHash{$organism}\n";
    }
}

#
# This method extract details for given experiment identifier in hash table.
#
sub getExperimentDetails
{	 
	no warnings;
	my $metaDataMap = SOAP::Lite
	-> uri('http://genome-www5.stanford.edu/Experiment')
	-> proxy('http://genome-www5.stanford.edu/cgi-bin/tools/webservices/exptHandle.pl')
	-> ExptMetaData ($_[0], 'hash table')	 
	-> result;
	# Display experiment detail hash table in key-value format.
	foreach my $metaData (keys %{$metaDataMap}){
	 	print "$metaData\t$$metaDataMap{$metaData}\n";
	}
	
}