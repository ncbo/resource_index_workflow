package org.ncbo.stanford.obr.service.index.impl;

import obs.common.utils.ExecutionTimer;
import obs.obr.populate.ObrWeight;

import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.service.AbstractResourceService;
import org.ncbo.stanford.obr.service.index.IndexationService;

public class IndexationServiceImpl extends AbstractResourceService implements IndexationService{

	public IndexationServiceImpl(ResourceAccessTool resourceAccessTool) {
		super(resourceAccessTool);
		// TODO Auto-generated constructor stub
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

	 

}
