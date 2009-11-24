package org.ncbo.stanford.obr.service.workflow.impl;

import obs.common.beans.IsaContextBean;
import obs.common.beans.MappingContextBean;
import obs.common.beans.MgrepContextBean;
import obs.common.beans.ReportedContextBean;
import obs.common.files.FileParameters;
import obs.common.utils.ExecutionTimer;
import obs.common.utils.Utilities;
import obs.obr.populate.ObrWeight;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.service.workflow.ResourceIndexWorkflow;
import org.ncbo.stanford.obr.util.MessageUtils;
import org.ncbo.stanford.obr.util.StringUtilities;

/**
 * @author Kuladip Yadav
 * 
 */
public class ResourceIndexWorkflowImpl implements ResourceIndexWorkflow {

	// Logger for this class
	private static Logger logger = Logger.getLogger(ResourceIndexWorkflowImpl.class);

	private static ObrWeight obrWeights = new ObrWeight(
			MgrepContextBean.PDA_WEIGHT, MgrepContextBean.SDA_WEIGHT,
			IsaContextBean.IEA_FACTOR, MappingContextBean.MEA_WEIGHT,
			ReportedContextBean.RDA_WEIGHT);

	public ResourceIndexWorkflowImpl() {

	}

	public void startResourceIndexWorkflow() {

		String[] resourceIDs = StringUtilities.splitSecure(MessageUtils
				.getMessage("obr.resource.ids"), ",");

		for (String resourceID : resourceIDs) {
			ResourceAccessTool tool = null;
			try {
				//
				tool = (ResourceAccessTool) Class.forName(
						MessageUtils.getMessage("resource."
								+ resourceID.toLowerCase())).newInstance();
				resourceProcessing(tool);
			} catch (Exception e) {
				logger.error(
						"Problem in creating resource tool for resource id : "
								+ resourceID, e);
			}

		}
	}

	public void resourceProcessing(ResourceAccessTool tool) {
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		Logger toolLogger = ResourceAccessTool.getLogger();

		toolLogger.info("**** Resource "
				+ tool.getToolResource().getResourceID() + " processing");

		// Adds resource entry into Resource Table(OBR_RT)
		tool.addResourceTableEntry();

		// Re-initialized tables
		if (Boolean.parseBoolean(MessageUtils
				.getMessage("obr.reinitialize.all"))) {
			tool.reInitializeAllTables();
		} else if (Boolean.parseBoolean(MessageUtils
				.getMessage("obr.reinitialize.only.annotation"))) {
			tool.reInitializeAllTablesExcept_ET();
		}

		// Update resource for new elements 
		if (Boolean
				.parseBoolean(MessageUtils.getMessage("obr.update.resource"))) {
			int nbElement = tool.updateResourceContent();
			toolLogger.info("Resource "
					+ tool.getToolResource().getResourceName()
					+ " updated with " + nbElement + " elements.");
		}

		// Total number of entries found in element table.
		int nbEntry = tool.numberOfElement();

		// value for withCompleteDictionary parameter.
		boolean withCompleteDictionary = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.dictionary.complete"));

		// Processing direct anotations
		int nbDirectAnnotation = tool.getAnnotationService()
				.resourceAnnotation(withCompleteDictionary,
						Utilities.arrayToHashSet(FileParameters.STOP_WORDS));

		toolLogger.info(nbEntry + " elements annotated (with "
				+ nbDirectAnnotation
				+ " new direct annotations) from resource "
				+ tool.getToolResource().getResourceID() + ".");

		// Flag for mapping expansion.  
		boolean isaClosureExpansion = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.expansion.relational"));
		
		// Flag for mapping expansion.
		boolean mappingExpansion = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.expansion.mapping"));
		
		// Flag for distance expansion.
		boolean distanceExpansion = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.expansion.distance"));

		// Creating semantic expansion annotation.
		int nbExpandedAnnotation = tool.getSemanticExpansionService()
				.semanticExpansion(isaClosureExpansion, mappingExpansion,
						distanceExpansion);
		toolLogger.info(nbEntry + " elements annotated (with "
				+ nbExpandedAnnotation
				+ " new expanded annotations) from resource "
				+ tool.getToolResource().getResourceID() + ".");

		// Indexation step to annotations.
		int nbIndexedAnnotation = tool.getIndexationService().indexation(
				obrWeights);
		toolLogger.info(nbEntry + " elements indexed (with "
				+ nbIndexedAnnotation
				+ " new indexed annotations) from resource "
				+ tool.getToolResource().getResourceID() + ".");

		// Update resource table entry for latest DictionaryID for DAT table
		tool.updateResourceTableDictionaryID();

		timer.end();
		toolLogger.info("Resource " + tool.getToolResource().getResourceName()
				+ " processed in: "
				+ timer.millisecondsToTimeString(timer.duration()));
		
	}

}
