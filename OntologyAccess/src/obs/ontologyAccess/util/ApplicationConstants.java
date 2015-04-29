package obs.ontologyAccess.util;

public interface ApplicationConstants {
		
	static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	//XSLT file location
	static final String XSLT_CONCEPT = "files/xslt/concept.xsl";
	static final String XSLT_CONCEPT_ROOT = "files/xslt/concept_root.xsl";
	static final String XSLT_ERROR = "files/xslt/error.xsl";
	static final String XSLT_ONTOLOGY = "files/xslt/ontology.xsl";
	static final String XSLT_ONTOLOGY_LIST = "files/xslt/ontology_list.xsl";
	static final String XSLT_SEARCH = "files/xslt/search.xsl";
	
	// BioPortal URI Base
	String uriBase = MessageUtils.getMessage("bp.url.base");
	static final String DEFAULT_URI_BASE = "http://rest.bioontology.org";
	static final String URI_BASE = ((uriBase != null) ? uriBase : DEFAULT_URI_BASE) + "/bioportal/";

	// Application API Key for BioPortal Authentication
	String apiKey = MessageUtils.getMessage("bp.obr.apikey");
	static final String DEFAULT_APIKEY_VALUE = "77591c48-6d2b-11e0-9b85-005056bd0024";	
	static final String APIKEY_VALUE = (apiKey != null) ? apiKey : DEFAULT_APIKEY_VALUE;
	
	static final String PATH_ONTOLOGIES = "ontologies";
	static final String PATH_CONCEPTS = "concepts";
	static final String PATH_SEARCH = "search";
	static final String PATH_AUTH = "auth";
	static final String PATH_ROOT = "root";
	static final String SEPARATOR = "/"; 
	static final String AND_PARAMETER = "&"; 
	static final String PARAMETERS = "?"; 
//	static final String APPLICATION_ID = "email=jonquet@stanford.edu" + AND_PARAMETER + "applicationid=bioportal-accesstool";
	static final String PARAM_APIKEY = "apikey";
	
	static final String APIKEY = PARAM_APIKEY + "=" + APIKEY_VALUE;
	
	// BioPortal ontology status ID for usable ontologies
	static final int STATUS_ID = 3;	
}
