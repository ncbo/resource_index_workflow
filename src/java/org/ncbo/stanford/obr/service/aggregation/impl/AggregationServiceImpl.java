package org.ncbo.stanford.obr.service.aggregation.impl;

import java.util.List;

import obs.common.utils.ExecutionTimer;
import obs.obr.populate.ObrWeight;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.service.AbstractResourceService;
import org.ncbo.stanford.obr.service.aggregation.AggregationService;

public class AggregationServiceImpl extends AbstractResourceService implements AggregationService{

	public AggregationServiceImpl(ResourceAccessTool resourceAccessTool) {
		super(resourceAccessTool);
	} 

	/**
	 * Processes the resource direct & expanded annotations to produce the index and
	 * populates the the corresponding _IT using a set weights.
	 * This function implements the step 4 of the OBR workflow.
	 * Returns the number of annotations created in the index. 
	 */
	public long aggregation(ObrWeight weights){
		long nbAnnotation;
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		logger.info("*** Executing aggregation process.... ");
		nbAnnotation = aggregationTableDao.aggregation(weights);
		timer.end();
		logger.info("### Aggregation processed in: " + timer.millisecondsToTimeString(timer.duration()));
		return nbAnnotation;
	} 
	
	/**
	 * Method removes indexing done for given ontology versions.
	 * 
	 * <p>For big resources, it remove local ontology id one by one
	 * <p>For other resources remove all local ontology ids
	 * 
	 * @param {@code List} of localOntologyID containing ontology versions.
	 */
	public void removeAggregation(List<String> localOntologyIDs) {
		 if(resourceAccessTool.getResourceType()!= ResourceType.BIG){
			 aggregationTableDao.deleteEntriesFromOntologies(localOntologyIDs);	
		 }else{
			 for (String localOntologyID : localOntologyIDs) {
				 aggregationTableDao.deleteEntriesFromOntology(localOntologyID);
			}
			 
		 }		 	
	}

	/**
	 * Method calculates concept frequency from aggregation table 
	 *  
	 * @return The number of annotations created in the index. 	
	 */
	public long calulateConceptFrequncy() {
		long nbAnnotation;
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		logger.info("*** Executing concept frequency process.... ");
		nbAnnotation = conceptFrequencyDao.calulateConceptFrequncy();
		timer.end();
		logger.info("### Concept frequency  processed in: " + timer.millisecondsToTimeString(timer.duration()));
		return nbAnnotation;
	}

	/**
	 * Method removes concept frequency calculation done for given ontology versions.
	 * 
	 * @param {@code List} of localOntologyID String containing ontology version.
	 */
	public void removeConceptFrequncy(List<String> localOntologyIDs) {		 
		 aggregationTableDao.deleteEntriesFromOntologies(localOntologyIDs);	
	} 
	
	
}
