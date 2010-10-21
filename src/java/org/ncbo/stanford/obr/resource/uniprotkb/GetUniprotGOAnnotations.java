package org.ncbo.stanford.obr.resource.uniprotkb;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import obs.obr.populate.Element;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.util.helper.StringHelper;

/**
 * This class enables to get the list of all annotations between human proteins (gene symboles indeed) and GO concept.
 * Resource UniProtKB
 * This annotation are extracted from the file 
 * ftp://ftp.geneontology.org/pub/go/gene-associations/gene_association.goa_human.gz
 * IN: COMMAND to lunch a script
 * OUT: list of annotations <gene symbole, GO ID> in an HashSet
 * 
 * @author Adrien Coulet
 * @version OBR_v0.2		
 * @created 21-Nov-2008
 *
 */
public class GetUniprotGOAnnotations implements StringHelper{
	
	// Logger for this class
	private static Logger logger = Logger.getLogger(GetUniprotGOAnnotations.class);

	//attributes 	
	private static String SHELL_SCRIPT_PATH = new File(GetUniprotGOAnnotations.class.getResource( "getGoUniprotKbAnnot.sh" ).getFile()).getAbsolutePath();
	
	private static String COMMAND = "sh "+ SHELL_SCRIPT_PATH;	
	HashSet<Element>  ProteinAnnotList            = new HashSet<Element>() ;;
	Hashtable<String, Hashtable<String, String>> allProtAnnot     = new Hashtable<String, Hashtable<String, String>>();	//<protAccession, Hashtable of attribut-values couple>
	Hashtable<String, String> protAnnotAttribute  = new Hashtable<String, String>();   	//<attributName, value> (a value could be a map)
	Hashtable<String, String> protAnnotAttribute2 = new Hashtable<String, String>();   	//<attributName, value> (a value could be a map)	
	Resource resource;
	Structure basicStructure = null;
	String resourceID = EMPTY_STRING;
	 
	//constructor
	public GetUniprotGOAnnotations(Resource myResource){
		this.resource = myResource;
		this.basicStructure = myResource.getResourceStructure();
		this.resourceID     = myResource.getResourceId();		 
	}
	
