package obs.ontologyAccess.test;

import java.util.HashSet;

import obs.ontologyAccess.UmlsAccessTool;

public class PreferredNameTest {

	private static String[] umlsConnectionInfo = {"jdbc:mysql://ncbo-db2.stanford.edu:3306/umls","ontrez","ontrez"};
	private static String[] umlsSABs = {"MMX"}; 
	private static String[] umlsTUIs = {};
	
	public static void main(String[] args) throws Exception {

		UmlsAccessTool customUMLSTool = new UmlsAccessTool(umlsSABs, "OBR", umlsConnectionInfo, umlsTUIs);
		
		HashSet<String> localConceptIDs = customUMLSTool.getAllLocalConceptIDs(); 
		for (String localConceptID: localConceptIDs){
			System.out.println(localConceptID + "\t" + customUMLSTool.getConceptPreferredName(localConceptID));
		}

	} 
}
