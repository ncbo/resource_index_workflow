/**
 * 
 */
package org.ncbo.stanford.obr.service.semantic;

import java.util.List;

/**
 * @author Kuladip Yadav
 *
 */
public interface SemanticExpansionService {
	
	/**
	 * 
	 * Processes the resource direct annotations to produce expanded annotations and
	 * populates the the corresponding _EAT.
	 * This function implements the step 3 of the OBR workflow.
	 * The 3 booleans corresponds to the semantic expansion component to use.
	 *  
	 * 
	 * @param isaClosureExpansion  - boolean 
	 * @param mappingExpansion     - boolean
	 * @param distanceExpansion    - boolean  
	 * @return                     - the number of direct annotations created. 
	 */
	public int semanticExpansion(boolean isaClosureExpansion, boolean mappingExpansion, boolean distanceExpansion);

	/**
	 * Method removes expanded annotations for given ontology versions.Entries are remove from 
	 * is a parent relation and mapping relation.
	 * 
	 * @param {@code List} of localOntologyIDs String containing ontology versions.
	 */
	public void removeExpandedAnnotations(List<String> localOntologyIDs);
}
