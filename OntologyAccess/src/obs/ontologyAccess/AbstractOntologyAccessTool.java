package obs.ontologyAccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

/**
 * This abstract class is used as a generic ontology and terminology access and manipulation tool.
 * It defines a set of methods that abstract on the following ontologies/terminologies API:
 * <ul>
 * 	<li> UMLS, available as a database;
 * 	<li> BioPortal, available through a REST web service API.
 * </ul>
 *  
 * A tool has a name and gives access to a set of ontologies/terminologies (after denoted ontologies only).<p>
 * This set of ontologies is implemented by means of an HashSet that contains the String used
 * to identify the ontologies in the different systems, called later <it>localOntologyID</it>:
 * <ul>
 * 	<li> SABs for UMLS (e.g., "SNOMEDCT", "MSHCZE");
 * 	<li> ontology URIs for BioPortal, called BPOntologyID (i.e., end of the ontology URI) (e.g., 38378).
 * </ul>
 * 
 * Note: some functions are restricted to the set of ontologies accessed by the tool, some function work for any 
 * ontology with a valid localOntologyID (e.g., getOntologyName).
 * 
 * A tool allows to manipulate concepts in a given system. Concepts are identified with the appropriated 
 * format for each system. The assumption taken by the OntologyAccessTool is that a concept ID, called later
 * <it> localConceptID </it>, is globally unique for a given system i.e., a concept is unique in a given ontology.
 * LocalConceptID format are: 
 * <ul>
 * 	<li> couple SAB/CUI for UMLS (e.g., NCIT/C0001175);
 * 	<li> concept URIs for BioPortal, called BPConceptID (i.e., the end of the concept URI) (e.g., 38378/DOID:1909)
 * </ul>
 *     
 * @author Clement Jonquet
 * @version 1.0
 * @created 21-Jul-2008
 */
public abstract class AbstractOntologyAccessTool implements OntologyAccess {

	// Logger for this class
	private static Logger logger = Logger
			.getLogger(AbstractOntologyAccessTool.class);
	protected HashSet<String> toolOntologies;
	private String toolName;
	
	public static final String ONTOLOGY_TOOL_PREFIX = "OBS_OATOOL_";
	
	// TODO: create objects for Paths,Ptr and Ptl and clean the mess with the function that use PTR/PTL
	
	/**
	 * Constructs a new AbstractOntologyAccessTool with an empty set of ontologies and a name.
	 */
	public AbstractOntologyAccessTool(String name){
		super();
		this.toolOntologies = new HashSet<String>();
		this.toolName = ONTOLOGY_TOOL_PREFIX + getRandomString(3) + "_" + name;
	}
	
	//************************************************ TOOL FUNCTIONS *********************************************
	
	/**
	 * Returns the set of ontologies accessed by the tool.
	 */
	public HashSet<String> getToolOntologies() {
		return toolOntologies;
	}
	
	/**
	 * Changes the set of Ontologies accessed by the tool.
	 */
	public void setToolOntologies(HashSet<String> toolOntologies) {
		this.toolOntologies = toolOntologies;
	}
	
	/**
	 * Returns the tool name.
	 */
	public String getToolName() {
		return toolName;
	}
	
	/**
	 * Adds the given ontologies to the set of ontologies accessed by the tool.
	 */
	public void addOntologies(String[] localOntologyIDs) throws NonValidLocalOntologyIDException {
		HashSet<String> localOntologyIDsSet = arrayToHashSet(localOntologyIDs);
		for(String localOntologyID: localOntologyIDsSet){
			this.addOntology(localOntologyID);
		}
	}
	
