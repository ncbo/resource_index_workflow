package org.ncbo.stanford.obr.service.workflow.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import obs.common.beans.DictionaryBean;
import obs.common.beans.IsaContextBean;
import obs.common.beans.MappingContextBean;
import obs.common.beans.MgrepContextBean;
import obs.common.beans.OntologyBean;
import obs.common.beans.ReportedContextBean;
import obs.common.utils.ExecutionTimer;
import obs.common.utils.Utilities;
import obs.obr.populate.ObrWeight;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.DaoFactory;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.service.obs.ObsDataPopulationService;
import org.ncbo.stanford.obr.service.obs.impl.ObsDataPopulationServiceImpl;
import org.ncbo.stanford.obr.service.workflow.ResourceIndexWorkflow;
import org.ncbo.stanford.obr.util.FileResourceParameters;
import org.ncbo.stanford.obr.util.LoggerUtils;
import org.ncbo.stanford.obr.util.MessageUtils;
import org.ncbo.stanford.obr.util.StringUtilities;

/**
 * A service  {@code ResourceIndexWorkflowImpl} is main service for workflow execution
 * it includes methods for populating slave obs tables and processing different resource tools with 
 * population of elements and indexing them using slave obs tables.
 * 
 * <p>Also, it includes functionality for removing duplicate ontologies.
 *  
 * @author Kuladip Yadav
 */
public class ResourceIndexWorkflowImpl implements ResourceIndexWorkflow, DaoFactory {

	// Logger for this class
	private static Logger logger;

	private static ObrWeight obrWeights = new ObrWeight(
			MgrepContextBean.PDA_WEIGHT, MgrepContextBean.SDA_WEIGHT,
			IsaContextBean.IEA_FACTOR, MappingContextBean.MEA_WEIGHT,
			ReportedContextBean.RDA_WEIGHT);
	
	private ObsDataPopulationService obsDataPopulationService = new ObsDataPopulationServiceImpl();
	  
	public ResourceIndexWorkflowImpl() {
		logger = LoggerUtils.createOBRLogger(ResourceIndexWorkflowImpl.class);
		
		try {
			AbstractObrDao.setSqlLogFile(new File("resource_index_workflow.sql"));
		} catch (IOException e) {
			logger.error("Problem in creating SQL log file.", e);
		}
	}
	
