package org.ncbo.stanford.obr.populate.main;

import org.ncbo.stanford.obr.service.workflow.ResourceIndexWorkflow;
import org.ncbo.stanford.obr.service.workflow.impl.ResourceIndexWorkflowImpl;
import org.ncbo.stanford.obr.util.MessageUtils;

/**
 * Main class for resource index population which is responsible for population of diffrent resources 
 * and annotate them with updated obs data.
 * 
 * @author Kuladip Yadav
 */
public class PopulateResourceIndex { 
	
	/**
	 * Main method for execution of workflow.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		 
		ResourceIndexWorkflow resourceIndexWorkflow = new ResourceIndexWorkflowImpl();
		
		boolean poluateSlaveTables = Boolean.parseBoolean(MessageUtils.getMessage("obs.slave.populate"));
		
		// Populate obs tables from master database 
		if(poluateSlaveTables){	
			resourceIndexWorkflow.populateObsSlaveTables();
		}	
		
		// Populate resource index data
	    resourceIndexWorkflow.startResourceIndexWorkflow();

	}

}
