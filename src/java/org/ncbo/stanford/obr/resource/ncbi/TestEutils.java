package org.ncbo.stanford.obr.resource.ncbi;

import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceLocator;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceSoap;

public class TestEutils {

public static void main(String args[]) throws Exception{
	System.out.println("-------------------------End1--------------------");
	EUtilsServiceLocator toolService = new  EUtilsServiceLocator();
	System.out.println("-------------------------End2--------------------");
	EUtilsServiceSoap toolEutils =  toolService.geteUtilsServiceSoap();
	System.out.println("-------------------------End--------------------");
	}
}