	// method
	public HashSet<Element> getElements(Map<String, String> localOntologyMap) {			
		
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		try {					
			// download, gunzip the resource file
			process = runtime.exec(COMMAND);
			//InputStream results = process.getInputStream();

			// parse the file
			BufferedReader resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			String resultLine = EMPTY_STRING ; 
			try {
				allProtAnnot = new Hashtable<String, Hashtable<String, String>>();

				Pattern annotPattern    = Pattern.compile("^(.*)\\t(.*)\\t(.*)\\t(.*)\\t(.*)$");		

				while((resultLine = resultReader.readLine()) != null) {
					if(resultLine.trim().equals(EMPTY_STRING)){
						continue;
					}
					String localElementID = EMPTY_STRING;
					String geneSymbol     = EMPTY_STRING;
					String proteinName    = EMPTY_STRING;
					//String goBranch       = EMPTY_STRING; // P (biological process), F (moelcular function), or C (cellular component)
					String goConceptID    = EMPTY_STRING; 
					// process the line
					Matcher annotMatcher = annotPattern.matcher(resultLine);					
					if (annotMatcher.matches()){
						protAnnotAttribute = new Hashtable<String, String>();	
						if (!annotMatcher.group(1).equals(EMPTY_STRING)){
							
							localElementID = annotMatcher.group(1).trim();	
							//IF the protein is already annotated: UPDATE
							if(allProtAnnot.containsKey(localElementID)){
								if (!annotMatcher.group(4).equals(EMPTY_STRING)&&!annotMatcher.group(5).equals(EMPTY_STRING)){	
									//goBranch            = annotMatcher.group(4);
									goConceptID         = annotMatcher.group(5);	// ex P::GO:12345
									protAnnotAttribute  = allProtAnnot.get(localElementID);
									// sometimes a protein is annotated two times with the same GO concept if there is two evidence for this annotation
									if (!protAnnotAttribute.get("goAnnotationList").contains(goConceptID)){
										String concatGoList = protAnnotAttribute.get("goAnnotationList")+">"+goConceptID;
										protAnnotAttribute.put("goAnnotationList", concatGoList);
									}
								}
							}
							// ELSE the protein does not exist: CREATION
							else{		
								if (!annotMatcher.group(2).equals(EMPTY_STRING)&&!annotMatcher.group(3).equals(EMPTY_STRING)){
									geneSymbol    = annotMatcher.group(2);
									proteinName   = annotMatcher.group(3);
									protAnnotAttribute.put("geneSymbol",  geneSymbol);
									protAnnotAttribute.put("proteinName", proteinName);
								}
								if (!annotMatcher.group(4).equals(EMPTY_STRING)&&!annotMatcher.group(5).equals(EMPTY_STRING)){
									//goBranch      = annotMatcher.group(4);
									goConceptID   = annotMatcher.group(5);
									protAnnotAttribute.put("goAnnotationList", goConceptID);									
								}
							}																					
						}
						allProtAnnot.put(localElementID, protAnnotAttribute);							
					}	
				}// end of parsing								

				// Second phase: creation of elements
				// The creation of element is not done during the parsing in order to get all annotation of a protein before to create the element 
				for (String localElementID : allProtAnnot.keySet()){
					protAnnotAttribute2 = new Hashtable<String, String>();
					protAnnotAttribute2 = allProtAnnot.get(localElementID);
									
					// PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
					Structure elementStructure = new Structure(this.basicStructure.getContextNames());	
					for (String contextName: this.basicStructure.getContextNames()){
						boolean attributeHasValue = false;
						
						for (String att : protAnnotAttribute2.keySet()){
							if (contextName.equals(this.resourceID+UNDERSCORE_STRING+att)){
								// not an existing annotation
								if(basicStructure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION) ||
										basicStructure.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)){				
									elementStructure.putContext(contextName, protAnnotAttribute2.get(att));	
									attributeHasValue = true;
									
								}else{ // existing annotations	
									String localConceptID_leftPart  = localOntologyMap.get(contextName); //ontoID
									String localConceptID           = EMPTY_STRING;
									String localExistingAnnotations = protAnnotAttribute2.get(att);
									String localConceptIDs          = EMPTY_STRING;
									String[] splittedAnnotations    = localExistingAnnotations.split(GT_SEPARATOR_STRING);
									// translate conceptIDs used in the resource in OBR localConceptID
									for (int i=0;i<splittedAnnotations.length;i++){			
										try{
											String localConceptID_rightPart = splittedAnnotations[i].trim();		
											localConceptID = localConceptID_leftPart + SLASH_STRING+ localConceptID_rightPart;//localConceptID_leftPart+"/"+localConceptID_rightPart;
											  
										}catch (Exception e) {
											logger.error("** PROBLEM ** Problem with the management of the conceptID used in the resource to reported in the OBR", e);
										}
										if(localConceptID!=EMPTY_STRING){
											if(localConceptIDs!=EMPTY_STRING){
												localConceptIDs+=GT_SEPARATOR_STRING+localConceptID;
											}else{
												localConceptIDs+=localConceptID;
											}
										}
									}													
									elementStructure.putContext(contextName, localConceptIDs);	
									attributeHasValue = true;									
								}// end of existing annotation
							}
						}			
						
						// to avoid null value in the structure
						if (!attributeHasValue){
							elementStructure.putContext(contextName,EMPTY_STRING);
						}										
					}
					// put the element structure in a new element
					try{											
						Element  myProtAnnot = new Element(localElementID, elementStructure);		
						ProteinAnnotList.add(myProtAnnot);
						//System.out.println("element "+localElementID+"...");
						
					}catch(BadElementStructureException e){
						logger.error(EMPTY_STRING, e);
					}					 					
					//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				}		
			} finally {
				resultReader.close();
			}
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			try {

			} finally {
				errorReader.close();
			}
		} catch(IOException ioe) {
			logger.error(EMPTY_STRING, ioe);
		}		
		return ProteinAnnotList;
	}
}
