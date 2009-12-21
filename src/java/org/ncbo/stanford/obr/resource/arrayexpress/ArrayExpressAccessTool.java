package org.ncbo.stanford.obr.resource.arrayexpress;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.ncbo.stanford.obr.resource.AbstractXmlResourceAccessTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
public class ArrayExpressAccessTool extends AbstractXmlResourceAccessTool {

	private static final String AE_URL = "http://www.ebi.ac.uk/arrayexpress/";
	private static final String AE_NAME = "ArrayExpress";
	private static final String AE_RESOURCEID = "AE";
	private static final String AE_DESCRIPTION = "ArrayExpress is a public repository for microarray data, which is aimed at storing MIAME-compliant data in accordance with MGED recommendations. The ArrayExpress Data Warehouse stores gene-indexed expression profiles from a curated subset of experiments in the repository.";
	private static final String AE_LOGO = "http://www.ebi.ac.uk/microarray-as/aer/include/aelogo.png";
	private static final String AE_SERVICE = "http://www.ebi.ac.uk/microarray-as/aer/jsp/ae_expts.jsp";
	private static final String AE_ELT_URL = "http://www.ebi.ac.uk/microarray-as/ae/browse.html?detailedview=on&keywords=";

	private static final String[] AE_ITEMKEYS = {"name", "description", "species"};
	private static final Double[] AE_WEIGHTS  = {1.0, 0.8, 1.0};
	
	// OntoID associated for reported annotations
	private static final String[] AE_ONTOIDS  = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, "1123"};
	
	private static Structure AE_STRUCTURE = new Structure(AE_ITEMKEYS, AE_RESOURCEID, AE_WEIGHTS, AE_ONTOIDS);
	private static String AE_MAIN_ITEMKEY = "name";
	
	// Constant for 'experiment' string
	private static final String AE_EXPERIMENT = "experiment";
	private static final String ELT_ACCNUM = "accnum";
	
	public ArrayExpressAccessTool(){
		super(AE_NAME, AE_RESOURCEID, AE_STRUCTURE );
		try {
			this.getToolResource().setResourceURL(new URL(AE_URL));
			this.getToolResource().setResourceLogo(new URL(AE_LOGO));
			this.getToolResource().setResourceElementURL(AE_ELT_URL);
		} 
		catch (MalformedURLException e) {
			logger.error("", e);			 
		}
		this.getToolResource().setResourceDescription(AE_DESCRIPTION);
	}

	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}

	@Override
	public int updateResourceContent() {
		int nbElement = 0;
		ArrayExpressElement aeElement;
		Element element;
		//parse using builder to get DOM representation of the XML file
		Document dom = AbstractXmlResourceAccessTool.parseXML(AE_SERVICE);
		//get the root element
		org.w3c.dom.Element domRoot = dom.getDocumentElement();
		//get a nodelist of 'experiment' XML elements
		NodeList experimentList = domRoot.getElementsByTagName(AE_EXPERIMENT);
		if(experimentList != null && experimentList.getLength() > 0) {
			int listSize = experimentList.getLength();
			logger.info("Total number of elements on " + this.getToolResource().getResourceName() + ": " + listSize);
			// for each 'experiment' XML element
			for(int i = 0 ; i <listSize; i++) {
				aeElement = new ArrayExpressElement((org.w3c.dom.Element)experimentList.item(i), this);
				element = aeElement.getElement();
				if (resourceUpdateService.addElement(element)){
					nbElement ++;
				}
			}
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return AE_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return AE_MAIN_ITEMKEY;
	}

	@Override
	//This function don't use ArrayExpressElement for optimization reasons.
	public HashSet<String> queryOnlineResource(String query) {
		HashSet<String> answerIDs = new HashSet<String>();

		// do not execute queryOnline for phrase with space
		String regexp = "\\S+\\s.+";
		if (!query.matches(regexp)){
			String accnum = EMPTY_STRING;
			//parse using builder to get DOM representation of the XML file done with the query
			Document dom = AbstractXmlResourceAccessTool.parseXML(this.getXMLForQuery(query));
			//get the root element
			org.w3c.dom.Element domRoot = dom.getDocumentElement();
			//get a nodelist of 'experiment' XML elements
			NodeList experimentList = domRoot.getElementsByTagName(AE_EXPERIMENT);
			org.w3c.dom.Element experimentElt;

			if(experimentList != null && experimentList.getLength() > 0) {
				int listSize1 = experimentList.getLength();
				// for each 'experiment' XML element
				for(int i = 0 ; i <listSize1; i++) {
					experimentElt = (org.w3c.dom.Element)experimentList.item(i);
					accnum = experimentElt.getAttribute(ELT_ACCNUM);
					answerIDs.add(accnum);
				}
			}
		}
		return answerIDs;
	}

	private String getXMLForQuery(String query){
		return AE_SERVICE + "?keyword=" + query.replaceAll(BLANK_SPACE, "%20");
	} 

	private class ArrayExpressElement {

		ArrayExpressAccessTool eltAETool;
		HashMap<String,String> eltInfo;		
		final String ELT_NAME = Structure.generateContextName(AE_RESOURCEID, AE_ITEMKEYS[0]);
		final String ELT_SPECIES = Structure.generateContextName(AE_RESOURCEID, AE_ITEMKEYS[2]);
		final String ELT_DESCRIPTION = Structure.generateContextName(AE_RESOURCEID, AE_ITEMKEYS[1]); 

		ArrayExpressElement(org.w3c.dom.Element experimentElt, ArrayExpressAccessTool aeTool){
			this.eltAETool = aeTool;
			this.eltInfo = new HashMap<String, String>(3);

			this.eltInfo.put(ELT_ACCNUM, experimentElt.getAttribute(ELT_ACCNUM));
			this.eltInfo.put(ELT_NAME, experimentElt.getAttribute(AE_ITEMKEYS[0]));
			String species = experimentElt.getAttribute(AE_ITEMKEYS[2]);
			if(species!= null && species.length() >0){
				species=  resourceUpdateService.mapTermsToVirtualLocalConceptIDs(species, AE_ONTOIDS[2], COMMA_STRING);
			}else{
				species =EMPTY_STRING;
			}
			this.eltInfo.put(ELT_SPECIES, species);
			String description = EMPTY_STRING;
			NodeList descriptionList;
			Node descriptionNode;
			descriptionList = experimentElt.getElementsByTagName(AE_ITEMKEYS[1]);
			//for each 'description' XML element
			if(descriptionList != null && descriptionList.getLength() > 0) {
				int listSize = descriptionList.getLength();
				for(int j=0; j<listSize; j++){
					descriptionNode = descriptionList.item(j);
					description += BLANK_SPACE + descriptionNode.getTextContent();
				}
			}
			this.eltInfo.put(ELT_DESCRIPTION, description);
		}

		Element getElement(){
			Element element = null;
			ArrayList<String> contextNames = this.eltAETool.getToolResource().getResourceStructure().getContextNames(); 
			Structure eltStructure = new Structure(contextNames);

			for(String contextName: contextNames){
				eltStructure.putContext(contextName, this.eltInfo.get(contextName));
			} 
			 
			try {
				element = new Element(this.eltInfo.get(ELT_ACCNUM), eltStructure);
			} catch (BadElementStructureException e) {			 
				logger.error("** PROBLEM ** Cannot create Element for ArrayExpressElement with accnum: " + this.eltInfo.get(ELT_ACCNUM) + ". Null have been returned.", e);
			}
			return element;
		}
	}

}
