/**
 * 
 */
package org.ncbo.stanford.obr.service.index;

import obs.obr.populate.ObrWeight;

/**
 * @author Kuladip Yadav
 *
 */
public interface IndexationService {
	
	/**
	 * Processes the resource direct & expanded annotations to produce the index and
	 * populates the the corresponding _IT using a set weights.
	 * This function implements the step 4 of the OBR workflow.
	 * 
	 * @param weights  Used for calculating score
	 * @return The number of annotations created in the index. 
	 * 
	 */
	public int indexation(ObrWeight weights);
	
	/**
	 * Method removes indexing done for given ontology version.
	 * 
	 * @param localOntologyID String containing ontology version.
	 */
	public void removeIndexation(String localOntologyID);

}
