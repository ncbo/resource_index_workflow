/**
 * 
 */
package org.ncbo.stanford.obr.service.obs.ontology;

/**
 * @author Kuladip Yadav
 *
 */
public interface OntologyService {
	
	/**
	 * This method gets latest version of ontology for given virtual ontology id
	 * 
	 * @param virtualOntologyID 
	 * @return String of latest version of ontology.
	 */
	public String getLatestLocalOntologyID(String virtualOntologyID);

}
