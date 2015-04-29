package obs.ontologyAccess.util;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import obs.ontologyAccess.bean.BioPortalErrorBean;
import obs.ontologyAccess.bean.BioPortalFullConceptBean;
import obs.ontologyAccess.bean.BioPortalFullOntologyBean;
import obs.ontologyAccess.bean.BioPortalLightConceptBean;
import obs.ontologyAccess.bean.BioPortalLightOntologyBean;
import obs.ontologyAccess.bean.LightConceptBeanList;
import obs.ontologyAccess.bean.LightOntologyBeanList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thoughtworks.xstream.XStream;

public class XmlUtils {

	private static final Log log = LogFactory.getLog(XmlUtils.class);

	private static HashMap<String, Transformer> transformers = new HashMap<String, Transformer>();
	private static XStream xstream = null;
	
	
	/**
	 * Generate an XML representation of a successfully processed request with
	 * XSL Transformation. 
	 *  
	 * 
	 * @param xmlFileString
	 * @param xsltFileString
	 * @return String
	 * @throws TransformerException
	 */
	public static String transformXML(String xmlString, String xsltFileString)
			throws TransformerException {
		
		File xsltFile = new File(xsltFileString);
		Source xsltSource = new StreamSource(xsltFile);
		
		Source xmlSource = new StreamSource(new StringReader(xmlString));

		return transformXML (xmlSource, xsltSource);

	}
	
	
	/**
	 * Generate an XML representation of a successfully processed request with
	 * XSL Transformation. 
	 * 
	 * @param xmlFileString
	 * @param xsltFileString
	 * @return String
	 * @throws TransformerException
	 */
	public static String transformXML(Source xmlSource, Source xsltSource)
			throws TransformerException {

		
		StringWriter result = new StringWriter();
		try {
			getTransformerInstance(xsltSource).transform(xmlSource, new StreamResult(result));
		} catch (Exception e) {			
			//e.printStackTrace();
			log.error("Error - Could not transform XML source with XSLT file:" + xsltSource.toString());
		}
		
		return result.toString();

	}
	
	public static Transformer getTransformerInstance(Source xsltSource) 
			throws TransformerException {

		// get Transformer instance 
		Transformer transformer = TransformerFactory.newInstance().newTransformer(xsltSource);

		// cache XSLT
		// Templates cachedXSLT = transFactory.newTemplates(xsltSource);
		// Transformer transformer = cachedXSLT.newTransformer();

		return transformer;
	}
	
	
	//TODO - TO BE DETERMINED : Use Hashmap to store the instance or NOT? as in bioporportal...
			/*****************************************
			 * This code is from bioportal 2.0
			 * Store transformer instance in Hashmap by XSL FILE
			 * 
			 * 
			 */
			
	/**
	 * returns a singleton transformer instance for a XSL file specified. do not
	 * use synchronized since it is expensive.
	 */
	public static Transformer getTransformerInstance(String xslFile)
			throws TransformerException {
		Transformer transformer = (Transformer) transformers.get(xslFile);

		if (transformer == null) {
			File ontologyXSLT = new File(xslFile);
			transformer = TransformerFactory.newInstance().newTransformer(
					new StreamSource(ontologyXSLT));
			transformers.put(xslFile, transformer);
		}

		return transformer;
	}
	
	
	/**
	 * returns a singleton XStream instance. do not use synchronized since it is
	 * expensive.
	 */
	public static XStream getXStreamInstance() {
		if (xstream == null) {
			
			// Use XPP3 parser instead of built-in JAXP DOM Parser for better performance
			xstream = new XStream();
			xstream.setMode(XStream.NO_REFERENCES);
			setXStreamAliases(xstream);
		}

		return xstream;
	}
	
	/**
	 * set aliases for XStream
	 */
	private static void setXStreamAliases(XStream xstream) {
		
//		xstream.autodetectAnnotations(true);
//		xstream.alias("success", SuccessBean.class);
//		xstream.omitField(SuccessBean.class, "sessionId");
		
        xstream.alias("error", BioPortalErrorBean.class);
        xstream.alias("list", ArrayList.class);
        xstream.alias("lightOntology", BioPortalLightOntologyBean.class);
        xstream.alias("fullOntology", BioPortalFullOntologyBean.class);
        xstream.alias("fullConcept", BioPortalFullConceptBean.class);
        xstream.alias("lightConcept", BioPortalLightConceptBean.class);
        xstream.alias("conceptList", LightConceptBeanList.class);
        xstream.alias("ontologyList", LightOntologyBeanList.class);
	}
	
	
	
}
