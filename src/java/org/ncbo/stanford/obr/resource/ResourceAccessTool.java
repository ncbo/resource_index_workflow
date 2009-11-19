package org.ncbo.stanford.obr.resource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
 
import obs.common.utils.Utilities;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.context.ContexDao.ContextEntry;
import org.ncbo.stanford.obr.service.annotation.AnnotationService;
import org.ncbo.stanford.obr.service.annotation.impl.AnnotationServiceImpl;
import org.ncbo.stanford.obr.service.index.IndexationService;
import org.ncbo.stanford.obr.service.index.impl.IndexationServiceImpl;
import org.ncbo.stanford.obr.service.resource.ResourceUpdateService;
import org.ncbo.stanford.obr.service.resource.impl.ResourceUpdateServiceImpl;
import org.ncbo.stanford.obr.service.semantic.SemanticExpansionService;
import org.ncbo.stanford.obr.service.semantic.impl.SemanticExpansionServiceImpl;
import org.ncbo.stanford.obr.util.helper.StringHelper;

/**
 * This abstract class is used as a generic Resource access and manipulation tool.
 * A tool has a name, and give access to exactly one Resource. 
 * A ResourceAccessTool is associated to several OBR tables in the OBS DB.
 *   
 * @author Adrien Coulet, Clement Jonquet, Kuladip Yadav
 * @version OBR_v_0.2
 * @created 20-Nov-2008
 */
public abstract class ResourceAccessTool implements StringHelper {
	
	// Logger for ResourceAccessTool 
	protected static Logger logger = Logger.getLogger(ResourceAccessTool.class);

	public static final String RESOURCE_NAME_PREFIX = "OBR_RESOURCE_";	
	private Resource toolResource;
	private String toolName;
	
	protected ResourceUpdateService resourceUpdateService;
	protected AnnotationService annotationService;
	protected SemanticExpansionService semanticExpansionService;
	protected IndexationService indexationService;
		
	/**
	 * Constructs a new ResourceAccessTool associated to a new Resource constructed with the given information
	 * Gets access also the associated tables in the DB (and eventually created them). 
	 * If the corresponding contexts do not exist in OBR_CXT, they are created.
	 */
	public ResourceAccessTool(String resource, String resourceID, Structure structure){
		super();		 
		logger.info("ResourceAccessTool creation...");		 
		this.toolName = RESOURCE_NAME_PREFIX + resourceID + Utilities.getRandomString(3);
		String mainContext = Structure.generateContextName(resourceID, this.mainContextDescriptor());
		this.toolResource = new Resource(resource, resourceID, structure, mainContext);
		
		this.resourceUpdateService = new ResourceUpdateServiceImpl(this);
		
		this.annotationService = new AnnotationServiceImpl(this);
		this.semanticExpansionService = new SemanticExpansionServiceImpl(this);
		this.indexationService= new IndexationServiceImpl(this);
		
		// Adds the structure's contexts in OBR_CXT
		ContextEntry context;
		for(String contextName: structure.getContextNames()){
			context = new ContextEntry(contextName, structure.getWeight(contextName), structure.getOntoID(contextName));
			AbstractObrDao.contextTableDao.addEntry(context);
		}
		
		logger.info("ResourceAccessTool " + this.getToolResource().getResourceID() + " created to access " + this.getToolResource().getResourceName() +" (" + this.getToolResource().getResourceID() + ").");
	} 
		

	/**
	 * @return the resourceUpdateService
	 */
	public ResourceUpdateService getResourceUpdateService() {
		return resourceUpdateService;
	}



	/**
	 * @param resourceUpdateService the resourceUpdateService to set
	 */
	public void setResourceUpdateService(ResourceUpdateService resourceUpdateService) {
		this.resourceUpdateService = resourceUpdateService;
	}



	/**
	 * @return the annotationService
	 */
	public AnnotationService getAnnotationService() {
		return annotationService;
	}



