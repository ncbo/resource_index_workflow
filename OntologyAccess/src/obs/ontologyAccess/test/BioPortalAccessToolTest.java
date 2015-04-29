package obs.ontologyAccess.test;

import obs.common.utils.ExecutionTimer;
import obs.ontologyAccess.BioPortalAccessTool;

public class BioPortalAccessToolTest {
	
	//private static int sampleSize = 100;
	private static String[] bpOntologies = {}; 

	public static void main(String[] args) throws Exception {
		
		ExecutionTimer timer = new ExecutionTimer();
		// creation of 3 tools
		BioPortalAccessTool emptyTool = new BioPortalAccessTool("");
		BioPortalAccessTool customTool = new BioPortalAccessTool(bpOntologies, "OBR"); 
		BioPortalAccessTool allTool = new BioPortalAccessTool("");
		allTool.addAllBioPortalOntologies();
		
		timer.start();
		// *********************************** AD-HOC TESTS *************************************************
		
		System.out.println("\tName: " + emptyTool.getOntologyName("32145"));
		//System.out.println("\tName: " + emptyTool.getOntologyName("38677"));
		
		System.out.println("\tName: " + emptyTool.getConceptDirectChildren("38677/DOID:4"));
		//System.out.println("\tName: " + emptyTool.getConceptDirectChildren("38677/DOID:4413"));
		//System.out.println("\tName: " + emptyTool.getConceptDirectChildren("38677/DOID:4364"));
		
		//System.out.println(emptyTool.conceptInformation("38677/DOID:1909"));
		//System.out.println(emptyTool.conceptInformation("37575/FBbt:00005542"));
		//System.out.println(emptyTool.conceptInformation("37575/_Anon10"));
		
		//System.out.println(emptyTool.getOntologyRootConcepts("38896").toString());
		//System.out.println(emptyTool.getOntologyRootConcepts("38677").toString());
		//System.out.println(emptyTool.getOntologyRootConcepts("13578").toString());

		//System.out.println(customTool.conceptInformation("38802/NCBITaxon:248365"));
		System.out.println(customTool.conceptInformation("13578/Melanoma"));
		System.out.println(customTool.getConceptParents("13578/Melanoma"));
				
		//System.out.println(customTool.mapStringToLocalConceptIDs("melanoma", "38865", false));
		
		//System.out.println("\tAll local concept ids: " + emptyTool.getLocalConceptIDs("38677"));
		/*
		HashSet<String> allConcepts = emptyTool.getLocalConceptIDs("13578");
		System.out.println("\tAll local concept ids ("+allConcepts.size()+"): "+ allConcepts );
		*/
		
		// *********************************** GENERIC TESTS *************************************************
		
		// samples of localConceptID for each tool
		//Hashtable<String, ArrayList<String>> localConceptIDsSample = new Hashtable<String, ArrayList<String>>();
		
		//AccessToolTests.testGetOntologyInfo(emptyTool);
		//AccessToolTests.testGetOntologyInfo(customTool);
		AccessToolTests.testGetOntologyInfo(allTool);
		
		//AccessToolTests.testGetRootConcepts(emptyTool);
		//AccessToolTests.testGetRootConcepts(customTool);
		AccessToolTests.testGetRootConcepts(allTool);
		
		//AccessToolTests.testGetRootConceptsAndChildren(customTool);
		AccessToolTests.testGetRootConceptsAndChildren(allTool);

		// fill the samples
		//AccessToolTests.testGetLocalConceptIDs(sampleSize, customTool, localConceptIDsSample);
		
		// *********************************** CONCEPTS WITH PROBLEM *************************************************
				
		//System.out.println(emptyTool.conceptInformation("4525/PathologicalPhenomenon"));
		
		//System.out.println(emptyTool.conceptInformation("13352/EHDAA:4957"));
		//System.out.println(emptyTool.conceptInformation("28096/http://purl.org/nif/ontology/nullA6286"));
		//System.out.println(emptyTool.conceptInformation("13578/Melanoma"));
		//System.out.println(emptyTool.conceptInformation("28837/http%3A%2F%2Fwww.owl-ontologies.com%2FGeographicalRegion.owl%23Geographical_Regions"));
		//System.out.println(emptyTool.conceptInformation("39149/http://purl.org/obo/owl/FMA#FMA_9669"));
		/*
		System.out.println(emptyTool.conceptInformation("13352/EHDAA:4957"));
		System.out.println(emptyTool.conceptInformation("38745/GO:0004899"));
		System.out.println(emptyTool.conceptInformation("38739/npo:Entity"));
		System.out.println(emptyTool.conceptInformation("38587/YPO:0000001"));
		
		System.out.println(emptyTool.conceptInformation("38563/Bleeding From Tongue"));
		//System.out.println(emptyTool.conceptInformation("28837/http://www.owl-ontologies.com/GeographicalRegion.owl#Geographical_Regions"));
		System.out.println(emptyTool.conceptInformation("13209/http://www.loria.fr/~coulet/ontology/sopharm/version2.0/mammalian_phenotype.owl#MP_0000001"));
		 */
		
		
		timer.end();
		System.out.println(timer.display());
		
	}
	
}
