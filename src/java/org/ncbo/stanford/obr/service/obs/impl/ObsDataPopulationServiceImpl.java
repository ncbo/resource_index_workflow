package org.ncbo.stanford.obr.service.obs.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import obs.common.beans.DictionaryBean;
import obs.common.utils.ExecutionTimer;
import obs.common.utils.Utilities;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.dao.DaoFactory;
import org.ncbo.stanford.obr.dao.obs.master.ObsMasterDao;
import org.ncbo.stanford.obr.dao.obs.ontology.OntologyDao.OntologyEntry;
import org.ncbo.stanford.obr.service.obs.ObsDataPopulationService;

/**
 * This service class {@code ObsDataPopulationServiceImpl} is provides implementation for populating obs slave data from master table which is used for
 * populating resource index.
 *  
 * @author Kuladip Yadav
 */
public class ObsDataPopulationServiceImpl implements ObsDataPopulationService, DaoFactory{
	 
	/** Default logger for {@code ObsDataPopulationServiceImpl} class. */
	private static Logger logger = Logger.getLogger(ObsDataPopulationServiceImpl.class);
	
	/** The obsMasterDao used for querying OBS master database. */
	private ObsMasterDao obsMasterDao;
	 
	/**
	 * Populates all the OBS master tables in the right sequence in order to traverse ontologies only once and reuse
	 * what has been already processed.
	 *
	 * @param withLatestDictionary if true then will populate OBS data with latest dictionary without creating new dictionary
	 * 							   if false, a new dictionary row in dictionary table is created.
	 */	
	public void populateObsSlaveData(boolean withLatestDictionary) {
		// initialize obs master dao
		if(obsMasterDao== null){
			obsMasterDao = ObsMasterDao.getInstance();
		}
		 
		//Initialize the Execution timer 
     	ExecutionTimer timer = new ExecutionTimer();
     	timer.start();
     	logger.info("Population of slave data from master obs database started.");
		// populates or reuse dictionary 
		if(dictionaryDao.numberOfEntry()==0 || !withLatestDictionary){
			dictionaryDao.addEntry(DictionaryBean.DICO_NAME+Utilities.getRandomString(4));
		}
		// Getting latest dictionary.
		int dictionaryID = dictionaryDao.getLastDictionaryBean().getDictionaryID();
		// Get ontologies available currently in slave table. 
		List<String> currentSlaveOntologies = ontologyDao.getAllLocalOntologyIDs();
		List<String>  localOntologyIDs = populateOntologySlaveData(dictionaryID, currentSlaveOntologies);
		
		if(localOntologyIDs != null && localOntologyIDs.size()>0){
			populateConceptsSlaveData(localOntologyIDs);
		    populateTermsSlaveData(localOntologyIDs);		 
			populateRelationSlaveData(localOntologyIDs);
			populateMappingSlaveData(localOntologyIDs);
		}else{
			logger.info("No new ontology found in master table.");
		}
		logger.info("Population of slave data from master obs database completed.");
		timer.end();		
		logger.info("Population of slave data processed in : " + timer.millisecondsToTimeString(timer.duration()));
		// Release the master database connection.
		obsMasterDao.closeConnection();
	}
 
	/**
	 * Populates new ontology versions present in OBS master database which are not present in 
	 * slave ontology table with particular dictionary. 
	 *  
	 * @param dictionaryID ID assign to new ontology versions from master table.
	 * @param currentSlavLocalOntologyIDs {@code List} of local ontology ids currently present in slave database.
	 * @return a list of newly populated local ontology ids from master table.
	 */
	public List<String> populateOntologySlaveData(int dictionaryID, List<String> currentSlavLocalOntologyIDs) {
		List<OntologyEntry>  ontologyEnties= obsMasterDao.getMasterOntologyEntries(dictionaryID, currentSlavLocalOntologyIDs);
		List<String> localOntologyIDs = new ArrayList<String>();
		int numberOfOntologiesAdded= ontologyDao.addEntries(ontologyEnties);	
		// Adding populated local ontology ids in list.
		for (OntologyEntry ontologyEntry : ontologyEnties) {
			localOntologyIDs.add(ontologyEntry.getLocalOntologyID());
		}		
		logger.info("Number of ontology entries added in slave ontology table : " + numberOfOntologiesAdded);
		
		return localOntologyIDs;
	} 
	