	/**
	 * @param annotationService the annotationService to set
	 */
	public void setAnnotationService(AnnotationService annotationService) {
		this.annotationService = annotationService;
	}



	/**
	 * @return the semanticExpansionService
	 */
	public SemanticExpansionService getSemanticExpansionService() {
		return semanticExpansionService;
	}



	/**
	 * @param semanticExpansionService the semanticExpansionService to set
	 */
	public void setSemanticExpansionService(
			SemanticExpansionService semanticExpansionService) {
		this.semanticExpansionService = semanticExpansionService;
	}



	/**
	 * @return the indexationService
	 */
	public IndexationService getIndexationService() {
		return indexationService;
	}



	/**
	 * @param indexationService the indexationService to set
	 */
	public void setIndexationService(IndexationService indexationService) {
		this.indexationService = indexationService;
	}



	/**
	 * Returns the log4j logger
	 */
	public static Logger getLogger() {
		return logger;
	}
	
	public void finalize() throws Throwable {
		super.finalize();
	}
	
	/**
	 * Returns the associated Resource.
	 */
	public Resource getToolResource() {
		return toolResource;
	}
	
	/**
	 * Returns the tool name.
	 */
	public String getToolName() {
		return toolName;
	}
	
	 

	/**
	 * Updates the associated Resource information fields (name, URL, description, logo URL) automatically.
	 */
	public abstract void updateResourceInformation();
	
	
	/**
	 * Enables to query a resource with a String as it is done online. 
	 * Returns a set of localElementIDs (String) which answer the query.  
	 */
	public abstract HashSet<String> queryOnlineResource(String query); 
	
	/********************************* OBR WORKFLOW FUNCTIONS *****************************************************/
		
	/**
	 * Updates the resource content automatically (locally or remotely). Returns the number of elements updated.
	 * This function implements the step 1 of the OBR workflow.
	 */
	public abstract int updateResourceContent();  
	
	/**
	 * Returns the number of elements in the _ET table. 
	 */
	public int numberOfElement(){
		return this.resourceUpdateService.numberOfEntry();
	}
	
	public void reInitializeAllTables(){
		this.resourceUpdateService.reInitializeAllTables();		 
	}
	
	public void reInitializeAllTablesExcept_ET(){
		this.resourceUpdateService.reInitializeAllTablesExcept_ET();		 
	}
	
	/**
	 * Generates an URL for a given Element localID String.
	 */
	public URL generateElementURL(String localElementID){
		URL elementURL;
		try{
			elementURL = new URL(this.elementURLString(localElementID));
		}
		catch(MalformedURLException e){
			logger.error("Problem when creating the URL of element " + localElementID + ". The URL of the corresponding resource has been returned.", e);
			elementURL = this.getToolResource().getResourceURL();
		}
		return elementURL;
	}
	
	/**
	 * Returns a String to generate the URL of the given element.
	 */
	public abstract String elementURLString(String localElementID);
	
	/**
	 * Returns the main context (from the Structure) that describes the resource element.
	 * e.g., title or name
	 */
	public abstract String mainContextDescriptor();
		
	/**
	 * Enables to query a resource with a String as it is done online. 
	 * Returns a set of elementLocalIDs (String) which answer the query.  
	 */
	//public abstract HashSet<String> queryOnlineResource(String query); 
	
	
	/**
	 * Adds the Resource tool entry into Resource table (OBR_RT)
	 * 
	 */
	public void addResourceTableEntry(){
		this.resourceUpdateService.addResource(this.getToolResource());
	}

	/**
	 * Update resource table with latest dictionayID
	 * 
	 */
	public void updateResourceTableDictionaryID() {
		resourceUpdateService.updateResourceForLatestDictionary(this.getToolResource());
	} 
	
	
	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("ResourceAccessTool [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\ttoolResource = ").append(this.toolResource).append(TAB)
	        .append("\ttoolName = ").append(this.toolName).append(TAB)	         
	        .append("]");
	    return retValue.toString();
	}
	
	 
	 
}