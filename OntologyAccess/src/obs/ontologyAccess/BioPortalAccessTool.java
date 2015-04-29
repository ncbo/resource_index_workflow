package obs.ontologyAccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import obs.ontologyAccess.bean.BioPortalFullConceptBean;
import obs.ontologyAccess.bean.BioPortalFullOntologyBean;
import obs.ontologyAccess.bean.BioPortalLightConceptBean;
import obs.ontologyAccess.bean.BioPortalLightOntologyBean;
import obs.ontologyAccess.bean.LightConceptBeanList;
import obs.ontologyAccess.bean.LightOntologyBeanList;
import obs.ontologyAccess.exception.BioPortalResponseException;
import obs.ontologyAccess.util.ApplicationConstants;
import obs.ontologyAccess.util.BioPortalAccessUtil;

/**
 * This class is used to access BioPortal ontologies. 
 * This tool works with BioPortal 2.0 release. Other versions have not been tested.
 *  
 * See also {@link OntologyAccess}, {@link AbstractOntologyAccessTool}.
 *  
 * ParametersBean to connect to BioPortal REST services must be an URL such as:
 * "http://ncbo-core-stage1:8080/bioportal/rest/"   
 *     
 * @author Clement Jonquet
 * @version 1.0
 * @created 27-Oct-2008
 */
public class BioPortalAccessTool extends AbstractOntologyAccessTool {
	
	// Logger for this class
	private static Logger logger = Logger
			.getLogger(BioPortalAccessTool.class);
	
	/**
	 * Constructs a tool to access BioPortal REST services.
	 * The new BioPortalAccessTool is constructed with an empty set of ontologies and a randomly generated name.
	 */
	public BioPortalAccessTool(String name){
		super("BIOPORTAL_" + name);
		logger.info("BioPortalAccessTool creation...");
		logger.info("BioPortaAccessTool " + this.getToolName() + " created.\n");
	}

	/**
	 * Constructs a tool to access BioPortal REST services and
	 * an array of ontologies (i.e., BioPortal URIs) and a customized generated name.
	 */
	public BioPortalAccessTool(String[] localOntologyIDs, String name) throws NonValidLocalOntologyIDException {
		this(name);
		// adds the ontologies
		this.addOntologies(localOntologyIDs);
	}
	
	public void addAllOntology(){
		this.addAllBioPortalOntologies();
	}
	
	/**
	 * Adds all the latest version of the BioPortal ontologies to the tool.
	 * The previous versions are not added to the tool. 
	 */
	public void addAllBioPortalOntologies(){
		// for each ontology in BioPortal 
		for(BioPortalLightOntologyBean ontology: this.getBioPortalOntologies().values()){
			try{
				// adds the localOntologyID to the set of of ontologies accessd by the tool 
				this.addOntology(ontology.getId().toString());
			}
			catch (NonValidLocalOntologyIDException e) {
				// nothing - Should not happen because we get the ontologyBeans directly from BioPortal
				logger.error("This should not happen - addAllUMLSOntologies()."); 
			}
		}
	}
	
	private Hashtable<String, BioPortalLightOntologyBean> getBioPortalOntologies(){
		LightOntologyBeanList ontologyBeans = new LightOntologyBeanList(new ArrayList<BioPortalLightOntologyBean>());
		try {
			ontologyBeans = BioPortalAccessUtil.getOntologies();
		}
		catch (BioPortalResponseException e) {
			logger.error(e.toString());
			logger.error("** PROBLEM: Cannot get list of all ontologies from BioPortal. Empty list returned.");
		}
		catch (Exception e) {
			logger.error("** PROBLEM: Cannot get list of all ontologies from BioPortal. Empty list returned.");
		}
		Hashtable<String, BioPortalLightOntologyBean> oBTable = new Hashtable<String, BioPortalLightOntologyBean>(ontologyBeans.getOntologies().size());
		// populates a table of all the ontology beans
		for (BioPortalLightOntologyBean ontologyBean : ontologyBeans.getOntologies()) {
			if(ontologyBean.getStatusId()== ApplicationConstants.STATUS_ID){
				oBTable.put(ontologyBean.getId().toString(), ontologyBean);
			}
		}
		return oBTable;
	}

