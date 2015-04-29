package obs.ontologyAccess.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import obs.ontologyAccess.AbstractOntologyAccessTool;
import obs.ontologyAccess.BioPortalAccessTool;
import obs.ontologyAccess.UmlsAccessTool;
import obs.ontologyAccess.bean.BioPortalFullConceptBean;
import obs.ontologyAccess.bean.BioPortalLightConceptBean;

public class AccessToolTests {

	public static void testGetOntologyInfo(AbstractOntologyAccessTool tool) throws Exception {
		System.out.println("************ ONTOLOGY INFORMATION FOR TOOL "+ tool.getToolName() +" *********************"); 
		System.out.println("** Tool "+ tool.getToolName() +" access "+ tool.getNumberOfOntologies() +" ontotlogies:");
		for(String ontology: tool.getToolOntologies()){
			System.out.println("\tID: " + ontology);
			System.out.println("\tName: " + tool.getOntologyName(ontology));
			System.out.println("\tVersion: " + tool.getOntologyVersion(ontology));
			System.out.println("\tDescription: " + tool.getOntologyDescription(ontology));
			System.out.println("");
		}
	}
	
	public static void testGetRootConcepts(AbstractOntologyAccessTool tool) throws Exception {
		System.out.println("*********** GET ROOT CONCEPT IDs FOR TOOL "+ tool.getToolName() +" *********************");
		HashSet<String> rootConcepts = new HashSet<String>();
		for(String ontology: tool.getToolOntologies()){
			System.out.println("onto: "+ontology);
			rootConcepts = tool.getOntologyRootConcepts(ontology);
			System.out.println("\t ********* in ontology ("+ rootConcepts.size() +"): " + ontology +" " + rootConcepts);
			for(String concept: rootConcepts){
				System.out.println(tool.conceptInformation(concept, false, false));
			}
		}
	}
	
	public static void testGetRootConceptsAndChildren(BioPortalAccessTool tool) throws Exception {
		System.out.println("*********** GET ROOT CONCEPT IDs AND CHILDREN FOR TOOL "+ tool.getToolName() +" *********************");
		ArrayList<BioPortalLightConceptBean> rootConcepts = new ArrayList<BioPortalLightConceptBean>();
		BioPortalFullConceptBean fullConcept;
		for(String ontology: tool.getToolOntologies()){
			rootConcepts = tool.getRootConceptBeans(ontology).getConcepts();
			System.out.println("\t ********* in ontology ("+ rootConcepts.size() +"): " + ontology +" " + rootConcepts);
			for(BioPortalLightConceptBean concept: rootConcepts){
				fullConcept = tool.getBioPortalConceptBean(BioPortalAccessTool.createLocalConceptID(ontology, concept.getId()));
				System.out.println(fullConcept.toString());
				for(BioPortalLightConceptBean directChild: fullConcept.getSubClass()){
					System.out.println(tool.getBioPortalConceptBean(BioPortalAccessTool.createLocalConceptID(ontology, directChild.getId())));
				}
			}
		}
	}
	
	public static void testOntologyMappings(AbstractOntologyAccessTool tool) throws Exception {
		System.out.println("*********** ONTOLOGY MAPPINGS FUNCTIONS FOR TOOL "+ tool.getToolName() +" *********************");
		for(String ontology: tool.getToolOntologies()){
			System.out.println("\tID: " + ontology + " Name: " + tool.getOntologyName(ontology));
			HashSet<String[]> mappings = tool.getAllOntologyMappings(ontology);
				for(String[] triplet: mappings){
					System.out.println(triplet[0]+ " maps to " + triplet[1] +"("+ triplet[2] +")");
				}
				System.out.println("");
		}
	}
	
	public static void testConceptMappingsWithSamples(AbstractOntologyAccessTool tool, Hashtable<String, ArrayList<String>> table) throws Exception {
		System.out.println("*********** CONCEPT MAPPINGS FUNCTIONS FOR TOOL "+ tool.getToolName() +" *********************");
		for(Map.Entry<String, ArrayList<String>> sample: table.entrySet()){
			System.out.println("Sample for ontology: " + sample.getKey());
			for(String localConceptID: sample.getValue()){
				System.out.println("Mappings for: " + localConceptID);
				HashSet<String[]> mappings = tool.getAllConceptMappings(localConceptID);
				for(String[] couple: mappings){
					System.out.print(couple[0]+ "(" + couple[1] +") - ");
				}
				System.out.println("");
			}
		}
	}
	
