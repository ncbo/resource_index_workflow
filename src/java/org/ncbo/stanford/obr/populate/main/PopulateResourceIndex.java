package org.ncbo.stanford.obr.populate.main;

import org.ncbo.stanford.obr.service.workflow.ResourceIndexWorkflow;
import org.ncbo.stanford.obr.service.workflow.impl.ResourceIndexWorkflowImpl;
import org.ncbo.stanford.obr.util.MessageUtils;

/**
 * Main class for resource index population which is responsible for population of different resources 
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
		boolean processResources = Boolean.parseBoolean(MessageUtils.getMessage("obr.resources.process"));
		boolean removeDuplicateOntologies = Boolean.parseBoolean(MessageUtils.getMessage("obs.slave.ontology.remove"));
		
		try{
			// Populate obs tables from master database 
			if(poluateSlaveTables){	
				try{
					resourceIndexWorkflow.populateObsSlaveTables();
					resourceIndexWorkflow.loadObsSlaveTablesIntoMemory();
				}catch (Exception e) {
					processResources=false;
					removeDuplicateOntologies=false;
					e.printStackTrace();
				} 
			}				
			
			// Populate resource index data
			if(processResources){
				 resourceIndexWorkflow.startResourceIndexWorkflow();
			}
		   
			// Remove duplicates.
			if(removeDuplicateOntologies){
				resourceIndexWorkflow.removeOntologyDuplicates();
				resourceIndexWorkflow.loadObsSlaveTablesIntoMemory();
			}
			
		}catch (Exception e) {
			 e.printStackTrace();
		} 

	}

}
