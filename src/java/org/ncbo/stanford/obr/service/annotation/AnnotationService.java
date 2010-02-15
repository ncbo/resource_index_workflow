package org.ncbo.stanford.obr.service.annotation;

import java.util.HashSet;
import java.util.List;

/**
 * Service for processing direct annotations
 * 
 * @author Kuladip Yadav
 *
 */
public interface AnnotationService {
   
	/**
	 * 
	 * Processes the resource with Mgrep and populates the the corresponding _DAT.
	 * The given boolean specifies if the complete dictionary must be used, or not (only the delta dictionary).
	 * The annotation done with termName that are in the specified stopword list are removed from _DAT.
	 * This function implements the step 2 of the OBR workflow.
	 * Returns the number of direct annotations created. 
	 * 
	 * @param withCompleteDictionary
	 * @param stopwords
	 * @return int - Number of annotations .
	 */
	public int resourceAnnotation(boolean withCompleteDictionary, HashSet<String> stopwords);
	
	/**
	 * Method removes annotations for given ontology versions.
	 * 
	 * @param {@code List} of localOntologyIDs String containing ontology versions.
	 */
	public void removeAnnotations(List<String> localOntologyIDs);
}