	public static void testGetLocalConceptIDs(int sampleSize, AbstractOntologyAccessTool tool, Hashtable<String, ArrayList<String>> table) throws Exception {
		System.out.println("*********** GET ALL CONCEPT IDs FOR TOOL "+ tool.getToolName() +" *********************");
		System.out.println("Number of concept IDs : ");
		int total = 0;
		for(String ontology: tool.getToolOntologies()){
			ArrayList<String> allLocalConceptIDs = new ArrayList<String>();
			allLocalConceptIDs.addAll(tool.getLocalConceptIDs(ontology)); 
			Collections.shuffle(allLocalConceptIDs);
			ArrayList<String> sample = new ArrayList<String>(sampleSize);
			int nb = 0;
			for (String localConceptID: allLocalConceptIDs){
				if(nb<sampleSize){
					sample.add(localConceptID);
					nb++;
				}
				else{
					break;
				}
			}
			table.put(ontology, sample);
			int count = allLocalConceptIDs.size();
			System.out.println("\tin ontology: " + ontology +" "+ count);
			total += count;
		}
		System.out.println("Total: " + total);
		//System.out.println("Or all in one shot:" + tool.getAllLocalConceptIDs().size());
	}
	
	public static void testConceptsWithSamples(AbstractOntologyAccessTool tool, Hashtable<String, ArrayList<String>> table) throws Exception{
		System.out.println("*********** CONCEPT FUNCTIONS FOR TOOL "+ tool.getToolName() +" *********************");
		for(Map.Entry<String, ArrayList<String>> sample: table.entrySet()){
			System.out.println("Sample for ontology: " + sample.getKey());
			for(String localConceptID: sample.getValue()){
				System.out.println(tool.conceptInformation(localConceptID, false, false));
			}
		}
	}
	
	public static void testSemanticTypes(UmlsAccessTool tool) throws Exception {
		System.out.println("*********** SEMANTIC TYPES FOR TOOL "+ tool.getToolName() +" *********************");
		for (String st: tool.getToolSemanticTypes()){
			System.out.println("\tSemantic type: " + st + ", " + tool.getSemanticTypeName(st));	
		}
	}
	
	public static void testMapStringToLocalConceptIDs(AbstractOntologyAccessTool tool) throws Exception {
		System.out.println("*********** MAP STRINGS FUNCTIONS FOR TOOL "+ tool.getToolName() +" *********************");
		ArrayList<String> ss = new ArrayList<String>();
		ss.add("melanoma"); 
		ss.add("Dipalmitoylphosphatidylcholine");
		ss.add("zeo");
		ss.add("Chromium^51^ albumin");
		ss.add("lesion traumatica de la via biliar");
		ss.add("Supernumerary heart valve cusps NEC (disorder)");
		ss.add("Dementia Due to HIV Disease");
		ss.add("PILONIDAL CYST");
		ss.add("Plasbumin-20% Solution for Injection");
		ss.add("ACETOACETATE");
		System.out.println("Concept that map for the strings:");
		for(String ontology: tool.getToolOntologies()){
			int total = 0;
			for(String s: ss){
				HashSet<String> localConceptIDs = tool.mapStringToLocalConceptIDs(s, ontology, true);
				//HashSet<String> localConceptIDs = tool.mapStringToLocalConceptIDsUMLS(s, ontology, false);
				System.out.println("\tin ontology ("+ localConceptIDs.size() +"): " + ontology + localConceptIDs);
				total += localConceptIDs.size();
			}
			System.out.println("\tTotal: " + total);
		}
		System.out.println("Concept that map for the strings in all ontology accessed by the tool:");
		for(String s: ss){
			System.out.println(tool.mapStringToLocalConceptIDs(s, true));
			//System.out.println(tool.mapStringToLocalConceptIDsUMLS(s, false));
		}
	}
	
}