	/**
	 * Adds a given ontology to the set of ontologies accessed by the tool.
	 */
	public void addOntology(String localOntologyID) throws NonValidLocalOntologyIDException {
		if (this.isValidLocalOntologyID(localOntologyID)){
			if(this.toolOntologies.add(localOntologyID)){
				logger.info("Ontology <" + this.getOntologyName(localOntologyID) + "> ("+ localOntologyID +") added to the set of ontologies accessed by " + this.getToolName() + ".");
			}
			else {
				//System.out.println("Ontology <" + this.getOntologyName(localOntologyID) + "> already in the set of ontologies accessed by " + this.getToolName() + ".");
			}	
		}
		else {
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
	}
	
	/**
	 * Adds all the possible ontologies in the repository to the set of ontologies accessed by the tool.
	 */
	public abstract void addAllOntology();
	
	/**
	 * Removes a given ontology to the set of ontologies accessed by the tool.
	 */
	public void removeOntology(String localOntologyID) throws NonValidLocalOntologyIDException {
		if (this.isValidLocalOntologyID(localOntologyID)){
			if(this.toolOntologies.remove(localOntologyID)){
				logger.info("Ontology <" + this.getOntologyName(localOntologyID) + "> removed from the set of ontologies accessed by " + this.getToolName() + ".");
			}
			else {
				logger.info("Ontology <" + this.getOntologyName(localOntologyID) + "> was not in the set of ontologies accessed by " + this.getToolName() + ".");
			}
		}
		else {
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
	}
		
	/**
	 * Returns the number of ontologies accessed by the tool.
	 */
	public int getNumberOfOntologies(){
		return this.toolOntologies.size();
	}
	
	//******************************** CONCEPT FUNCTIONS ***************************************
						
	public String getConceptOntology(String localConceptID) throws NonValidLocalConceptIDException {
		String conceptOntology;
		if (this.isValidLocalConceptID(localConceptID)){
			String[] sarray = localConceptID.split("/");
			conceptOntology = sarray[0];
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return conceptOntology;
	}
	
	/**
	 * Returns the set of all localConceptID contains in a given ontology.
	 * Implements a traverse of the ontology using the getRootConcepts and the getConceptChildren functions.
	 * May be overrided for better performance.
	 */
	public HashSet<String> getLocalConceptIDs(String localOntologyID) throws NonValidLocalOntologyIDException{
		HashSet<String> localConceptIDs = new HashSet<String>();
		// computes the root concepts
		HashSet<String> rootConcepts = this.getOntologyRootConcepts(localOntologyID);
		// puts the root concepts in the main structure
		localConceptIDs.addAll(rootConcepts);
		// for each root concepts computes the children
		for (String rootConcept: rootConcepts){
			try {
				// puts the children in the main structure
				localConceptIDs.addAll(this.getConceptChildren(rootConcept).keySet());
			} catch (NonValidLocalConceptIDException e) {
				// nothing - Should not happen because we already check the content of toolOntologies when creating
				logger.error("This should not happen - getLocalConceptIDs(String localOntologyID).");
			}
		}
		return localConceptIDs;
	}

	/**	
	 * Returns the set of all localConceptIDs in all the ontologies accessed by the tool.
	 * May be overrided for better performance.
	 */
	public HashSet<String> getAllLocalConceptIDs(){
		HashSet<String> allLocalConceptIDs = new HashSet<String>();
		for (String ontology: this.toolOntologies){
			try{
				allLocalConceptIDs.addAll(this.getLocalConceptIDs(ontology));
			}
			catch (NonValidLocalOntologyIDException e) {
				// nothing - Should not happen because we already check the content of toolOntologies when creating
				logger.error("This should not happen - getAllLocalConceptIDs().");
			}
		}
		return allLocalConceptIDs;
	} 
	
	/**
	 * For a given String, returns the set of concepts in all the ontologies accessed by the tool
	 * for which the given String is
	 * (i) exactly equal to one of the concept names if the flag is true;
	 * (ii) contained into one of the concept names if the flag is false;
	 * May be overrided for better performance.
	 */
	public HashSet<String> mapStringToLocalConceptIDs(String s, boolean exactMap){
		HashSet<String> localConceptIDs = new HashSet<String>();
		for (String ontology: this.toolOntologies){
			try{
				localConceptIDs.addAll(this.mapStringToLocalConceptIDs(s, ontology, exactMap));
			}
			catch (NonValidLocalOntologyIDException e) {
				// nothing - Should not happen because we already check the content of toolOntologies when creating
				logger.error("This should not happen - mapToLocalConceptIDs(String s).");
			}
		}
		return localConceptIDs;			
	}
	
	protected boolean isValidLocalConceptIDWithRegExp(String localConceptID, String regexp){
		boolean valid = false;
		if (localConceptID.matches(regexp)){
			String[] sarray = localConceptID.split("/");
			String conceptOntology = sarray[0];
			if (this.isValidLocalOntologyID(conceptOntology)){
				valid = true;
			}	
		}
		return valid;
	}
	
	/**
	 * Returns a String that contains all the information about a concept (GUI function).
	 */
	public String conceptInformation(String localConceptID, boolean withAllParents, boolean withAllChildren) throws NonValidLocalConceptIDException {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("Information for localConceptID: ");
		buffer.append(localConceptID);
		buffer.append("\n\tOntology: ");
		buffer.append(this.getConceptOntology(localConceptID));
		buffer.append("\n\tPreferred name: ");
		buffer.append(this.getConceptPreferredName(localConceptID));
		HashSet<String> synonyms = this.getConceptSynonyms(localConceptID);
		buffer.append("\n\tSynonyms (");
		buffer.append(synonyms.size());
		buffer.append("): ");
		buffer.append(synonyms);
		
		// direct parents
		HashSet<String> directParents = this.getConceptDirectParents(localConceptID);
		buffer.append("\n\tDirect parents (");
		buffer.append(directParents.size());
		buffer.append("): ");
		buffer.append(directParents);
		directParents = null;
		
		if (withAllParents) {
		// all parents
			Hashtable<String, Integer> parents2 = this.getConceptParents(localConceptID);
			buffer.append("\n\tParents (");
			buffer.append(parents2.size());
			buffer.append("): ");
			buffer.append(parents2);
			parents2 = null;
		}

		// direct children
		HashSet<String> directChildren = this.getConceptDirectChildren(localConceptID);
		buffer.append("\n\tDirect children (");
		buffer.append(directChildren.size());
		buffer.append("): ");
		buffer.append(directChildren);
		directChildren = null;
		
		if (withAllChildren) {
			// all children
			Hashtable<String, Integer> children = this.getConceptChildren(localConceptID);
			buffer.append("\n\tChildren (");
			buffer.append(children.size());
			buffer.append("): ");
			buffer.append(children);
			children = null;
		}
		buffer.append("\n");
		return buffer.toString();
	}


	// ************************************* MAPPING FUNCTIONS *****************************
	
	/**
	 * Returns a set of couples [mappedLocalConceptID, mappingType] saying that the given concept is  
	 * mapped to (according to mappingType) mappedLocalConceptID in all the ontologies accessed by the tool
	 * ((including the one in which the given concept is defined (internal mappings)).
	 */
	public abstract HashSet<String[]> getAllConceptMappings(String localConceptID) throws NonValidLocalConceptIDException;
	
	/**
	 * Returns a set of triplets [localConceptID, mappedLocalConceptID, mappingType]
	 * saying that the localConceptID is mapped to (according to mappingType) mappedLocalConceptID
	 * localConceptID is in the first given ontology; mappedLocalConceptID is in one ontology
	 * accessed by the tool (including the one given (internal mappings)).
	 */
	public abstract HashSet<String[]> getAllOntologyMappings(String localOntologyID) throws NonValidLocalOntologyIDException;
		
		
	// ************************************* IS-A HIERARCHY FUNCTIONS *****************************
					
	/**
	 * For a given concept, returns a hashtable that represents the set of concepts of all the parents (recursively to the ontology root).
	 * The hashtable keys are the localConceptID and the values are the level in the hierarchy (parents=1, grand-parents=2, ...).
	 * If there are several path to the root with different levels, we keep the shortest level for a concept.
	 * The current implementation implements a breadth first search algorithm.
	 * May be overrided for better performance.
	 * Cycles are removed, otherwise the recursion loops.
	 */
	public Hashtable<String, Integer> getConceptParents(String localConceptID) throws NonValidLocalConceptIDException{
		Hashtable<String, Integer> conceptParentsTable = new Hashtable<String, Integer>();
		// we use an arraylist in order to be able to use ListIterator that can be dynamically changed
		ArrayList<String> conceptParentsList = new ArrayList<String>();
		// we start with the concept to process in the list but not in the table
		conceptParentsList.add(localConceptID);
		
		// used to temporary store the parents from one level and add them to the iterator when incrementing the level
		ArrayList<String> tempConceptParentsList = new ArrayList<String>();
		String currentConcept;
		int level = 1;
		Integer currentParentLevel;
		HashSet<String> currentConceptParents = new HashSet<String>(); 
		// for each concept in the list  
		ListIterator<String> it = conceptParentsList.listIterator();
		while(it.hasNext()){
			currentConcept = it.next();
			// removes the currentConcept from the main list
			it.remove();
			// compute the parents of the current concept
			currentConceptParents = this.getConceptDirectParents(currentConcept);
			// for each parents of the currentConcept
			for (String currentParent: currentConceptParents){
				// if the currentParent is not in the table
				if (!conceptParentsTable.containsKey(currentParent)){
					// adds the currentParent to the temporary list of parents (to be added later)
					// if the currentParent is not the localConceptID itself (cycle in direct parents are possible)
					if(!localConceptID.equals(currentParent)){
						tempConceptParentsList.add(currentParent);
						conceptParentsTable.put(currentParent, level);
					}
				}
				else{
					// Note: this else does not exclude the situation where the currentParent=currentConcept
					currentParentLevel = conceptParentsTable.get(currentParent);
					if (currentParentLevel > (level+1)){
						// changes the level in the table if shorter
						conceptParentsTable.put(currentParent, level+1);
					}
				}
			}
			// if there is no more concept at that level to process
			if(conceptParentsList.isEmpty()){
				// increments the level
				level++;
				// adds all the parents to the main list
				for (String currentParent: tempConceptParentsList){
					it.add(currentParent);
				}
				tempConceptParentsList.clear();
				// refreshes the listIterator 
				it = conceptParentsList.listIterator();
			}
		}
		return conceptParentsTable;
	}
	
	/**
	 * For a given concept, returns a hashtable that represents the set of concepts of all the children (recursively to the leaves).
	 * The hashtable keys are the localConceptID and the values are the level in the hierarchy (child=1, grand-child=2, ...).
	 * If there are several path to the same leave with different levels, we keep the shortest level for a concept.
	 * The current implementation implements a breadth first search algorithm.
	 * May be overrided for better performance.
	 */
	public Hashtable<String, Integer> getConceptChildren(String localConceptID) throws NonValidLocalConceptIDException{
		Hashtable<String, Integer> conceptChildrenTable = new Hashtable<String, Integer>();
		// we use an arraylist in order to be able to use ListIterator that can be dynamically changed
		ArrayList<String> conceptChildrenList = new ArrayList<String>();
		// we start with the concept to process in the list but not in the table
		conceptChildrenList.add(localConceptID);
		
		// used to temporary store the children from one level and add them to the iterator when incrementing the level
		ArrayList<String> tempConceptChildrenList = new ArrayList<String>();
		String currentConcept;
		int level = 1;
		Integer currentChildLevel;
		HashSet<String> currentConceptChildren = new HashSet<String>(); 
		// for each concept in the list  
		ListIterator<String> it = conceptChildrenList.listIterator();
		while(it.hasNext()){
			currentConcept = it.next();
			// removes the currentConcept from the main list
			it.remove();
			// compute the children of the current concept
			currentConceptChildren = this.getConceptDirectChildren(currentConcept);
			// for each children of the currentConcept
			for (String currentChild: currentConceptChildren){
				// if the currentChild is not in the table
				if (!conceptChildrenTable.containsKey(currentChild)){
					// adds the currentChild to the temporary list of children (to be added later)
					tempConceptChildrenList.add(currentChild);
					conceptChildrenTable.put(currentChild, level);
				}
				else{
					// Note: this else does not exclude the situation where the currentChild=currentConcept
					currentChildLevel = conceptChildrenTable.get(currentChild);
					if (currentChildLevel > (level+1)){
						// changes the level in the table if shorter
						conceptChildrenTable.put(currentChild, level+1);
					}
				}
			}
			// if there is no more concept at that level to process
			if(conceptChildrenList.isEmpty()){
				// increments the level
				level++;
				// adds all the children to the main list
				for (String currentChild: tempConceptChildrenList){
					it.add(currentChild);
				}
				tempConceptChildrenList.clear();
				// refreshes the listIterator 
				it = conceptChildrenList.listIterator();
			}
		}
		return conceptChildrenTable;
	}
	/* old version.... 
	 This version is processing some grad*-children before the all the children which create a bug in the level
	 (if a concept has already been processed, I can update its level if shorter, but not hte level of its children!)
	 I hvae checked and changed the function implemented like that elsewhere 
	public Hashtable<String, Integer> getConceptChildren(String localConceptID) throws NonValidLocalConceptIDException{
		Hashtable<String, Integer> conceptChildrenTable = new Hashtable<String, Integer>();
		// we use an arraylist in order to be able to use ListIterator that can be dynamically changed
		ArrayList<String> conceptChildrenList = new ArrayList<String>(this.getConceptDirectChildren(localConceptID));
		// fills the table with the first level children
		for(String conceptChild: conceptChildrenList){
			conceptChildrenTable.put(conceptChild, new Integer(1));
		}
		String currentConcept;
		int index = 0;
		int currentChildLevel;
		HashSet<String> currentConceptChildren = new HashSet<String>(); 
		// for each children in the list  
		ListIterator<String> it = conceptChildrenList.listIterator();
		while(it.hasNext()){
			currentConcept = it.next();
			// compute the children of the child
			currentConceptChildren = this.getConceptDirectChildren(currentConcept);
			index++;
			// for each children of the child (currentConcept)
			for (String currentChild: currentConceptChildren){
				// if the currentChild is not in the list
				if (!conceptChildrenList.contains(currentChild)){
					// adds the child of the child to the main list iterator if not present
					it.add(currentChild);
					// adds the child to the table with a level incremented
					conceptChildrenTable.put(currentChild, conceptChildrenTable.get(currentConcept)+1);
				}
				// if the currentChild is already in the list
				else{
					// gets the current level for the currentChild in the table
					currentChildLevel = conceptChildrenTable.get(currentChild);
					if (currentChildLevel > (conceptChildrenTable.get(currentConcept)+1)){
						// changes the level in the table if shorter
						conceptChildrenTable.put(currentChild, conceptChildrenTable.get(currentConcept)+1);
					}
				}
			}
			// updates the listIterator
			it = conceptChildrenList.listIterator(index);
		}
		return conceptChildrenTable;
	}*/
	
	/**
	 * Returns a set of path to the root (PTRs) for a given concept.
	 * PTRs are represented as a list of localConceptIDs, separated by periods (.)
	 * The first one in the list is top of the hierarchy;
	 * the last one in the list is the direct parent of the concept.
	 */
	public abstract HashSet<String> getConceptPtrs(String localConceptID) throws NonValidLocalConceptIDException;
	
	// ******************************* DISPLAY FUNCTIONS *****************************************

	public String toString(){
		String[] toolString = new String[5]; 
		toolString[0] = "----------------------------------";
		toolString[1] = "Object OntologyAccessTool:";
		toolString[2] = "Tool name: " + this.toolName;
		toolString[3] = "Number of ontologies accessed: " + this.getNumberOfOntologies();
		toolString[4] = "----------------------------------";
		return arrayToString(toolString, "\n");
	}
	
	/**
	 * Returns a string which contains the given PRT in a pretty print form. 
	 */
	public String displayPtr(String localConceptID, String ptr) throws NonValidLocalConceptIDException{
		StringBuffer buffer = new StringBuffer();
		ArrayList<String> ptrList;
		int nbTab = 0;
		// split the PTR to a list
		ptrList = ptXToList(ptr); 
		for(String parentConcept: ptrList){
			nbTab++;
			buffer.append("|___").append(this.conceptStr(parentConcept)).append("\n");
			for (int i=0; i<nbTab; i++){
				buffer.append("   ");
			}
		}
		buffer.append("|___").append(this.conceptStr(localConceptID)).append("\n\n");
		return buffer.toString();
	}
	
	/**
	 * Returns a string which contains the given PRT in a pretty print form. 
	 * If a concept from the given localConceptIDs set is in the given ptr, them it is highlighted.
	 */
	public String displayPtr(String localConceptID, String ptr, Set<String> localConceptIDs) throws NonValidLocalConceptIDException{
		StringBuffer buffer = new StringBuffer();
		ArrayList<String> ptrList;
		int nbTab = 0;
		// split the PTR to a list
		ptrList = ptXToList(ptr); 
		for(String parentConcept: ptrList){
			nbTab++;
			buffer.append("|___").append(this.conceptStr(parentConcept));
			if(localConceptIDs.contains(parentConcept)){
				buffer.append("**");
			}
			buffer.append("\n");
			for (int i=0; i<nbTab; i++){
				buffer.append("   ");
			}
		}
		buffer.append("|___").append(this.conceptStr(localConceptID)).append("\n\n");
		return buffer.toString();
	}
	
	/**
	 * Returns a string which contains the hierarchy for a given concept based on the path-to-the-root
	 * information, in a pretty form. 
	 */
	public String displayConceptHierarchy(String localConceptID) throws NonValidLocalConceptIDException{
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.getConceptOntology(localConceptID)).append("\n");
		// gets the PTR from OBS DB
		HashSet<String> ptrs = this.getConceptPtrs(localConceptID);
		// for each PTR
		for(String ptr: ptrs){
			buffer.append(this.displayPtr(localConceptID, ptr));
		} 
		return buffer.toString();
	}
	
	public String conceptStr(String localConceptID) throws NonValidLocalConceptIDException{
		return localConceptID + " [" + this.getConceptPreferredName(localConceptID) + "]"; 
	}
	
	/**
	 * Returns a String ptX (PTR or PTL) as a list.  
	 */
	public static ArrayList<String> ptXToList(String ptX){
		// This function needs to take care of localConceptIDs that contains "."
		ArrayList<String> ptrList = new ArrayList<String>();
		// extracts the string (localOntologyID + .) to use to split the PTR
		String localOntologyID = ptX.substring(0, ptX.indexOf("/"));
		// splits the given PTR to an array of localConceptIDs
		String[] concepts = ptX.split("\\."+localOntologyID);
		// adds the first concept in the ptr without concatenating the localOntologyID
		ptrList.add(concepts[0]);
		// for each concepts in the PTR
		for (int i=1; i<concepts.length; i++){
			String splitpart = concepts[i];
			ptrList.add(localOntologyID + splitpart);
		}
		return ptrList;
	}
	

	/**
	 * Returns a list ptX (PTR or PTL) as a String.  
	 */
	public static String ptXToString(List<String> ptX){
		StringBuffer ptXAsString = new StringBuffer(ptX.size());
		String concept;
		// traverses the given ptX to
		for(Iterator<String> it = ptX.iterator(); it.hasNext();){
			concept = it.next();
			ptXAsString.append(concept);
			if(it.hasNext()){
				ptXAsString.append(".");
			}
		}
		if(ptXAsString.length()>0){
			return ptXAsString.toString();	
		}
		else{
			return "";
		}
	}
	
	/**
	 * Concatenate 2 ptXs as String. 
	 */
	public static String concatPtX(String localConceptIDorPtX, String ptX){
		if (localConceptIDorPtX!="")
			return localConceptIDorPtX.concat("." + ptX);
		else{
			return ptX;
		}
	}
	
	//************************************* UTILS ********************************************
	
	private static String getRandomString(int length) {
		UUID uuid = UUID.randomUUID();
		String myRandom = uuid.toString();
		return myRandom.substring(0,length);
	}
	
	/**
	 * Convert an array of strings to one string.
	 * Put the 'separator' string between each element. 
	 */
	private static String arrayToString(String[] a, String separator) {
	    StringBuffer result = new StringBuffer();
	    if (a.length > 0) {
	        result.append(a[0]);
	        for (int i=1; i<a.length; i++) {
	            result.append(separator);
	            result.append(a[i]);
	        }
	    }
	    return result.toString();
	}
	
	/**
	 * Returns a HashSet<String> for a given String[].
	 */
	protected static HashSet<String> arrayToHashSet(String[] array){
		HashSet<String> set = new HashSet<String>();
		if(array!=null){
			for (int i=0; i<array.length; i++){
				set.add(array[i]);
			}
		}
		return set;
	}
		
}
