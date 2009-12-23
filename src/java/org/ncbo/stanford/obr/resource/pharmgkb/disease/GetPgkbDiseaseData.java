
package org.ncbo.stanford.obr.resource.pharmgkb.disease;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import obs.common.utils.StreamGobbler;
import obs.obr.populate.Element;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.resource.pharmgkb.drug.GetPgkbDrugData;
import org.ncbo.stanford.obr.resource.pharmgkb.gene.GetPgkbGeneData;


/**
 * This class enables to get pharmgkb data related to a disease
 * by lunching the web service client diseases.pl
 * IN: disease accession id (ex: PA447230)
 * OUT: related data enclosed in a diseaseElement
 * 
 * @author Adrien Coulet
 * @version OBR_v0.2		
 * @created 11-Nov-2008
 *
 */

public class GetPgkbDiseaseData {
	
	// Logger for this class
	private static Logger logger = Logger.getLogger(GetPgkbDiseaseData.class);
	//attributes
	private static String PERL_SCRIPT_PATH =new File(ClassLoader.getSystemResource("org/ncbo/stanford/obr/resource/pharmgkb/disease/diseases.pl" ).getFile()).getAbsolutePath();
	private static String COMMAND                 = "perl " +PERL_SCRIPT_PATH; 
	Hashtable<String, Hashtable<String, Hashtable<Integer, String>>> diseaseData      = new Hashtable<String, Hashtable<String, Hashtable<Integer, String>>>();	//<diseaseAccession, Hashtable of attribut-value couple>
	Hashtable<String, Hashtable<Integer, String>> diseaseAttribute = new Hashtable<String, Hashtable<Integer, String>>();	//<attributName, value> (a value could be a map)
	Hashtable<Integer, String>   attributeValues  = new Hashtable<Integer, String>();
	Structure basicStructure = null;
	String resourceID = "";
	
	//constructor
	public GetPgkbDiseaseData(Resource myResource){	
		this.basicStructure = myResource.getResourceStructure();	
		this.resourceID     = myResource.getResourceID();
	}
	
	public GetPgkbDiseaseData(){		 
	}
	
	// method
	public Element getDiseaseElement(String diseaseAccession) {

		Structure elementStructure = basicStructure;
		Element myDisease = null;
		
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		try {
			
			logger.info("get data for "+diseaseAccession+"... ");
			
			if (diseaseAccession!=null){				
				process = runtime.exec(COMMAND+" "+diseaseAccession);
			}

			// error message and script output management
	        StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), "ERROR");            
	        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");	            
            
	        errorGobbler.start();
	        outputGobbler.start();
	        
            int exitValue = process.waitFor();
            logger.info("ExitValue: " + exitValue);        

	        HashMap<Integer, String> lines = StreamGobbler.lines;         	        
			