	/**
	 * This method populates slave obs tables from master obs tables which includes
	 * ontology table, concept table, term table, relation table and mapping table.
	 * 
	 * <p>This method compares slave and master ontology tables and populate newly added data in slave tables.
	 */
	public void populateObsSlaveTables() {
		 logger.info("Populating obs slave tables starts");	
		 boolean withLatestDictionary = Boolean.parseBoolean(MessageUtils.getMessage("obs.slave.dictionary.latest"));
		 
		 this.obsDataPopulationService.populateObsSlaveData(withLatestDictionary);			 
		 System.gc();
		 logger.info("Populating obs slave tables completed.");	
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ncbo.stanford.obr.service.workflow.ResourceIndexWorkflow#loadObsSlaveTablesIntoMemeory()
	 */
	public void loadObsSlaveTablesIntoMemory() {
		 logger.info("Populating obs slave memory  tables starts");		  
		 this.obsDataPopulationService.loadObsSlaveTablesIntoMemory();			 
		 System.gc();
		 logger.info("Populating obs slave memory completed.");	 		
	}

	/**
	 * This method includes complete resource index workflow. It process resources and 
	 * update elements for them and annotated them using obs tables.
	 * 
	 * <P>This methods process all the resources included in properties file.
	 * 
	 */
	public void startResourceIndexWorkflow() { 
		ExecutionTimer workflowTimer = new ExecutionTimer();
		workflowTimer.start();
		// gets all resource ids for processing, 
		String[] resourceIDs = StringUtilities.splitSecure(MessageUtils
				.getMessage("obr.resource.ids"), ",");		
		
		//Initialize the Execution timer 		
		ExecutionTimer timer = new ExecutionTimer();		
		logger.info("The Resources index Workflow Started.");	
		for (String resourceID : resourceIDs) {
			ResourceAccessTool resourceAccessTool = null;
			try {
				// Create resource tool object using reflection.
				resourceAccessTool = (ResourceAccessTool) Class.forName(
						MessageUtils.getMessage("resource."
								+ resourceID.toLowerCase())).newInstance();				 
				logger.info("Start processing Resource " + resourceAccessTool.getToolResource().getResourceName() + "....");
				timer.start();
				resourceProcessing(resourceAccessTool);
				timer.end();
				logger.info("Resource " + resourceAccessTool.getToolResource().getResourceName() + " processed in: " + timer.millisecondsToTimeString(timer.duration()));
			} catch (Exception e) {
				logger.error(
						"Problem in creating resource tool for resource id : "
								+ resourceID, e);
			}finally{				
				resourceAccessTool= null;
				System.gc();
			}

		}
		workflowTimer.end();
		logger.info("Resources index Workflow completed in : " + timer.millisecondsToTimeString(timer.duration()));	
	}

	/**
	 * This method process individual resource and update elements for it.
	 * Also it annotate that elements using obs tables and index them. 
	 * 
	 * @param resourceAccessTool a {@code ResourceAccessTool} to be processed. 
	 */
	public void resourceProcessing(ResourceAccessTool resourceAccessTool) {
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		// Creating logger for resourceAcessTool
		Logger toolLogger = ResourceAccessTool.getLogger();
		toolLogger.info("**** Resource "
				+ resourceAccessTool.getToolResource().getResourceID() + " processing");
		// Adds resource entry into Resource Table(OBR_RT)
		resourceAccessTool.addResourceTableEntry();

		// Re-initialized tables
		if (Boolean.parseBoolean(MessageUtils
				.getMessage("obr.reinitialize.all"))) {
			resourceAccessTool.reInitializeAllTables();
		} else if (Boolean.parseBoolean(MessageUtils
				.getMessage("obr.reinitialize.only.annotation"))) {
			resourceAccessTool.reInitializeAllTablesExcept_ET();
		}

		// Update resource for new elements 
		if (Boolean
				.parseBoolean(MessageUtils.getMessage("obr.update.resource"))) {
			int nbElement = resourceAccessTool.updateResourceContent();
			resourceAccessTool.updateResourceUpdateInfo();
			
			toolLogger.info("Resource "
					+ resourceAccessTool.getToolResource().getResourceName()
					+ " updated with " + nbElement + " elements.");
		}

		// gets the latest dictionary from OBS_DVT
		DictionaryBean dictionary = dictionaryDao.getLastDictionaryBean();
		
		// value for withCompleteDictionary parameter.
		boolean withCompleteDictionary = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.dictionary.complete"));
		
		// Execute the workflow according to resource type. 
		int nbIndexedAnnotation= executeWorkflow(resourceAccessTool, dictionary, withCompleteDictionary,  toolLogger);
				
		// Update obr_statistics table.
		if(nbIndexedAnnotation > 0) {
			resourceAccessTool.calculateObrStatistics(withCompleteDictionary, dictionary);
		} 
		// Update resource table entry for latest DictionaryID and 
		resourceAccessTool.updateResourceWorkflowInfo();
		
		timer.end();
		toolLogger.info("Resource " + resourceAccessTool.getToolResource().getResourceName()
				+ " processed in: "
				+ timer.millisecondsToTimeString(timer.duration()));		
	}
	
	/**
	 * This method execute resource. 
	 * 
	 * <p>After creating direct annotations perform expanded annotation on newly created direct
	 * annotation and index it. 
	 * 
	 * @param resourceAccessTool {@code ResourceAccessTool}
	 * @param dictionary {@code DictionaryBean) containing latest dictionary
	 * @param toolLogger {@code Logger} object for given resourceAccessTool
	 */
	private int executeWorkflow(ResourceAccessTool resourceAccessTool, DictionaryBean dictionary, boolean withCompleteDictionary, Logger toolLogger){
		
		int nbEntry ;		
		// Total number of entries found in element table.		 
		nbEntry = resourceAccessTool.numberOfElement();	 
		
		// Processing direct annotations
		int nbDirectAnnotation = resourceAccessTool.getAnnotationService()
				.resourceAnnotation(withCompleteDictionary, dictionary, 
						Utilities.arrayToHashSet(FileResourceParameters.STOP_WORDS)); 
		
		toolLogger.info(nbEntry + " elements annotated (with "
				+ nbDirectAnnotation
				+ " new direct annotations) from resource "
				+ resourceAccessTool.getToolResource().getResourceID() + ".");

		// Flag for mapping expansion.  
		boolean isaClosureExpansion = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.expansion.relational"));
		
		// Flag for mapping expansion.
		boolean mappingExpansion = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.expansion.mapping"));
		
		// Flag for distance expansion.
		boolean distanceExpansion = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.expansion.distance"));

		// Creating semantic expansion annotation.
		int nbExpandedAnnotation = resourceAccessTool.getSemanticExpansionService()
				.semanticExpansion(isaClosureExpansion, mappingExpansion,
						distanceExpansion);
		toolLogger.info(nbEntry + " elements annotated (with "
				+ nbExpandedAnnotation
				+ " new expanded annotations) from resource "
				+ resourceAccessTool.getToolResource().getResourceID() + ".");
		
		//Create indexes on Annotation and expanded annotation table.
		toolLogger.info("Creating indexes on Annotation and expanded annotation table starts ..");
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		resourceAccessTool.createIndexForAnnotationTables();
		timer.end();
		toolLogger.info("Creating indexes on Annotation and expanded annotation table completed in "
				+ timer.millisecondsToTimeString(timer.duration()));			
		toolLogger.info("Creating indexes on Annotation and expanded annotation table ends ..");
		
		// Indexation step to annotations.
		int nbIndexedAnnotation = resourceAccessTool.getIndexationService().indexation(
				obrWeights);
		toolLogger.info(nbEntry + " elements indexed (with "
				+ nbIndexedAnnotation
				+ " new indexed annotations) from resource "
				+ resourceAccessTool.getToolResource().getResourceID() + ".");			
			 
		return nbIndexedAnnotation;  
		
	} 
 
	/**
	 * Deletes the ontology duplicates from the OBS slave tables and all the resource index tables.
	 * which includes ontology table, concept table, term table, relation table and mapping table
	 * and annotations tables for all resources.
	 *  
	 * <p>This method ensures to keep only the latest version of ontologies.	  
	 */
	public void removeOntologyDuplicates() {
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		
		logger.info("Remove onotlogy duplicates started....");
		Set<String> ontologiesToRemove = new HashSet<String>();		 
		String virtualOntologyID1;
		String localOntologyID1;
		String virtualOntologyID2;
		String localOntologyID2;
		// Get all ontology beans from ontology tables.
		List<OntologyBean> allOntologyBeans = ontologyDao.getAllOntologyBeans();
		
		// Check for duplicate ontologies i.e two or more versions of same ontology
		for (OntologyBean ontologyBean1 : allOntologyBeans) {
			localOntologyID1= ontologyBean1.getLocalOntologyID();
			virtualOntologyID1 =ontologyBean1.getVirtualOntologyID();
			// traverses all the ontologies of the the OBS DB
			for (OntologyBean ontologyBean2 : allOntologyBeans) {
				localOntologyID2= ontologyBean2.getLocalOntologyID();
				virtualOntologyID2= ontologyBean2.getVirtualOntologyID();				
				// searches for duplicates
				if(virtualOntologyID1.equals(virtualOntologyID2) && !localOntologyID1.equals(localOntologyID2) ){
					// removes the ontology with the smallest localOntologyID (that situation will should happen only for BioPortal ontologies)
					if(Integer.parseInt(localOntologyID1)>Integer.parseInt(localOntologyID2)){
						ontologiesToRemove.add(localOntologyID2);
					}
					else{
						ontologiesToRemove.add(localOntologyID1);
					}
				}
			}
		}
				 
		// remove from obr tables.
		logger.info("Removing dulicate ontology version from obr tables started");	
		removeOntologiesFromOBRTables(new ArrayList<String>(ontologiesToRemove));
		logger.info("Removing dulicate ontology version from obr tables completed");	
		// Iterating each duplicate ontology version and remove from obr and obs tables.
		
		logger.info("Removing dulicate ontology version from obs slave tables started");	
		for (String localOntologyID : ontologiesToRemove) {
			logger.info("Removing ontology version :" + localOntologyID);			 
			// remove ontology from obs slave database.
			obsDataPopulationService.removeOntology(localOntologyID);
		} 
		logger.info("Removing dulicate ontology version from obs slave tables completed.");	
		timer.end();
		
		logger.info("Remove dupicate ontologies completed in: "
			+ timer.millisecondsToTimeString(timer.duration()));
	}
	
	/**
	 * Remove all the annotations done by given by localOntology for all the resources.
	 * 
	 * @param localOntologyID ontology version
	 */
	private void removeOntologiesFromOBRTables(List<String> localOntologyIDs){
		// gets all resource ids for processing, 
		String[] resourceIDs = StringUtilities.splitSecure(MessageUtils
				.getMessage("obr.resource.ids"), ",");
		//Initialize the Execution timer 
		ExecutionTimer timer = new ExecutionTimer();	
		timer.start();
		logger.info("The Remove ontology from OBR tables Started.");	
		for (String resourceID : resourceIDs) {
			ResourceAccessTool resourceAccessTool = null;
			try {
				// Create resource tool object using reflection.
				resourceAccessTool = (ResourceAccessTool) Class.forName(
						MessageUtils.getMessage("resource."
								+ resourceID.toLowerCase())).newInstance();	 
				resourceAccessTool.removeOntologies(localOntologyIDs); 
				
			} catch (Exception e) {
				logger.error(
						"Problem in creating resource tool for resource id : "
								+ resourceID, e);
			}finally{				
				resourceAccessTool= null;
				System.gc();
			}

		}		
		timer.end();
		logger.info("The Remove ontology from OBR tables processed in: " + timer.millisecondsToTimeString(timer.duration()));
		 
	}

}
