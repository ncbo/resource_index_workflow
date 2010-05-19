/**
 * 
 */
package org.ncbo.stanford.obr.service.aggregation;

import java.util.List;

import obs.obr.populate.ObrWeight;

/**
 * @author Kuladip Yadav
 *
 */
public interface AggregationService {
	
	/**
	 * Processes the resource direct & expanded annotations to produce the index and
	 * populates the the corresponding _IT using a set weights.
	 * This function implements the step 4 of the OBR workflow.
	 * 
	 * @param weights  Used for calculating score
	 * @return The number of annotations created in the index. 
	 * 
	 */
	public int aggregation(ObrWeight weights);
	
	/**
	 * Method removes indexing done for given ontology versions.
	 * 
	 * @param {@code List} of localOntologyID String containing ontology version.
	 */
	public void removeAggregation(List<String> localOntologyID);

}
