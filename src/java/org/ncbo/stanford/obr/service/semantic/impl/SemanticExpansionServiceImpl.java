package org.ncbo.stanford.obr.service.semantic.impl;

import java.util.List;

import obs.common.utils.ExecutionTimer;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.service.AbstractResourceService;
import org.ncbo.stanford.obr.service.semantic.SemanticExpansionService;

public class SemanticExpansionServiceImpl extends AbstractResourceService implements SemanticExpansionService{

	public SemanticExpansionServiceImpl(ResourceAccessTool resourceAccessTool) {
		super(resourceAccessTool);
		// TODO Auto-generated constructor stub
	}	 

	/** 
	 * Processes the resource direct annotations to produce expanded annotations and
	 * populates the the corresponding _EAT.
	 * This function implements the step 3 of the OBR workflow.
	 * The 3 booleans corresponds to the semantic expansion component to use.
	 * 
	 * @param isaClosureExpansion {@code boolean} for is a closure expansion
	 * @param mappingExpansion    {@code boolean} for mapping expansion
	 * @param distanceExpansion   {@code boolean} for mapping expansion
	 * @return                    the number of direct annotations created. 
	 */
	public int semanticExpansion(boolean isaClosureExpansion, boolean mappingExpansion, boolean distanceExpansion){
		int nbAnnotation = 0;
		ExecutionTimer timer = new ExecutionTimer();
		// isaClosure expansion
		if(isaClosureExpansion){
			timer.start();
			logger.info("Executing isa transitive closure expansion... ");
			int isaAnnotation; 
			
			isaAnnotation = isaExpandedAnnotationTableDao.isaClosureExpansion(directAnnotationTableDao);
				
			logger.info(isaAnnotation);
			nbAnnotation += isaAnnotation;
			timer.end();
			logger.info("Isa transitive closure expansion  processed in: " + timer.millisecondsToTimeString(timer.duration()));
			timer.reset();
		}
		// mapping expansion
		if(mappingExpansion){
			timer.start();
			logger.info("Executing mapping expansion... ");
			int mappingAnnotation = mapExpandedAnnotationTableDao.mappingExpansion(directAnnotationTableDao);
			logger.info(mappingAnnotation);
			nbAnnotation += mappingAnnotation;
			timer.end();
			logger.info("Mapping expansion processed in: " + timer.millisecondsToTimeString(timer.duration()));
			timer.reset();
		}
		// distance expansion
		if(distanceExpansion){
			timer.start();
			logger.info("Distance semantic expansion not implemeted yet.");
			timer.end();
			logger.info("Distance expansion processed in: " + timer.millisecondsToTimeString(timer.duration()));
			timer.reset();
		}
		return nbAnnotation;
	}
	
	/**
	 * Method removes expanded annotations for given ontology versions.Entries are remove from 
	 * is a parent relation and mapping relation.
	 * 
	 * <p>For big resources, it remove local ontology id one by one
	 * <p>For other resources remove all local ontology ids
	 * 
	 * @param {@code List} of localOntologyIDs String containing ontology versions.
	 */
	public void removeExpandedAnnotations(List<String> localOntologyIDs) {
		
		if(resourceAccessTool.getResourceType()!= ResourceType.BIG){
			isaExpandedAnnotationTableDao.deleteEntriesFromOntologies(localOntologyIDs); 
			mapExpandedAnnotationTableDao.deleteEntriesFromOntologies(localOntologyIDs); 
		 }else{
			 for (String localOntologyID : localOntologyIDs) {
				 isaExpandedAnnotationTableDao.deleteEntriesFromOntology(localOntologyID); 
				 mapExpandedAnnotationTableDao.deleteEntriesFromOntology(localOntologyID); 
			}
			 
		 }	
	}

	/*
	 * (non-Javadoc)
	 * @see org.ncbo.stanford.obr.service.semantic.SemanticExpansionService#createIndexForExpandedAnnotationTable()
	 */
	public void createIndexForExpandedAnnotationTables() {
		if(!isaExpandedAnnotationTableDao.indexesExist()){
			isaExpandedAnnotationTableDao.createIndexes();	 
		} 
		
		if(!mapExpandedAnnotationTableDao.indexesExist()){
			mapExpandedAnnotationTableDao.createIndexes();	 
		}  
		
	}

}
