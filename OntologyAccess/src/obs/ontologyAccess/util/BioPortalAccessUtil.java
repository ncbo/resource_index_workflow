package obs.ontologyAccess.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import obs.ontologyAccess.bean.BioPortalFullConceptBean;
import obs.ontologyAccess.bean.BioPortalFullOntologyBean;
import obs.ontologyAccess.bean.BioPortalLightConceptBean;
import obs.ontologyAccess.bean.LightConceptBeanList;
import obs.ontologyAccess.bean.LightOntologyBeanList;
import obs.ontologyAccess.exception.BioPortalResponseException;

public class BioPortalAccessUtil {
	
	// Logger for this class
	private static Logger logger = Logger.getLogger(BioPortalAccessUtil.class);

	//************************************ GET URIs FUNCTIONS *****************************
	
	/**
	 * Returns URI string for getting all ontologies from BioPortal.
	 */
	public static String getOntologyListURI() {
		return ApplicationConstants.URI_BASE
		+ ApplicationConstants.PATH_ONTOLOGIES
		+ ApplicationConstants.PARAMETERS
		+ ApplicationConstants.APIKEY;
	}

	/**
	 * Returns URI string for getting a given ontology from BioPortal.
	 */
	public static String getOntologyURI(String localOntologyID) {
		return ApplicationConstants.URI_BASE 
		+ ApplicationConstants.PATH_ONTOLOGIES
		+ ApplicationConstants.SEPARATOR + localOntologyID
		+ ApplicationConstants.PARAMETERS
		+ ApplicationConstants.APIKEY;
	}
	
	/** 
	 * Returns URI string for getting root concepts for a given localOntologyID from BioPortal.
	 */
	public static String getRootConceptsURI(String localOntologyID) {
		return ApplicationConstants.URI_BASE
		+ ApplicationConstants.PATH_CONCEPTS
		+ ApplicationConstants.SEPARATOR + localOntologyID
		+ ApplicationConstants.SEPARATOR
		+ ApplicationConstants.PATH_ROOT
		+ ApplicationConstants.PARAMETERS
		+ ApplicationConstants.APIKEY;
	}

	/**
	 * Returns URI string for getting a given concept from BioPortal.
	 */
	public static String getConceptURI(String localOntologyID, String bpConceptId) {
		StringBuffer urib = new StringBuffer();
		urib.append(ApplicationConstants.URI_BASE);
		urib.append(ApplicationConstants.PATH_CONCEPTS); 
		urib.append(ApplicationConstants.SEPARATOR);
		urib.append(localOntologyID);
		urib.append(ApplicationConstants.PARAMETERS);
		urib.append("conceptid=");
		urib.append(bpEncode(bpConceptId));
		urib.append(ApplicationConstants.AND_PARAMETER);
		urib.append(ApplicationConstants.APIKEY);
		
		return urib.toString();
	}

	/**
	 * Returns URI string for getting search results for a given string in a given virtual ontologyID 
	 * from BioPortal.
	 */
	public static String getSearchURI(String term, Integer ontologyID, boolean exactMap) {
		StringBuffer urib = new StringBuffer();
		urib.append(ApplicationConstants.URI_BASE);
		urib.append(ApplicationConstants.PATH_SEARCH); 
		urib.append(ApplicationConstants.SEPARATOR);
		urib.append(bpEncode(term));
		urib.append(ApplicationConstants.PARAMETERS);
		urib.append("ontologyids=").append(ontologyID.toString()).append(ApplicationConstants.AND_PARAMETER).append("isexactmatch=");
		if(exactMap){
			urib.append("1");
		}
		else{
			urib.append("0");
		}
		urib.append(ApplicationConstants.AND_PARAMETER);
		urib.append(ApplicationConstants.APIKEY);
		
		return urib.toString();
	}

	public static String getURIForAuthentication() {
		StringBuffer urib = new StringBuffer();
		urib.append(ApplicationConstants.URI_BASE);
		urib.append(ApplicationConstants.PATH_AUTH); 

		return urib.toString();
	}
	
	/**
	 * Encoding function to hit URI for BioPortal.
	 * Right now, called only for concept URI and search URI.
	 * Should be call for all URIs localOntologyID becomes something else than a number.
	 */
	private static String bpEncode(String stringToEncode){
		String encoded;
		try {
			encoded = URLEncoder.encode(stringToEncode, "UTF-8").replace("+", "%20");
			//System.out.println("original string:" + stringToEncode);
			//System.out.println("encoded string:" + encoded);
			return encoded; 
		} catch (UnsupportedEncodingException e) {
			//e.printStackTrace();
			logger.error("** PROBLEM: Cannot encode string: "+ stringToEncode+" for URI. Original string returned.");
			return stringToEncode;
		}
	}
	