			try {
				diseaseData = new Hashtable<String, Hashtable<String, Hashtable<Integer, String>>>();				
				Integer attributeNumber = 0;
				Pattern setPattern  = Pattern.compile("^\\t(.*)$");
				Pattern dataPattern = Pattern.compile("^(.+):(.*)$");
				String attributeName = null;
				
				if(!lines.keySet().isEmpty()){
					for(int i=0; i<lines.keySet().size();i++) {
						String resultLine=lines.get(i);
						// process the line	
						Matcher setMatcher = setPattern.matcher(resultLine);		
						// line with an attribute name =====================
						if (!setMatcher.matches()){ 
							//new attribute	
							Matcher dataMatcher = dataPattern.matcher(resultLine);
							if(dataMatcher.matches()){
								//first we put in the hashtable last things							
								if (attributeName!=null && attributeValues!=null){
									diseaseAttribute.put(attributeName, attributeValues);
								}							
								// then initialization
								attributeName   = dataMatcher.group(1);
								attributeValues = new Hashtable<Integer, String>();
								
								if(!dataMatcher.group(2).equals("")){ // simple case in which we have atributeName: value on one line
									String value = null;
									value = dataMatcher.group(2).replaceFirst(" ", "");
									attributeValues.put(1, value);	
								}else{
									attributeNumber = 0;
								}							
							}
						// non header line => value ========================		
						}else{											
							if (attributeName!=null){
								attributeNumber++;
								String value = null;
								value = setMatcher.group(1);
								attributeValues.put(attributeNumber, value);
							}
						}
					}
				}
				if(attributeName!=null && attributeValues!=null){
					diseaseAttribute.put(attributeName, attributeValues); //update of diseaseAttribute content				
					//update the diseaseData
					diseaseData.put(diseaseAccession, diseaseAttribute);//update of diseaseData content
				}else{
					logger.info("PROBLEM when getting data with the web service");
				}
				// PUT DATA INTO AN ELEMENT++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
				// for each attribute
				GetPgkbGeneData myGeneExtractor = new GetPgkbGeneData();
				GetPgkbDrugData  myDrugExtractor = new GetPgkbDrugData();
				String attInString = "";
				
				for (String contextName: elementStructure.getContextNames()){
					boolean attributeHasValue = false;
					for (String att : diseaseAttribute.keySet()){
						if (contextName.equals(this.resourceID+"_"+att)){
							attributeHasValue = true;
							// transform repetitive element (hashtables) in a string with > as a separator.
							attInString = "";
							Hashtable<Integer, String> valueTable = diseaseAttribute.get(att);
							for (Integer valueNb :valueTable.keySet()){
								if (!attInString.equals("")){
									// specific case of gene => we want to store gene symbol and not the PharmGKB localElementID
									if(att.equals("diseaseRelatedGenes")){
										attInString = attInString+"> "+myGeneExtractor.getGeneSymbolByGenePgkbLocalID(valueTable.get(valueNb));
									}else if(att.equals("diseaseRelatedDrugs")){
										attInString = attInString+"> "+myDrugExtractor.getDrugNameByDrugLocalID(valueTable.get(valueNb));
									}else{
										attInString = attInString+"> "+valueTable.get(valueNb);
									}
								}else{
									if(att.equals("diseaseRelatedGenes")){
										attInString=myGeneExtractor.getGeneSymbolByGenePgkbLocalID(valueTable.get(valueNb));
									}else if(att.equals("diseaseRelatedDrugs")){
										attInString = myDrugExtractor.getDrugNameByDrugLocalID(valueTable.get(valueNb));
									}else{
										attInString=valueTable.get(valueNb);
									}									
								}				
							}
							elementStructure.putContext(contextName,attInString);
						}
					}
					// to avoid null value in the structure
					if (!attributeHasValue){
						elementStructure.putContext(contextName,"");
					}				
				}
				//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				
			} finally {
			}
        }catch(Throwable t){
        	logger.error("Problem in processing element", t);
        }

		// creation of the element
		try{
			myDisease = new Element(diseaseAccession, elementStructure);
		}catch(BadElementStructureException e){
			logger.error("", e);
		}
		return myDisease;
	}
	
	public String getDiseaseNameByDiseaseLocalID(String diseaseLocalID) {
		String diseaseName = "";
		
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		try {
			if (diseaseLocalID!=null){				
				process = runtime.exec(COMMAND+" "+diseaseLocalID);
			}

			// error message and script output management
	        StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), "ERROR");            
	        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");	            
            
	        errorGobbler.start();
	        outputGobbler.start();
	        
            int exitValue = process.waitFor();
            System.out.println("ExitValue: " + exitValue);        

	        HashMap<Integer, String> lines = StreamGobbler.lines;         	        
			
			try {
				diseaseName = ""; 				
				Pattern dataPattern  = Pattern.compile("^diseaseName: (.*)$");
				
				if(!lines.keySet().isEmpty()){
					for(int i=0; i<lines.keySet().size();i++) {
						String resultLine=lines.get(i);
						// process the line	
						Matcher dataMatcher = dataPattern.matcher(resultLine);		
						// line with the geneSymbol ===========================
						if (dataMatcher.matches()){ 
								diseaseName = dataMatcher.group(1);	
								//System.out.println(genePgkbLocalID+" => "+geneSymbol);
						}
					}
				}
			}finally {
			}
        }catch(Throwable t){
        	logger.error("Problem in getting disease name for diseaseLocalID : " + diseaseLocalID, t);            
        }
		return diseaseName;
	}
}
