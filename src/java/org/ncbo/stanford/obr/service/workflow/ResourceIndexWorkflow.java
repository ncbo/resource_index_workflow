package org.ncbo.stanford.obr.service.workflow;

import org.ncbo.stanford.obr.resource.ResourceAccessTool;

/**
 * @author Kuladip Yadav
 *
 */
public interface ResourceIndexWorkflow {

	/**
	 * 
	 */
	public void startResourceIndexWorkflow();
	
	/**
	 * 
	 * @param tool
	 */
	public void resourceProcessing(ResourceAccessTool tool);
}
