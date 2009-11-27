/**
 * 
 */
package org.ncbo.stanford.obr.service.resource;

import java.io.File;
import java.util.HashSet;

import org.ncbo.stanford.obr.exception.ResourceFileException;

import obs.obr.populate.Element;
import obs.obr.populate.Resource;
import obs.obr.populate.Element.BadElementStructureException;

/**
 * @author Kuladip Yadav
 *
 */
public interface ResourceUpdateService {
	
	/**
	 * 
	 * 
	 * @param toolResource
	 */
	void addResource(Resource toolResource);

	/**
	 * @param toolResource
	 */
	void updateResourceForLatestDictionary(Resource toolResource);

	/**
	 * @param resourceFile
	 * @return
	 * @throws ResourceFileException 
	 * @throws BadElementStructureException 
	 */
	int updateResourceContentFromFile(File resourceFile) throws BadElementStructureException, ResourceFileException;

	/**
	 *
	 * Returns a set of all the localElementIDs contained in the table. 
	 *
	 * @return Collection of local_element_id strings.
	 */
	HashSet<String> getAllLocalElementIDs();

	/**
	 * This method split terms string with splitString
	 * and get local concept id's using ontology access tool. 
	 * 
	 * @param terms
	 * @param localOntologyID
	 * @param splitString
	 * @return String containing local concept id's separated by '>' 
	 */
	public String mapTermsToLocalConceptIDs(String terms, String localOntologyID, String splitString);
	
	/**
	 * This method split terms string with splitString
	 * and get local concept id's using ontology access tool. 
	 * 
	 * @param terms
	 * @param virtualOntologyID
	 * @param splitString
	 * @return String containing local concept id's separated by '>' 
	 */
	public String mapTermsToVirtualLocalConceptIDs(String terms, String virtualOntologyID, String splitString);

	/**
	 * Adds new entry for given @code Element in element table.
	 * 
	 * @param element
	 * @return boolean - true if successfully added otherwise false.
	 */
	public boolean addElement(Element element);
	
	/**
	 * Gets the number of entries present in element table.
	 * 
	 * @return int
	 */
	int numberOfEntry();

	/**
	 * Remove all the entries from element table, annotation table, index table for
	 *  
	 */
	void reInitializeAllTables();

	/**
	 * Remove all the entries from element table, annotation table, index table for
	 */
	void reInitializeAllTablesExcept_ET();

	
}
