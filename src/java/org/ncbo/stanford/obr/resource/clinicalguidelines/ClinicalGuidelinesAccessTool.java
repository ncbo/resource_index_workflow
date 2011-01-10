package org.ncbo.stanford.obr.resource.clinicalguidelines;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.stanford.obr.resource.AbstractXmlResourceAccessTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * ClinicalGuidelinesAccessTool is responsible for getting data elements for 
 * Clinical Guidelines(CGL). Gets the guidelines data to populate element table 
 * using xml file  http://www.guideline.gov/rssFiles/ngc_complete.xml
 *  
 * @author kyadav
 * @version $$
 */
public class ClinicalGuidelinesAccessTool extends AbstractXmlResourceAccessTool {

	private static final String CGL_URL = "http://www.guideline.gov";
	private static final String CGL_NAME = "ClinicalGuidelines";
	private static final String CGL_RESOURCEID = "CGL";
	private static final String CGL_DESCRIPTION = "NGC is an initiative of the Agency for Healthcare Research and Quality (AHRQ) , U.S. Department of Health and Human Services.The NGC mission is to provide physicians and other health professionals, health care providers, health plans, integrated delivery systems, purchasers, and others an accessible mechanism for obtaining objective, detailed information on clinical practice guidelines and to further their dissemination, implementation, and use.";
 	private static final String CGL_LOGO = "http://www.guideline.gov/images/guidelinelogo_large.gif";
	private static final String CGL_SERVICE = "http://www.guideline.gov/rssFiles/ngc_complete.xml";
	private static final String CGL_ELT_URL = "http://www.guideline.gov/content.aspx?id=";

	private static final String[] CGL_ITEMKEYS = {"title", "msh_concept", "url"};
	private static final Double[] CGL_WEIGHTS  = {1.0, 0.9, 0.0};
	
	// OntoID associated for reported annotations
	private static final String[] CGL_ONTOIDS  = {Structure.FOR_CONCEPT_RECOGNITION, "1351", Structure.NOT_FOR_ANNOTATION };
	
	private static Structure CGL_STRUCTURE = new Structure(CGL_ITEMKEYS, CGL_RESOURCEID, CGL_WEIGHTS, CGL_ONTOIDS);
	private static String CGL_MAIN_ITEMKEY = "title";
	
	// Constant for 'experiment' string
	private static final String CGL_ITEM = "item";  
	private static final String CGL_GUID = "guid";
	private static final String CGL_LINK = "link";
	private static final String CGL_MSH_CONCEPT = "pubmed:concept";
	private static final String CGL_MSH_TERM = "pubmed:cxs"; 
	
	private static final String ELT_TITLE = Structure.generateContextName(CGL_RESOURCEID, CGL_ITEMKEYS[0]);
	private static final String ELT_MSH_CONCEPT = Structure.generateContextName(CGL_RESOURCEID, CGL_ITEMKEYS[1]);
	private static final String ELT_LINK = Structure.generateContextName(CGL_RESOURCEID, CGL_ITEMKEYS[2]); 
	 
	 
	public ClinicalGuidelinesAccessTool(){
		super(CGL_NAME, CGL_RESOURCEID, CGL_STRUCTURE );
		try {
			this.getToolResource().setResourceURL(new URL(CGL_URL));
			this.getToolResource().setResourceLogo(new URL(CGL_LOGO));
			this.getToolResource().setResourceElementURL(CGL_ELT_URL);
		} 
		catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);			 
		}
		this.getToolResource().setResourceDescription(CGL_DESCRIPTION);
	}
	
	@Override
	public ResourceType  getResourceType() {		 
		return ResourceType.SMALL;
	}

	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}

	@Override
	public int updateResourceContent() {
		int nbElement = 0;
		ClinicalGuidelinesElement cglElement;
		Element element;
		//parse using builder to get DOM representation of the XML file
		Document dom = AbstractXmlResourceAccessTool.parseXML(CGL_SERVICE);
		//get the root element
		org.w3c.dom.Element domRoot = dom.getDocumentElement();
		//get a node list of 'item' XML elements
		NodeList itemList = domRoot.getElementsByTagName(CGL_ITEM);
		if(itemList != null && itemList.getLength() > 0) {
			int listSize = itemList.getLength();
			logger.info("Total number of elements on " + this.getToolResource().getResourceName() + ": " + listSize);
			// for each 'experiment' XML element
			for(int i = 0 ; i <listSize; i++) {
				cglElement = new ClinicalGuidelinesElement((org.w3c.dom.Element)itemList.item(i), this);
				element = cglElement.getElement();
				if (element != null && resourceUpdateService.addElement(element)){
					nbElement ++;
				}
			}
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return CGL_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return CGL_MAIN_ITEMKEY;
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {
		 
		return null;
	} 
	
	
	/**
	 * 
	 * Element class for Clinical Guidelines Tool
	 * 
	 * @author k.yadav
	 */
	private class ClinicalGuidelinesElement {

		private ClinicalGuidelinesAccessTool eltAETool;
		private HashMap<String,String> eltInfo;	
		
		ClinicalGuidelinesElement(org.w3c.dom.Element itemElement, ClinicalGuidelinesAccessTool aeTool){
			this.eltAETool = aeTool;
			this.eltInfo = new HashMap<String, String>(3); 
			String nodeName = null; 
			
			String title= EMPTY_STRING;
			String mshConcepts= EMPTY_STRING;
			String guid  = null;
			String link  = EMPTY_STRING;
			HashSet<String> meshTerms= new HashSet<String>();
			
			for (int i = 0; i < itemElement.getChildNodes().getLength(); i++) {
				Node node = itemElement.getChildNodes().item(i);
				nodeName= node.getNodeName();
				
				if(CGL_GUID.equals(nodeName)){ // Extracting guid as local_element_id
					guid= node.getTextContent();
				}else if(CGL_ITEMKEYS[0].equals(nodeName)){// Extracting title
					title = node.getTextContent();
				}else if(CGL_MSH_CONCEPT.equals(nodeName)){ // Extracting MeSH Concepts
					for (int j = 0; j < node.getChildNodes().getLength(); j++) {
						if(CGL_MSH_TERM.equals(node.getChildNodes().item(j).getNodeName())){
							meshTerms.add(node.getChildNodes().item(j).getTextContent());
						}
					} 					 
				} else if(CGL_LINK.equals(nodeName)){// Extracting link 
					link = node.getTextContent();
				}
			} 	
			
			mshConcepts =  resourceUpdateService.mapTermsToVirtualLocalConceptIDs(meshTerms, CGL_ONTOIDS[1]);
			
			this.eltInfo.put(CGL_GUID, guid);
			this.eltInfo.put(ELT_TITLE, title);			
			this.eltInfo.put(ELT_MSH_CONCEPT, mshConcepts.trim());  
			this.eltInfo.put(ELT_LINK, link); 
		}

		Element getElement(){
			Element element = null;
			ArrayList<String> contextNames = this.eltAETool.getToolResource().getResourceStructure().getContextNames(); 
			Structure eltStructure = new Structure(contextNames);

			for(String contextName: contextNames){
				eltStructure.putContext(contextName, this.eltInfo.get(contextName));
			} 
			 
			try {
				element = new Element(this.eltInfo.get(CGL_GUID), eltStructure);
			} catch (BadElementStructureException e) {			 
				logger.error("** PROBLEM ** Cannot create Element for ClinicalGuidelinesElement with guid: " + this.eltInfo.get(CGL_GUID) + ". Null have been returned.", e);
			}
			return element;
		}
	}

}
