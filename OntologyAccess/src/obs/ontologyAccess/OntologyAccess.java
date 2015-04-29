package obs.ontologyAccess;

import java.util.HashSet;
import java.util.Hashtable;

/**
 * This interface defines the function that are necessary to access ontologies.
 * 
 * @author Clement Jonquet
 * @version 1.0
 * @created 21-Jul-2008
 */
public interface OntologyAccess {

	/**
	 * Returns true if the given String is a valid localOntologyID. 
	 */
	public boolean isValidLocalOntologyID(String localOntologyID);
	
	/**
	 * Returns true if the given String is a valid localConceptID. 
	 */
	public boolean isValidLocalConceptID(String localConceptID);
	

	//*************************** ONTOLOGY FUNCTIONS ****************************************
	
	/**
	 * Returns the ontology name as defined in the corresponding system for a given ontology. 
	 */
	public String getOntologyName(String localOntologyID) throws NonValidLocalOntologyIDException;
	
	/**
	 * Returns the ontology description as defined in the corresponding system for a given ontology. 
	 */
	public String getOntologyDescription(String localOntologyID) throws NonValidLocalOntologyIDException;
	
	/**
	 * Returns the ontology version as defined in the corresponding system for a given ontology. 
	 */
	public String getOntologyVersion(String localOntologyID) throws NonValidLocalOntologyIDException;

	/**
	 * Returns the a set of the ontology roots (top level concepts, i.e., concepts without parents).
	 */
	public HashSet<String> getOntologyRootConcepts(String localOntologyID) throws NonValidLocalOntologyIDException;
	

	//*************************************** CONCEPT FUNCTIONS ***************************************
	
	/**
	 * For a given concept, returns the concept preferred name. 
	 */
	public String getConceptPreferredName(String localConceptID) throws NonValidLocalConceptIDException;

	/**
	 * For a given concept, returns a set of String, synonym names of the concept.
	 */
	public HashSet<String> getConceptSynonyms(String localConceptID) throws NonValidLocalConceptIDException;
	
	/**
	 * For a given concept, returns the ontology in which the concept is defined.
	 */
	public String getConceptOntology(String localConceptID) throws NonValidLocalConceptIDException;
	
	/**
	 * For a given concept, returns the conceptID part of the localConceptID.
	 */
	public String getConceptConceptIDPart(String localConceptID) throws NonValidLocalConceptIDException;
	
	/**
	 * Returns the set of all localConceptID contains in a given ontology.
	 */
	public HashSet<String> getLocalConceptIDs(String localOntologyID) throws NonValidLocalOntologyIDException;
		
	/**
	 * For a given String, returns the set of concepts in the given ontology for which the given String is
	 * (i) exactly equal to one of the concept names if the flag is true;
	 * (ii) contained into one of the concept names if the flag is false;
	 */
	public abstract HashSet<String> mapStringToLocalConceptIDs(String s, String localOntologyID, boolean exactMap) throws NonValidLocalOntologyIDException;
	
	/**
	 * Returns true if the given concept is a root concept in its ontology.
	 */
	public abstract boolean isRootConcept(String localConceptID) throws NonValidLocalConceptIDException;
	
	/**
	 * Returns a String that contains all the information about a concept (GUI function).
	 * The 2 flags specify respectively if all the parents and all the children are computed.
	 */
	public String conceptInformation(String localConceptID, boolean withAllParents, boolean withAllChildren) throws NonValidLocalConceptIDException;
	
	//************************************* MAPPING FUNCTIONS *****************************

	/**
	 * Returns a set of couples [mappedLocalConceptID, mappingType] saying that the given concept is  
	 * mapped to (according to mappingType) mappedLocalConceptID in the given ontology.
	 * The given ontology can be the one in which the given concept is defined (internal mappings).
	 */
	public HashSet<String[]> getConceptMappings(String localConceptID, String localOntologyID) throws NonValidLocalConceptIDException, NonValidLocalOntologyIDException;	
	
	/**
	 * Returns a set of triplets [localConceptID, mappedLocalConceptID, mappingType]
	 * saying that the localConceptID is mapped to (according to mappingType) mappedLocalConceptID
	 * localConceptID is in the first given ontology; mappedLocalConceptID is in the second.
	 * The 2 given ontologies can be the same (internal mappings).
	 * Note: mappings are not assumed to be symetric. Therefore, a call to getOntologyMappings(o1, o2)
	 * may returned different result than getOntologyMappings(o2, o1).
	 */
	public HashSet<String[]> getOntologyMappings(String localOntologyID1, String localOntologyID2) throws NonValidLocalOntologyIDException;
	
	//*************************************IS-A HIERARCHY FUNCTIONS *****************************
	
	/**
	 * For a given concept, returns the set of concept of all the direct parents.
	 */
	public abstract HashSet<String> getConceptDirectParents(String localConceptID) throws NonValidLocalConceptIDException;
	
	/**
	 * For a given concept, returns the set of concept of all the direct children.
	 */
	public abstract HashSet<String> getConceptDirectChildren(String localConceptID) throws NonValidLocalConceptIDException;

	/**
	 * For a given concept, returns a hashtable that represents the set of concepts of all the parents (recursively to the ontology root).
	 * The hashtable keys are the localConceptID and the values are the level in the hierarchy (parents=1, grand-parents=2, ...).
	 * If there are several paths to the root with different levels, we keep the shortest level for a concept.
	 */
	public Hashtable<String, Integer> getConceptParents(String localConceptID) throws NonValidLocalConceptIDException;
	
	/**
	 * For a given concept, returns a hashtable that represents the set of concepts of all the children (recursively to the leaves).
	 * The hashtable keys are the localConceptID and the values are the level in the hierarchy (child=1, grand-child=2, ...).
	 * If there are several path to the same leave with different levels, we keep the shortest level for a concept.
	 */
	public Hashtable<String, Integer> getConceptChildren(String localConceptID) throws NonValidLocalConceptIDException;
	
	//************************************* EXCEPTIONS *******************************************
	
	public class NonValidLocalOntologyIDException extends Exception {
		private static final long serialVersionUID = 1L;
		public NonValidLocalOntologyIDException(String nonValidLocalOntologyID){
			super("The given localOntologyID is not valid: " + nonValidLocalOntologyID);
		}
	}
	
	public class NonValidLocalConceptIDException extends Exception {
		private static final long serialVersionUID = 1L;
		public NonValidLocalConceptIDException(String nonValidLocalConceptID){
			super("The given localConceptID is not valid: " + nonValidLocalConceptID);
		}
	}
	
	public class NonValidVirtualOntologyIDException extends Exception {
		private static final long serialVersionUID = 1L;
		public NonValidVirtualOntologyIDException(String nonValidVirtualOntologyID){
			super("The given virtual ontology identifier is not valid: " + nonValidVirtualOntologyID);
		}
	}
}
