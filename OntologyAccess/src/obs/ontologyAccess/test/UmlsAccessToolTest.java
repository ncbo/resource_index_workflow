package obs.ontologyAccess.test;

import java.util.ArrayList;
import java.util.Hashtable;

import obs.ontologyAccess.UmlsAccessTool;


public class UmlsAccessToolTest{

	private static String[] umlsConnectionInfo = {"jdbc:mysql://ncbo-db2.stanford.edu:3306/umls2009aa","obs","obs"};
	//private static int sampleSize = 100;
	private static String[] umlsSABs = {"CST"}; //""MSH", "NCI", "SNOMEDCT", "RXNORM", "MMX", "NDFRT"}; 
	private static String[] umlsTUIs = {}; //"T047", "T048", "T191", "T037", "T184", "T019", "T020", "T190", "T050", "T200"};
	
	public static void main(String[] args) throws Exception {

		// creation of 3 tools 
		UmlsAccessTool customTool = new UmlsAccessTool(umlsSABs, "OBR", umlsConnectionInfo, umlsTUIs); 
		UmlsAccessTool emptyTool = new UmlsAccessTool(umlsConnectionInfo, "");
		//UmlsAccessTool allTool = new UmlsAccessTool(umlsConnectionInfo, "");
		//allTool.addAllUMLSOntologies();
		
		// samples of localConceptID for each tool
		Hashtable<String, ArrayList<String>> localConceptIDsSample = new Hashtable<String, ArrayList<String>>();
		
		AccessToolTests.testGetOntologyInfo(emptyTool);
		//AccessToolTests.testGetOntologyInfo(customTool);
		//AccessToolTests.testGetOntologyInfo(allTool);
		
		//AccessToolTests.testMapStringToLocalConceptIDs(emptyTool);
		//AccessToolTests.testMapStringToLocalConceptIDs(customTool);
		//AccessToolTests.testMapStringToLocalConceptIDs(allTool);
		
		// fill the samples
		//AccessToolTests.testGetLocalConceptIDs(sampleSize, emptyTool, localConceptIDsSample);
		//AccessToolTests.testGetLocalConceptIDs(sampleSize, customTool, localConceptIDsSample);
		//AccessToolTests.testGetLocalConceptIDs(sampleSize, allTool, localConceptIDsSample);

		//AccessToolTests.testGetRootConcepts(emptyTool);
		//AccessToolTests.testGetRootConcepts(customTool);
		//AccessToolTests.testGetRootConcepts(allTool);

		AccessToolTests.testSemanticTypes(emptyTool);
		//AccessToolTests.testSemanticTypes(customTool);
		//AccessToolTests.testSemanticTypes(allTool);

		// uses the previously filled samples
		AccessToolTests.testConceptsWithSamples(emptyTool, localConceptIDsSample);
		//AccessToolTests.testConceptsWithSamples(customTool, localConceptIDsSample);
		//AccessToolTests.testConceptsWithSamples(allTool, localConceptIDsSample);

		//AccessToolTests.testConceptMappingsWithSamples(customTool, localConceptIDsSample);
		AccessToolTests.testOntologyMappings(emptyTool);
		//AccessToolTests.testOntologyMappings(customTool);
		//AccessToolTests.testOntologyMappings(allTool);
		
		//System.out.println(emptyTool.conceptInformation("SNOMEDCT/C0017654", true, true));
		System.out.println(emptyTool.conceptInformation("CST/C0014130", true, false));
		System.out.println(emptyTool.conceptInformation("CST/C1140097", true, false));
		System.out.println(emptyTool.conceptInformation("CST/C0549611", true, false));
		System.out.println(emptyTool.conceptInformation("CST/C0549525", true, false));
		//System.out.println(emptyTool.conceptInformation("AIR/C0150933", true, true));
		//System.out.println(emptyTool.conceptInformation("NCI/C1708566", true, true));
			
	}
}
