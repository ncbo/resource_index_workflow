package org.ncbo.stanford.obr.service;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.dao.annoation.DirectAnnotationDao;
import org.ncbo.stanford.obr.dao.element.ElementDao;
import org.ncbo.stanford.obr.dao.index.IndexDao;
import org.ncbo.stanford.obr.dao.obs.CommonObsDao;
import org.ncbo.stanford.obr.dao.semantic.ExpandedAnnotationDao;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.util.helper.StringHelper;

public abstract class AbstractResourceService implements StringHelper{	
	
	// Logger for AbstractResourceService 
	protected static Logger logger = Logger.getLogger(AbstractResourceService.class);
	
	protected static ResourceAccessTool resourceAccessTool;	
	
	protected static ElementDao elementTableDao;
	protected static DirectAnnotationDao directAnnotationTableDao;
	protected static ExpandedAnnotationDao expandedAnnotationTableDao;
	protected static IndexDao indexTableDao;
	
	// OBS tables dao
	protected static CommonObsDao commonObsDao = CommonObsDao.getInstance();
	
	 
	public AbstractResourceService(ResourceAccessTool resourceAccessTool) {
		super();
		
		if(AbstractResourceService.resourceAccessTool== null 
				|| AbstractResourceService.resourceAccessTool.getToolResource().getResourceID()!=resourceAccessTool.getToolResource().getResourceID()){
			 AbstractResourceService.resourceAccessTool = resourceAccessTool;
				
			// Creating Element Table Dao for given resource access tool
			 elementTableDao= new ElementDao(resourceAccessTool.getToolResource().getResourceID() 
					, resourceAccessTool.getToolResource().getResourceStructure()) ;
					 
			 directAnnotationTableDao= new DirectAnnotationDao(resourceAccessTool.getToolResource().getResourceID()) ;
			
			 expandedAnnotationTableDao= new ExpandedAnnotationDao(resourceAccessTool.getToolResource().getResourceID());
			
			 indexTableDao = new IndexDao(resourceAccessTool.getToolResource().getResourceID());
			
		}
		
		
	}
	
	
	

}