	//************************************* XML PARSING FUNCTIONS *****************************
	
	/**
	 * Get list of ontologies returned from BioPortal.
	 */
	public static LightOntologyBeanList getOntologies() throws BioPortalResponseException {
		LightOntologyBeanList ontologies = null;

		// gets the XML using the right URI and XSLT
		String xmlString = RequestUtils.getResponseFromURI(getOntologyListURI(), ApplicationConstants.XSLT_ONTOLOGY_LIST);

		// parses the XML string with Xstream
		if (null != xmlString && !"".equals(xmlString)) {
			ontologies = (LightOntologyBeanList) XmlUtils.getXStreamInstance().fromXML(xmlString);

		}
		return ontologies;
	}

	/**
	 * Get a concept bean from BioPortal for a given localOntologyID and bpConceptId.
	 */
	public static BioPortalFullConceptBean getConcept(String localOntologyID, String bpConceptId) throws BioPortalResponseException {
		BioPortalFullConceptBean concept = null;
		
		// gets the XML using the right URI and XSLT
		String xmlString = RequestUtils.getResponseFromURI(getConceptURI(localOntologyID, bpConceptId), ApplicationConstants.XSLT_CONCEPT);

		// parses the XML string with Xstream
		if (null != xmlString && !"".equals(xmlString)) {
			//System.out.println(xmlString);
			concept = (BioPortalFullConceptBean) XmlUtils.getXStreamInstance().fromXML(xmlString);
			// completes the bean if necessary
			if(concept.getExactSynonyms()==null) concept.setExactSynonyms(new ArrayList<String>(0));
			if(concept.getNarrowSynonyms()==null) concept.setNarrowSynonyms(new ArrayList<String>(0));
			if(concept.getBroadSynonyms()==null) concept.setBroadSynonyms(new ArrayList<String>(0));
			if(concept.getRelatedSynonyms()==null) concept.setRelatedSynonyms(new ArrayList<String>(0));
			if(concept.getBpSynonyms()==null) concept.setBpSynonyms(new ArrayList<String>(0));
			if(concept.getSuperClass()==null) concept.setSuperClass(new ArrayList<BioPortalLightConceptBean>(0));
			if(concept.getSubClass()==null) concept.setSubClass(new ArrayList<BioPortalLightConceptBean>(0));
		}
		return concept;
	}
	
	/**
	 * Get an ontology bean from BioPortal for a given localOntologyID.
	 */
	public static BioPortalFullOntologyBean getOntology(String localOntologyID) throws BioPortalResponseException {
		BioPortalFullOntologyBean ontology = null;

		// gets the XML using the right URI and XSLT
		String xmlString = RequestUtils.getResponseFromURI(getOntologyURI(localOntologyID), ApplicationConstants.XSLT_ONTOLOGY);
		
		// parses the XML string with Xstream
		if (null != xmlString && !"".equals(xmlString)) {
			ontology = (BioPortalFullOntologyBean) XmlUtils.getXStreamInstance().fromXML(xmlString);
		}
		return ontology;
	}
	
	/**
	 * Get list of concept beans from BioPortal for a given localOntologyID.
	 */
	public static LightConceptBeanList getRootConcepts(String localOntologyID) throws BioPortalResponseException {
		LightConceptBeanList concepts = null;

		// gets the XML using the right URI and XSLT
		String xmlString = RequestUtils.getResponseFromURI(getRootConceptsURI(localOntologyID), ApplicationConstants.XSLT_CONCEPT_ROOT);

		// parses the XML string with Xstream
		if (null != xmlString && !"".equals(xmlString)) {
			//System.out.println(xmlString);
			concepts = (LightConceptBeanList) XmlUtils.getXStreamInstance().fromXML(xmlString);
		}
		return concepts;
	}
	
	/**
	 * Get list of concept beans from BioPortal for a given string to search and a virtual ontologyID to search in.
	 */
	public static LightConceptBeanList getSearchResults(String s, Integer ontologyID, boolean exactMap) throws BioPortalResponseException {
		LightConceptBeanList concepts = null;

		// gets the XML using the right URI and XSLT
		String xmlString = RequestUtils.getResponseFromURI(getSearchURI(s, ontologyID, exactMap), ApplicationConstants.XSLT_SEARCH);
		
		// parses the XML string with Xstream
		if (null != xmlString && !"".equals(xmlString)) {
			concepts = (LightConceptBeanList) XmlUtils.getXStreamInstance().fromXML(xmlString);
		}
		return concepts;
	}	
}
