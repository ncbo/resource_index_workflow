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
	public void addResource(Resource toolResource);

	/**
	 * @param toolResource
	 */
	public void updateResourceForLatestDictionary(Resource toolResource);

	/**
	 * @param resourceFile
	 * @return
	 * @throws ResourceFileException 
	 * @throws BadElementStructureException 
	 */
	public int updateResourceContentFromFile(File resourceFile) throws BadElementStructureException, ResourceFileException;

	/**
	 *
	 * Returns a set of all the localElementIDs contained in the table. 
	 *
	 * @return Collection of local_element_id strings.
	 */
	public HashSet<String> getAllLocalElementIDs();
	
	/**
	 * Returns a set of all the values contained in the given column of table. 
	 */
	public  HashSet<String> getAllValuesByColumn(String columName);

	/**
	 * Returns the value of a given context for a given element in the table.
	 */
	public String getContextValueByContextName(String localElementID, String contextName);

		
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
	 * 
	 */
	public String mapTermsToVirtualLocalConceptIDs(HashSet<String> terms, String virtualOntologyID);
	
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
	 * This method gets concept for given term name for given local ontology id
	 * 
	 * @param localOntologyID
	 * @param termName
	 * @return
	 */
	public String getLocalConceptIdByPrefNameAndOntologyId(String localOntologyID, String termName);
	
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
	public int numberOfEntry();

	/**
	 * Remove all the entries from element table, annotation table, index table for
	 *  
	 */
	public void reInitializeAllTables();

	/**
	 * Remove all the entries from element table, annotation table, index table for
	 */
	public void reInitializeAllTablesExcept_ET();

	/**
	 * This method calculates number of indexed annotations, mgrep annotations, reported annotations, isa annotations, mapping annotations
	 * for current resource.
	 * 
	 */
	public void calculateObrStatistics();
	
}
