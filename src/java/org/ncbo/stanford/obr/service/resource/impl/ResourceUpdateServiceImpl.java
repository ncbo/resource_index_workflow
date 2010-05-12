package org.ncbo.stanford.obr.service.resource.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import obs.common.beans.DictionaryBean;
import obs.common.utils.ExecutionTimer;
import obs.obr.populate.Element;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.dao.statistics.StatisticsDao.StatisticsEntry;
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
				if (this.addElement(element)){
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
		resourceTableDao.addEntryOrUpdate(resource);
	}
 
	public int numberOfEntry() {		 
		return elementTableDao.numberOfEntry();
	} 

	public void reInitializeAllTables() {
		elementTableDao.reInitializeSQLTable();
		reInitializeAllTablesExcept_ET();
	}

	public void reInitializeAllTablesExcept_ET() {
		elementTableDao.resetDictionary();
		directAnnotationTableDao.reInitializeSQLTable();
		isaExpandedAnnotationTableDao.reInitializeSQLTable();
		mapExpandedAnnotationTableDao.reInitializeSQLTable();
		indexTableDao.reInitializeSQLTable();
		resourceTableDao.resetDictionary(resourceAccessTool.getToolResource().getResourceID());
		statisticsDao.deleteStatisticsForResource(resourceAccessTool.getToolResource().getResourceID());
	}
	
	/**
	 * Returns a set of all the localElementIDs contained in the table. 
	 */
	public HashSet<String> getAllLocalElementIDs(){
		return elementTableDao.getAllLocalElementIDs();
	}
	
	/**
	 * Returns a set of all the values contained in the given column of table. 
	 */
	public HashSet<String> getAllValuesByColumn(String columName){
		return elementTableDao.getAllValuesByColumn(columName);
	}
	
	/**
	 * Returns the value of a given context for a given element in the table.
	 */
	public String getContextValueByContextName(String localElementID, String contextName){
		return elementTableDao.getContextValueByContextName(localElementID, contextName);
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
				concepts.addAll(termDao.mapStringToLocalConceptIDs(term.trim(),localOntologyID));
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
				concepts.addAll(termDao.mapStringToLocalConceptIDs(term.trim(),localOntologyID));
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

	/* (non-Javadoc)
	 * @see org.ncbo.stanford.obr.service.resource.ResourceUpdateService#mapTermsToVirtualLocalConceptIDs(java.lang.String, java.lang.String, java.lang.String)
	 */
	public String mapTermsToVirtualLocalConceptIDs(String terms,
			String virtualOntologyID, String splitString) {
		String localOntologyID= ontologyDao.getLatestLocalOntologyID(virtualOntologyID );
		
		String concepts =mapTermsToLocalConceptIDs(terms, localOntologyID, splitString );
		 
		return concepts.replaceAll(localOntologyID, virtualOntologyID);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ncbo.stanford.obr.service.resource.ResourceUpdateService#mapTermsToVirtualLocalConceptIDs(java.util.HashSet, java.lang.String, java.lang.String)
	 */
	public String mapTermsToVirtualLocalConceptIDs(java.util.HashSet<String> terms, String virtualOntologyID ) {
		String localOntologyID= ontologyDao.getLatestLocalOntologyID(virtualOntologyID );
		
		String concepts =mapTermsToLocalConceptIDs(terms, localOntologyID);
		 
		return concepts.replaceAll(localOntologyID, virtualOntologyID);

		
	};
	
	/*
	 * (non-Javadoc)
	 * @see org.ncbo.stanford.obr.service.resource.ResourceUpdateService#getLocalConceptIdByPrefNameAndOntologyId(java.lang.String, java.lang.String)
	 */
	public String getLocalConceptIdByPrefNameAndOntologyId(String virtualOntologyID, String termName){
	   String localOntologyID= ontologyDao.getLatestLocalOntologyID(virtualOntologyID );
	   
	   return ontologyDao.getLocalConceptIdByPrefNameAndOntologyId(localOntologyID, termName);
	}
	
	
	/**
	 * This method calculates number of indexed annotations, mgrep annotations, reported annotations, isa annotations, mapping annotations
	 * for current resource.
	 * 
	 */
	public void calculateObrStatistics(boolean withCompleteDictionary, DictionaryBean dictionary) {
		
		logger.info("Processing of statistics started...");
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		
		// Getting Indexed annotations
		HashMap<Integer, Integer> indexedAnnotations= indexTableDao.getIndexedAnnotationStatistics(withCompleteDictionary, dictionary);

		// Getting MGREP annotations
		HashMap<Integer, Integer> mgrepAnnotations = directAnnotationTableDao.getMgrepAnnotationStatistics(withCompleteDictionary, dictionary);
		
		// Getting REPORTED annotations
		HashMap<Integer, Integer> reportedAnnotations= directAnnotationTableDao.getReportedAnnotationStatistics(withCompleteDictionary, dictionary);
		
		// Getting ISA annotations
		HashMap<Integer, Integer> isaAnnotations= isaExpandedAnnotationTableDao.getISAAnnotationStatistics(withCompleteDictionary, dictionary);
		
		// Getting Mapping annotations
		HashMap<Integer, Integer> mappingAnnotations= mapExpandedAnnotationTableDao.getMappingAnnotationStatistics(withCompleteDictionary, dictionary);
		
		HashSet<StatisticsEntry> entries = new HashSet<StatisticsEntry>();
		
		int indexed;
		int mgrep;
		int reported;
		int isA ;
		int mapping;
		
		// Get resource id (primary key) from ResourceTable
		int resource_id = resourceTableDao.getResourceIDKey(resourceAccessTool.getToolResource().getResourceID()); 

		// Get list of onltogyID used for indexing 
		ArrayList<Integer> ontologyListFromStatsTables = statisticsDao.getOntolgyIDsForResource(resource_id);
		
		// Iterating for each ontologies
		for (Integer ontologyID : indexedAnnotations.keySet()) {			
			
			ontologyListFromStatsTables.remove(ontologyID);
			
			
			if(indexedAnnotations.get(ontologyID)!= null){
				indexed = indexedAnnotations.get(ontologyID).intValue();
			}else{
				indexed = 0;
			}
			
			if(mgrepAnnotations.get(ontologyID)!= null){
				mgrep = mgrepAnnotations.get(ontologyID).intValue();
			}else{
				mgrep = 0;
			}
			
			if(reportedAnnotations.get(ontologyID)!= null){
				reported = reportedAnnotations.get(ontologyID).intValue();
			}else{
				reported = 0;
			}
			
			if(isaAnnotations.get(ontologyID)!= null){
				isA = isaAnnotations.get(ontologyID).intValue();
			}else{
				isA = 0;
			}
			
			if(mappingAnnotations.get(ontologyID)!= null){
				mapping = mappingAnnotations.get(ontologyID).intValue();
			}else{
				mapping = 0;
			} 
			
			// Creating entry for OBR_STATS table
			StatisticsEntry entry= new StatisticsEntry(resource_id, ontologyID.intValue(), indexed, mgrep, reported, isA, mapping);
			entries.add(entry);
		}
		
		// Adding/updating entries for OBR_STATS tables.
		int noOfEntiesUpdated=statisticsDao.addEntries(entries);
		
		// Remove non updated entries from stats table.
		for (Integer integer : ontologyListFromStatsTables) {
			statisticsDao.deleteEntry(resource_id, integer);
		}
		
		logger.info("Number of entries added/updated in statistics table are :" + noOfEntiesUpdated);
		timer.end();
		logger.info("Resource " + resourceAccessTool.getToolResource().getResourceName()
				+ " statistics processed in: "
				+ timer.millisecondsToTimeString(timer.duration()));
		logger.info("Processing of statistics completed.");
		
	} 
	
	/**
	 * This method gets latest version of ontology for given virtual ontology id
	 * 
	 * @param virtualOntologyID 
	 * @return String of latest version of ontology.
	 */
	public String getLatestLocalOntologyID(String virtualOntologyID) {
		return ontologyDao.getLatestLocalOntologyID(virtualOntologyID );
	}

	/**
	 * Method update resource table with total number of element and update date.
	 * 
	 * @param resource {@code Resource} to be updated. 
	 * @return boolean {@code true} if updated successfully.
	 */
	public boolean updateResourceUpdateInfo(Resource resource) {
		// TODO Auto-generated method stub
		int totalElements = elementTableDao.getTotalNumberOfElement();
		return resourceTableDao.updateNumberOfElementAndDate(resource.getResourceID(), totalElements);
		
	}

	/**
	 * Method update resource table after completion of resource workflow.
	 * It includes updation of dictionary id and date for resource workflow completed.
	 * 
	 * @param resource {@code Resource} to be updated. 
	 * @return boolean {@code true} if updated successfully.
	 */
	public boolean updateResourceWorkflowInfo(Resource resource) {		
		 DictionaryBean dictionary = dictionaryDao.getLastDictionaryBean();
		 return resourceTableDao.updateDictionaryAndWorkflowDate(resource, dictionary.getDictionaryID());
		
	}
}