	/**
	 * Populates new concepts presents in OBS master database which are not present in 
	 * slave concept table for given ontology versions {@code localOntologyIDs}.
	 * 
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of concept entries added in slave concept table.
	 */
	public int populateConceptsSlaveData(List<String> localOntologyIDs) {
		int numberOfConceptsAdded= 0;
		File conceptEntryFile = null;		
		try{
			// Writes concept entries to file from master concept table. 
			conceptEntryFile = obsMasterDao.writeMasterConceptEntries(localOntologyIDs); 
			// load file entries into slave concept table. 
			numberOfConceptsAdded = conceptDao.populateSlaveConceptTableFromFile(conceptEntryFile);
			logger.info("Number of concept entries added in slave concept table : " + numberOfConceptsAdded);
		}finally {
			 // Delete generated file.
			 if(conceptEntryFile!= null && conceptEntryFile.exists()){
				 conceptEntryFile.delete();
			 }
		}		
		return numberOfConceptsAdded;
	} 
	
	/**
	 * Populates new term presents in OBS master database which are not present in 
	 * slave term table for given ontology versions {@code localOntologyIDs}.
	 * 
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of term entries added in slave term table.
	 */
	public int populateTermsSlaveData(List<String> localOntologyIDs) {		
		int numberOfTermsAdded= 0;
		File termsEntryFile = null;		
		try{
			// Writes term entries to file from master term table.
			termsEntryFile = obsMasterDao.writeMasterTermEntries(localOntologyIDs); 
			// Load file entries into slave term table. 
			numberOfTermsAdded = termDao.populateSlaveTermTableFromFile(termsEntryFile);
			logger.info("Number of term entries added in slave term table : " + numberOfTermsAdded);
		}finally {
			 // Delete generated file.
			 if(termsEntryFile!= null && termsEntryFile.exists()){
				 termsEntryFile.delete();
			 }
		}		
		return numberOfTermsAdded;
	} 
	
	/**
	 * Populates <b>is a parent</b> relation table entries presents in OBS master database which are not present in 
	 * slave relation table for given ontology versions {@code localOntologyIDs}.
	 * 
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of relation entries added in slave relation table.
	 */
	public int populateRelationSlaveData(List<String> localOntologyIDs){	
		int numberOfRelationsAdded= 0;
		File relationEntryFile = null;		
		try{
			// Remove all data from relation table.
			obsMasterDao.removeAllDataFromTable(relationDao.getTableSQLName());
			// Writes 'is a parent' relation entries to file from master relation table.
			relationEntryFile = obsMasterDao.writeMasterRelationEntries(localOntologyIDs);
			// Load file entries into slave term table. 
			numberOfRelationsAdded = relationDao.populateSlaveRelationTableFromFile(relationEntryFile);
			logger.info("Total Number of relations entries added in slave relation table : " + numberOfRelationsAdded);
		}finally {
			 if(relationEntryFile!= null && relationEntryFile.exists()){
				 relationEntryFile.delete();
			 }
		}
		return numberOfRelationsAdded;
	} 
	 
	/**
	 * Populates new concept mapping entries presents in OBS master database which are not present in 
	 * slave mapping table for given ontology versions {@code localOntologyIDs}.
	 * 
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of mapping entries added in slave map table.
	 */
	public int populateMappingSlaveData(List<String> localOntologyIDs){	
		int numberOfMappingsAdded = 0 ;
		File mappingEntryFile = null;
		try{
			// Remove all data from mapping table.
			obsMasterDao.removeAllDataFromTable(mapDao.getTableSQLName());
			// Writes mapping entries to file from master map table.
			mappingEntryFile = obsMasterDao.writeMasterMappingEntries(localOntologyIDs);
			// Load file entries into slave mapping table. 
			numberOfMappingsAdded = mapDao.populateSlaveMappingTableFromFile(mappingEntryFile);
			logger.info("Total Number of mapping entries added in slave map table : " + numberOfMappingsAdded);
		}finally {
			 if(mappingEntryFile!= null && mappingEntryFile.exists()){
				 mappingEntryFile.delete();
			 }
		}
		return numberOfMappingsAdded;
	}

	/**
	 * This method gets list of ontology versions present in master ontology id
	 * 
	 * @return {@code List} of local ontology id
	 */
	public List<String> getMasterSlaveLocalOntologyIDs() {		
		 return obsMasterDao.getAllLocalOntologyIDs();
	}

	/**
	 * This method removes given ontology version from all the obs slave tables
	 * i.e. removes entries from table obs_ontology, obs_concept, obs_term, obs_relation, obs_map
	 * 
	 * @param localOntologyID ontology version to remove. 
	 */
	public void removeOntology(String localOntologyID) {
		boolean status = false;
		 // remove ontology from relation table
		 status =relationDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from relation table.");
		 }
		 
		 // remove ontology from map table
		 status = mapDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from mapping table.");
		 }
		 
		 // remove ontology from term table
		 status =termDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from term table.");
		 }
		 
		 // remove ontology from concept table
		 status = conceptDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from concept table.");
		 }
		 
		 // remove ontology from ontology table
		 status = ontologyDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from ontology table.");
		 }
	}
}
