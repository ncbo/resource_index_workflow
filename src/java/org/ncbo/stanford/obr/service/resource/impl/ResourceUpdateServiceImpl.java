package org.ncbo.stanford.obr.service.resource.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import obs.obr.populate.Element;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException; 

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.exception.ResourceFileException;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.service.AbstractResourceService;
import org.ncbo.stanford.obr.service.resource.ResourceUpdateService;

public class ResourceUpdateServiceImpl extends AbstractResourceService implements ResourceUpdateService{
	 
	// Logger for ResourceUpdateServiceImpl 
	protected static Logger logger = Logger.getLogger(ResourceUpdateServiceImpl.class);
 

	/**
	 * 
	 * @param resourceAccessTool
	 */
	public ResourceUpdateServiceImpl(ResourceAccessTool resourceAccessTool) {
		super(resourceAccessTool);		
	} 
	
	/**
	 * Update the resource content according to a given tab delimited text file.
	 * The first column of the file must be the elementLocalID. 
	 * This file must contain the same number of itemKey columns than the associated resource structure.
	 * Returns the number of elements updated. Can be used for updateResourceContent.
	 */
	public int updateResourceContentFromFile(File resourceFile) throws BadElementStructureException, ResourceFileException{
		int nbElement = 0;
		logger.info("Updating resource content with local file " + resourceFile.getName() + "...");
		try{
			FileReader fstream = new FileReader(resourceFile);
			BufferedReader in = new BufferedReader(fstream);
			String line = in.readLine();
			String[] elementCompleteInfo = line.split("\t");
			ArrayList<String> contextNames = resourceAccessTool.getToolResource().getResourceStructure().getContextNames(); 
			if (elementCompleteInfo.length != contextNames.size()+1){
				throw new ResourceFileException("Number of columns too short in file " + resourceFile.getName());
			} 
			Element element;
			Structure eltStructure = new Structure(contextNames);
			while (line != null){
				elementCompleteInfo = line.split("\t");
				int i=0;
				for(String contextName: contextNames){
					eltStructure.putContext(contextName, elementCompleteInfo[i+1]);
					i++;
				}
				element = new Element(elementCompleteInfo[0], eltStructure);
				if (elementTableDao.addEntry(element)){
					nbElement ++;
				}
				line = in.readLine();
			}
			in.close();
			fstream.close();
		}
		catch (IOException e){
			logger.error("** PROBLEM ** Cannot update resource " + resourceAccessTool.getToolResource().getResourceName() + " with file " + resourceFile.getName(), e);
		}
		return nbElement;
	}
	
	/**
	 * Adds the Resource tool entry into Resource table (OBR_RT)
	 * 
	 */
	public void addResource(Resource resource){
		AbstractObrDao.resourceTableDao.addEntryOrUpdate(resource);
	}

	/**
	 * Update resource table with latest dictionayID
	 * 
	 */
	public void updateResourceForLatestDictionary(Resource resource) {
		AbstractObrDao.resourceTableDao.updateDictionaryID(resource);
	}

	public int numberOfEntry() {
		// TODO Auto-generated method stub
		return elementTableDao.numberOfEntry();
	} 

	public void reInitializeAllTables() {
		elementTableDao.reInitializeSQLTable();
		reInitializeAllTablesExcept_ET();
	}

	public void reInitializeAllTablesExcept_ET() {
		directAnnotationTableDao.reInitializeSQLTable();
		expandedAnnotationTableDao.reInitializeSQLTable();
		indexTableDao.reInitializeSQLTable();
		
	}
	
	/**
	 * Returns a set of all the localElementIDs contained in the table. 
	 */
	public HashSet<String> getAllLocalElementIDs(){
		return elementTableDao.getAllLocalElementIDs();
	}

	public boolean addElement(Element element){
		return elementTableDao.addEntry(element);
	}
	
	/**
	 * This method split terms string with splitString
	 * and get local concept id's using ontology access tool. 
	 * 
	 * @param terms
	 * @param localOntologyID
	 * @param splitString
	 * @return String containing local concept id's separated by '>' 
	 */
	public String mapTermsToLocalConceptIDs(String terms, String localOntologyID, String splitString){
		 
		HashSet<String> concepts= new HashSet<String>();
		
		//Split given string using splitString
		String[] termsArray;
		if(splitString!= null){
			termsArray = terms.split(splitString);
		}else{
			termsArray = new String[]{terms};
		}
		
		for (String term : termsArray) {
			try {
				concepts.addAll(commonObsDao.mapStringToLocalConceptIDs(term.trim(),localOntologyID));
			} catch (Exception e) {				 
				logger.error("** PROBLEM ** Non Valid Local Ontology ID "+ localOntologyID, e );
			}
		}
		
		// Put all conceptIDs collection to StringBuffer.
		StringBuilder conceptIDs=new StringBuilder();
		if(concepts!= null && concepts.size() >0){
			 
			for (Iterator<String> iterator = concepts.iterator(); iterator
					.hasNext();) {
				conceptIDs.append(iterator.next());
				conceptIDs.append(GT_SEPARATOR_STRING);
				
			}
			
			conceptIDs.delete(conceptIDs.length()-2, conceptIDs.length());
		}
		
		return conceptIDs.toString();
	}
	
	/**
	 * This method gets local concept ids for Set of terms for given localOntologyID
	 * 
	 * @param terms Set of term strings.
	 * @param localOntologyID	 
	 * @return String containing local concept id's separated by '>' 
	 */
	public String mapTermsToLocalConceptIDs(HashSet<String> terms, String localOntologyID){
		  
		HashSet<String> concepts= new HashSet<String>();
		
		for (String term : terms) {
			try {
				concepts.addAll(commonObsDao.mapStringToLocalConceptIDs(term.trim(),localOntologyID));
			} catch (Exception e) {				 
				logger.error("** PROBLEM ** Non Valid Local Ontology ID "+ localOntologyID );
			}
		} 
		// Put all conceptIDs collection to StringBuffer.
		StringBuilder conceptIDs=new StringBuilder();
		if (!concepts.isEmpty()){    	   
			for (Iterator<String> iterator = concepts.iterator(); iterator
					.hasNext();) {
				conceptIDs.append(iterator.next());
				conceptIDs.append(GT_SEPARATOR_STRING);
				
			}
			
			conceptIDs.delete(conceptIDs.length()-2, conceptIDs.length());
		}
		
		return conceptIDs.toString();
	}
	
}
