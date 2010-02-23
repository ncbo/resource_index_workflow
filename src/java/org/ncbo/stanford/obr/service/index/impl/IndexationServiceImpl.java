package org.ncbo.stanford.obr.service.index.impl;

import java.util.List;

import obs.common.utils.ExecutionTimer;
import obs.obr.populate.ObrWeight;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.service.AbstractResourceService;
import org.ncbo.stanford.obr.service.index.IndexationService;

public class IndexationServiceImpl extends AbstractResourceService implements IndexationService{

	public IndexationServiceImpl(ResourceAccessTool resourceAccessTool) {
		super(resourceAccessTool);
	} 

	/**
	 * Processes the resource direct & expanded annotations to produce the index and
	 * populates the the corresponding _IT using a set weights.
	 * This function implements the step 4 of the OBR workflow.
	 * Returns the number of annotations created in the index. 
	 */
	public int indexation(ObrWeight weights){
		int nbAnnotation;
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		nbAnnotation = indexTableDao.indexation(weights);
		timer.end();
		logger.info("Indexation processed in: " + timer.millisecondsToTimeString(timer.duration()));
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
	public void removeIndexation(List<String> localOntologyIDs) {
		 if(resourceAccessTool.getResourceType()!= ResourceType.BIG){
			 indexTableDao.deleteEntriesFromOntologies(localOntologyIDs);	
		 }else{
			 for (String localOntologyID : localOntologyIDs) {
				 indexTableDao.deleteEntriesFromOntology(localOntologyID);
			}
			 
		 }		 	
	} 
}