	// **************************** CONCEPT FUNCTIONS ************************************************
	
	public String getConceptConceptIDPart(String localConceptID) throws NonValidLocalConceptIDException{
		return getConceptBpConceptID(localConceptID);
	}
	
	/**
	 * Returns the bpConceptID part of the localConceptID i.e., the end of the concept URI e.g., DOID:1909
	 * These IDs are not unique in BioPortal.
	 */
	public String getConceptBpConceptID(String localConceptID) throws NonValidLocalConceptIDException {
		String bpConceptID;
		if (this.isValidLocalConceptID(localConceptID)){
			String[] sarray = localConceptID.split("/");
			// Custom code to deal with bpConceptID that are URLs
			// e.g., 28837/http://www.owl-ontologies.com/GeographicalRegion.owl#Geographical_Regions
			if(sarray.length>2){
				bpConceptID = "";
				for (int i=1; i<sarray.length; i++){
					bpConceptID += sarray[i];
					if(i<sarray.length-1) bpConceptID += "/";
				}
			}
			else{
				bpConceptID = sarray[1];
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return bpConceptID;
	}

	/**
	 * Returns a concept bean for the given localConceptID. 
	 */
	public BioPortalFullConceptBean getBioPortalConceptBean(String localConceptID) throws NonValidLocalConceptIDException {
		BioPortalFullConceptBean concept;
		if (this.isValidLocalConceptID(localConceptID)){
			try {
				concept = BioPortalAccessUtil.getConcept(this.getConceptOntology(localConceptID), this.getConceptBpConceptID(localConceptID));
			}
			catch (BioPortalResponseException e) {
				logger.error(e.toString());
				logger.error("** PROBLEM: Cannot get concept bean from BioPortal for localConceptID: "+ localConceptID+". Null returned.");
				concept = null;
			}
			catch (Exception e) {
				logger.error("** PROBLEM: Cannot get concept bean from BioPortal for localConceptID: "+ localConceptID+". Null returned.", e);
				concept = null;
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return concept;
	}
	
	protected boolean checkConceptExistence(String localConceptID){
		BioPortalFullConceptBean bpConcept = null;
		try {
			bpConcept = this.getBioPortalConceptBean(localConceptID);
		}
		catch(Exception e){
			logger.error("The localConceptID does not really exist: "+localConceptID);
		}
		if (bpConcept != null){
			return true;	
		}
		else{
			logger.info("The localConceptID does not really exist: "+localConceptID);
			return false;
		}
	}
	
	/**
	 * For a given concept, returns the preferred name of the concept.
	 * Returns the label field of the corresponding concept bean.
	 */
	public String getConceptPreferredName(String localConceptID) throws NonValidLocalConceptIDException {
		BioPortalFullConceptBean concept = this.getBioPortalConceptBean(localConceptID);
		if (concept==null){
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		else{
			return concept.getLabel();
		}
	}
	
	/**
	 * For a given concept, returns the virtual ontology id of the concept.
	 */
	public String getConceptVirtualOntology(String localConceptID) throws NonValidLocalConceptIDException, NonValidLocalOntologyIDException {
		if (this.isValidLocalConceptID(localConceptID)){
			BioPortalFullOntologyBean ontology = this.getBioPortalOntologyBean(this.getConceptOntology(localConceptID));
			return ontology.getOntologyId().toString();
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
	}
	
	/**
	 * For a given concept, returns a set of String, synonym names of the concept.
	 * Returns the union of the synonyms fields of the concept bean. Right now synonyms will use only EXACT SYNONYM, 
	 * NARROW SYNONYM, BROAD SYNONYM, RELATED SYNONYM and BP_SYNONYM fields.
	 */
	public HashSet<String> getConceptSynonyms(String localConceptID) throws NonValidLocalConceptIDException {
		HashSet<String> synonyms = new HashSet<String>();
		BioPortalFullConceptBean concept = this.getBioPortalConceptBean(localConceptID);
		if (concept==null){
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		else{
			synonyms.addAll(concept.getExactSynonyms());
			synonyms.addAll(concept.getNarrowSynonyms());
			synonyms.addAll(concept.getBroadSynonyms());
			synonyms.addAll(concept.getRelatedSynonyms());
			synonyms.addAll(concept.getBpSynonyms());
			return synonyms;
		}
	}
	
	/**
	 * Returns true if the given concept is a root (top level concept) in this ontology.
	 * Returns true if the concept is in the result of getRootConcepts. 
	 */
	public boolean isRootConcept(String localConceptID) throws NonValidLocalConceptIDException {
		boolean isRoot = false;
		try {
			isRoot = this.getOntologyRootConcepts(this.getConceptOntology(localConceptID)).contains(localConceptID);
		} catch (NonValidLocalOntologyIDException e) {
			// nothing - Should not happen because we get the root concepts directly from BioPortal
			logger.error("This should not happen - isRootConcept.");
		}
		return isRoot;
	}
	
	public String conceptInformation(String localConceptID) throws NonValidLocalConceptIDException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(super.conceptInformation(localConceptID, false, false));
		buffer.append("\n\tBPConceptID: ");
		buffer.append(this.getConceptBpConceptID(localConceptID));
		buffer.append("\n");
		return buffer.toString();
	}
	
	// ************************************* MAPPING FUNCTIONS *****************************

	/**
	 * Not available in this version. Needs BioPortal to provide mapping services.
	 * Returns empty set.
	 */
	public HashSet<String[]> getConceptMappings(String localConceptID, String localOntologyID) throws NonValidLocalConceptIDException, NonValidLocalOntologyIDException{
		HashSet<String[]> mappedConcepts = new HashSet<String[]>();
		if (this.isValidLocalConceptID(localConceptID)){
			if (this.isValidLocalOntologyID(localOntologyID)){
				// nothing for now
			}
			else{
				throw new NonValidLocalOntologyIDException(localOntologyID);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return mappedConcepts;
	}	

	/**
	 * Not available in this version. Needs BioPortal to provide mapping services.
	 * Returns empty set.
	 */
	public HashSet<String[]> getAllConceptMappings(String localConceptID) throws NonValidLocalConceptIDException {
		HashSet<String[]> mappedConcepts = new HashSet<String[]>();
		if (this.isValidLocalConceptID(localConceptID)){
			// nothing for now
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
	return mappedConcepts;
	}	
	
	/**
	 * Not available in this version. Needs BioPortal to provide mapping services.
	 * Returns empty set.
	 */
	public HashSet<String[]> getOntologyMappings(String localOntologyID1, String localOntologyID2) throws NonValidLocalOntologyIDException {
		HashSet<String[]> mappedConcepts = new HashSet<String[]>();
		if (this.isValidLocalOntologyID(localOntologyID1)){
			if (this.isValidLocalOntologyID(localOntologyID2)){
				// nothing for now
			}
			else{
				throw new NonValidLocalOntologyIDException(localOntologyID2);
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID1);
		}
		return mappedConcepts;
	}
	
	/**
	 * Not available in this version. Needs BioPortal to provide mapping services.
	 * Returns empty set.
	 */
	public HashSet<String[]> getAllOntologyMappings(String localOntologyID) throws NonValidLocalOntologyIDException {
		HashSet<String[]> mappedConcepts = new HashSet<String[]>();
		if (this.isValidLocalOntologyID(localOntologyID)){
			// nothing for now
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
	return mappedConcepts;
}
		
	// ******************************** ONTOLOGY FUNCTIONS ******************************************
	
	/**
	 * Returns an ontology bean for the given localOntologyID. 
	 */
	public BioPortalFullOntologyBean getBioPortalOntologyBean(String localOntologyID) throws NonValidLocalOntologyIDException {
		BioPortalFullOntologyBean ontology;
		if (this.isValidLocalOntologyID(localOntologyID)){
			try {
				ontology = BioPortalAccessUtil.getOntology(localOntologyID);
			}
			catch (BioPortalResponseException e) {
				logger.error(e.toString());
				logger.error("** PROBLEM: Cannot get ontology bean from BioPortal for localOntologyID: "+ localOntologyID+". Null returned.");
				ontology = null;
			}
			catch (Exception e) {
				logger.error("** PROBLEM: Cannot get ontology bean from BioPortal for localOntologyID: "+ localOntologyID+". Null returned.", e);
				ontology = null;
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return ontology;
	}

	/**
	 * Returns the ontology name as defined in the corresponding system for a given ontology. 
	 * Returns the field displayLabel of the corresponding ontology bean. 
	 */
	public String getOntologyName(String localOntologyID) throws NonValidLocalOntologyIDException {
		BioPortalFullOntologyBean ontology = this.getBioPortalOntologyBean(localOntologyID);
		if (ontology==null){
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		else{
			return ontology.getDisplayLabel();	
		}
	}	

	/**
	 * Returns the ontology description as defined in the corresponding system for a given ontology. 
	 * Returns a concatenation of different ontology properties (if they exist) from BioPortal: format + oboFoundryId
	 * + contactName + contactEmail + homepage + documentation.
	 * @throws NullBioPortalOntologyBeanException 
	 */
	public String getOntologyDescription(String localOntologyID) throws NonValidLocalOntologyIDException {
		StringBuffer ontologyDescription = new StringBuffer();
		BioPortalFullOntologyBean ontology = this.getBioPortalOntologyBean(localOntologyID);
		if (ontology==null){
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		else{
			ontologyDescription.append("format:");
			ontologyDescription.append(ontology.getFormat());
			ontologyDescription.append(", oboFoundryId:");
			ontologyDescription.append(ontology.getOboFoundryId());
			ontologyDescription.append(", contactName:");
			ontologyDescription.append(ontology.getContactName());
			ontologyDescription.append(", contactEmail:");
			ontologyDescription.append(ontology.getContactEmail());
			ontologyDescription.append(", homepage:");
			ontologyDescription.append(ontology.getHomepage());
			ontologyDescription.append(", documentation:");
			ontologyDescription.append(ontology.getDocumentation());
			return ontologyDescription.toString();
		}
	}

	/**
	 * Returns the ontology version as defined in the corresponding system for a given ontology.
 	 * Returns the field versionNumber of the corresponding ontology bean. 
	 */
	public String getOntologyVersion(String localOntologyID) throws NonValidLocalOntologyIDException {
		BioPortalFullOntologyBean ontology = this.getBioPortalOntologyBean(localOntologyID);
		if (ontology==null){
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		else{
			return ontology.getVersionNumber();	
		}
	}

	/**
	 * Returns the a list of concept beans of the ontology root concepts (i.e., concepts without parents).
	 * Get the results by hitting the URL: http://rest.bioontology.org/bioportal/rest/concepts/{localOntologyID}/root
	 * 
	 * For instance: http://rest.bioontology.org/bioportal/rest/concepts/38677/root
	 */
	public LightConceptBeanList getRootConceptBeans(String localOntologyID) throws NonValidLocalOntologyIDException {
		LightConceptBeanList rootConcepts = new LightConceptBeanList(new ArrayList<BioPortalLightConceptBean>());
		if (this.isValidLocalOntologyID(localOntologyID)){
			try {
				rootConcepts = BioPortalAccessUtil.getRootConcepts(localOntologyID);
				}
			catch (BioPortalResponseException e) {
				logger.error(e.toString());
				logger.error("** PROBLEM: Cannot get list of root concept beans from BioPortal for localOntologyID: "+ localOntologyID+". Empty list returned.");
			}
			catch (Exception e) {
				logger.error("** PROBLEM: Cannot get list of root concept beans from BioPortal for localOntologyID: "+ localOntologyID+". Empty list returned.");
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return rootConcepts;
	}
	
	/**
	 * Returns the a set of localConceptID of the ontology root concepts (i.e., concepts without parents).
	 */
	public HashSet<String> getOntologyRootConcepts(String localOntologyID) throws NonValidLocalOntologyIDException {
		HashSet<String> rootConcepts = new HashSet<String>();
		for (BioPortalLightConceptBean concept: this.getRootConceptBeans(localOntologyID).getConcepts()){
			rootConcepts.add(BioPortalAccessTool.createLocalConceptID(localOntologyID, concept.getId()));
		}
		return rootConcepts;
	}
	
	/**
	 * LocalConceptID must match the following regular expression: [\\d]+/.+
	 * For instance: 38378/DOID:1909.
	 * This function do check if the ontologyID part of the localConceptID is a valid localOntologyID using 
	 * isValidLocalOntologyID.
	 */
	public boolean isValidLocalConceptID(String localConceptID) {
		return isValidLocalConceptIDWithRegExp(localConceptID, "[\\d]+/.+");
	}

	/**
	 * Creates a localConceptID with a given localOntologyID and a given BPConceptID.
	 * LocalConceptID form is: 'localOntologyID/BPConceptID'.
	 */
	public static String createLocalConceptID(String localOntologyID, String bpConceptID){
		return localOntologyID+"/"+bpConceptID;
	}
	
	/**
	 * LocalOntologyID must match the following regular expression: [\\d]+
	 * For instance: 32145 or 38378.
	 */
	public boolean isValidLocalOntologyID(String localOntologyID) {
		String regex = "[\\d]+";
		return (localOntologyID.matches(regex));
	}
	
	/**
	 * For a given String, returns the list of concepts in the given ontology for which the given String is
	 * (i) exactly equal to one of the concept names if the flag is true;
	 * (ii) contained into one of the concept names if the flag is false;
	 *  
	 * Get the results by hitting the URL: http://rest.bioontology.org/bioportal/search/software/?ontologyids={bioportal_virtual_ontology_id}&isexactmatch=[0|1]
	 * For instance: http://rest.bioontology.org/bioportal/search/melanoma/?ontologyids=1009&isexactmatch=0
	 * 
	 * Attention, the results are in the latest version of the given ontology: the given localOntologyId is converted to virtual ontology id
	 * and then results are retrieved in that virtual ontology id (i.e., which corresponds to the latest localOntologyId
	 * in BioPortal which can be different from the given one).
	 * 
	 */
	public LightConceptBeanList mapStringToLocalConceptBeans(String s, String localOntologyID, boolean exactMap) throws NonValidLocalOntologyIDException {
		LightConceptBeanList localConceptIDs = new LightConceptBeanList(new ArrayList<BioPortalLightConceptBean>(0));
		if (this.isValidLocalOntologyID(localOntologyID)){
			BioPortalFullOntologyBean ontology = this.getBioPortalOntologyBean(localOntologyID);
			if (ontology==null){
				throw new NonValidLocalOntologyIDException(localOntologyID);
			}
			else{
				try {
					localConceptIDs = BioPortalAccessUtil.getSearchResults(s, ontology.getOntologyId(), exactMap);
				}
				catch (BioPortalResponseException e) {
					logger.error(e.toString());
					logger.error("** PROBLEM: Cannot map given string "+ s + " to concept beans. Empty list retuned");
				}
				catch (Exception e) {
					logger.error("** PROBLEM: Cannot map given string "+ s + " to concept beans. Empty list retuned", e);
				}
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return localConceptIDs;
	}
	
	/**
	 * For a given String, returns the set of localConceptIDs in the given ontology for which the given String is
	 * (i) exactly equal to one of the concept names if the flag is true;
	 * (ii) contained into one of the concept names if the flag is false;
	 *  
	 * This function uses mapStringToLocalConceptBeans and checks (by using getBioPortalConceptBean)
	 * if the results actually exists in the given localOntologyID. Longer, but accurate.
	 */
	public HashSet<String> mapStringToLocalConceptIDs(String s, String localOntologyID, boolean exactMap) throws NonValidLocalOntologyIDException {
		HashSet<String> localConceptIDs = new HashSet<String>();
		BioPortalFullConceptBean fullConcept;
		String localConceptID;
		for (BioPortalLightConceptBean concept: this.mapStringToLocalConceptBeans(s, localOntologyID, exactMap).getConcepts()){
			// creates a localConceptID with the search results
			localConceptID = BioPortalAccessTool.createLocalConceptID(localOntologyID, concept.getId());
			try {
				// gets the full concept bean
				fullConcept = this.getBioPortalConceptBean(localConceptID);
				if (fullConcept==null){
					logger.info("Concept " + concept.getId().toString() + " does not exists in " + localOntologyID);
				}
				else{
					localConceptIDs.add(BioPortalAccessTool.createLocalConceptID(localOntologyID, concept.getId()));
				}
			} catch (NonValidLocalConceptIDException e) {
				e.printStackTrace();
			}
		}
		return localConceptIDs;
	}
	
	//*************************************IS-A HIERARCHY FUNCTIONS *****************************
	
	/**
	 * For a given concept, returns the set of concept of all the direct parents.
	 */
	public ArrayList<BioPortalLightConceptBean> getConceptDirectParentBeans(String localConceptID) throws NonValidLocalConceptIDException {
		BioPortalFullConceptBean concept = this.getBioPortalConceptBean(localConceptID);
		if (concept==null){
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		else{
			return concept.getSuperClass();
		}
	}
		
	/**
	 * For a given concept, returns the set of localConceptIDs of all the direct parents.
	 * This function does not remove the cycles (i.e. concepts that are parent of themselves).
	 */
	public HashSet<String> getConceptDirectParents(String localConceptID) throws NonValidLocalConceptIDException {
		HashSet<String> directParents = new HashSet<String>();
		for (BioPortalLightConceptBean concept: this.getConceptDirectParentBeans(localConceptID)){
			directParents.add(BioPortalAccessTool.createLocalConceptID(this.getConceptOntology(localConceptID), concept.getId()));
		}
		return directParents;
	}

	public ArrayList<BioPortalLightConceptBean> getConceptDirectChildBeans(String localConceptID) throws NonValidLocalConceptIDException {
		BioPortalFullConceptBean concept = this.getBioPortalConceptBean(localConceptID);
		if (concept==null){
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		else{
			return concept.getSubClass();
		}
	}
		
	/**
	 * For a given concept, returns the set of localConceptIDs of all the direct children.
	 * This function does not remove the cycles (i.e. concepts that are child of themselves).
	 */
	public HashSet<String> getConceptDirectChildren(String localConceptID) throws NonValidLocalConceptIDException {
		HashSet<String> directChildren = new HashSet<String>();
		for (BioPortalLightConceptBean concept: this.getConceptDirectChildBeans(localConceptID)){
			directChildren.add(BioPortalAccessTool.createLocalConceptID(this.getConceptOntology(localConceptID), concept.getId()));
		}
		return directChildren;
	}

	/**
	 * Not implemented return empty set for now.
	 */
	@Override
	public HashSet<String> getConceptPtrs(String localConceptID) throws NonValidLocalConceptIDException {
		// TODO not implemented yet
		return new HashSet<String>(0);
	}
	
}
