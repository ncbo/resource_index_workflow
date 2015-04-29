package obs.ontologyAccess.util;

import java.io.InputStreamReader;
import java.io.StringWriter;

import javax.xml.transform.TransformerException;
import obs.ontologyAccess.bean.BioPortalErrorBean;
import obs.ontologyAccess.exception.BioPortalResponseException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class RequestUtils {

	// Logger for this class
	private static Logger logger = Logger.getLogger(RequestUtils.class);
	
	private static final String RI_USER_AGENT = "BioPortal-RI";
	
	private static HttpClient uniqueClient = new HttpClient();
	
	/**
	 * Create HTTP Client for Restlet and get the response string from given
	 * Restlet URI.
	 * 
	 * @param protocol
	 * @param uriString
	 * @return
	 */
	/* Commented in April 2009. Scalability bug with HTTP client (too many open files) that I cannot fix... I decided to use another client 
	public static String getResponseFromURI(Protocol protocol, String uri) {

		// Prepare HTTP client connector
		Client client;
		if(protocol.equals(Protocol.HTTP)){
			client = uniqueClient;
		}
		else{
			client = new Client(protocol);
		}
		Response response = client.get(uri);
		
		String responseString = null;
		try {
			if (response != null) {
				if (response.getEntity() == null) {
					System.out.println("**************************************Error... .getEntity() from uri " + uri
							+ "is null.");
				} else {
				responseString = response.getEntity().getText();
				}

			} else {
				System.out.println("Error... response from uri " + uri
						+ "is null.");
			}
		} catch (Exception e) {
			
			System.out.println("**********INSDIE getResponseFromURI()");
			
			e.printStackTrace();
		}

		return responseString;
	}
	*/

	/**
	 * Create HTTP Client for Restlet and get the response string from given
	 * Restlet URI. Default Protocol is HTTP.
	 * 
	 */
	/*
	public static String getResponseFromURI(String URI) {

		return RequestUtils.getResponseFromURI(Protocol.HTTP, URI);

	}
	*/

	public static String getResponseFromURI(String uri){
		String contents; 
		GetMethod method = new GetMethod(uri);
		method.setRequestHeader("User-Agent", RI_USER_AGENT);
		 
		// Execute the GET method
		int statusCode = -1;
		try {
			statusCode = uniqueClient.executeMethod(method);
			 //if( statusCode != -1 ) {
			// Commented May 11th, 2009 to get rif of the warmmings when using getResponseBodyAsString
			// contents = method.getResponseBodyAsString();
			InputStreamReader in2 = new InputStreamReader(method.getResponseBodyAsStream(), "UTF-8");
			StringWriter sw = new StringWriter();
			int x;
			while((x = in2.read()) != -1){
				sw.write(x);
			}
			in2.close();
			contents = sw.toString(); 
			method.releaseConnection();
			//}
			
		} catch (Exception e) {
			logger.error("** PROBLEM retrieving URI content (status code = "+statusCode+"): "+ uri + ". Null returned.", e);
			contents = null;
		}
		 return contents;
	}
		
	/**
	 * Apply XSL to the XML String Response from the Restlet URI. This is useful
	 * for transforming XML so that it will conform to XStream XML deserialization.
	 */
	public static String getResponseFromURI(String URI, String XSLT) throws BioPortalResponseException {

		//System.out.println("URI to hit: " + URI);
		//String xmlOriginal = RequestUtils.getResponseFromURI(Protocol.HTTP, URI);
		String xmlOriginal = RequestUtils.getResponseFromURI(URI);
		String xmlTransformed = null;
		try {
			// check response from URI before proceeding further
			checkBioPortalResponse(xmlOriginal);

			xmlTransformed = XmlUtils.transformXML(xmlOriginal, XSLT);
			//System.out.println("Result = " + xmlTransformed);


		} catch (TransformerException e) {
			logger.error("Could not tranform XML with XSL : " + XSLT);
		}

		return xmlTransformed;
	}


	/**
	 * For given response XML, apply error XSL to see if matches with Error
	 * XSLT. If matches Error, throw BioPortalResponseException.
	 * 
	 * @param xmlString
	 * @return
	 */
	public static void checkBioPortalResponse(String xmlString) throws BioPortalResponseException {

		//System.out.println("BioPortal response: " +xmlString);
		try {
			String xmlError = XmlUtils.transformXML(xmlString, ApplicationConstants.XSLT_ERROR);
			
			// if error exist (not empty), throw error with BioPortalResponseException
			if (! xmlError.equalsIgnoreCase(ApplicationConstants.XML_HEADER)) {

				// populate Exception object with the error info from Error XML provided by BioPortal.
				BioPortalErrorBean errorBean = (BioPortalErrorBean) XmlUtils.getXStreamInstance().fromXML(xmlError);
				//System.out.println("errorBean:" +errorBean.toString());
				throw new BioPortalResponseException(errorBean);

			}

		} catch (TransformerException e) {
			logger.error("getResponseError(): Could not tranform XML with XSL : "
							+ ApplicationConstants.XSLT_ERROR);
		}
		/*
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("** PROBLEM ** Problem during XML parsing. Empty BioPortalResponseException raised.");
			throw new BioPortalResponseException(new BioPortalErrorBean("", Calendar.getInstance().getTime(), "", "", ""));
		}
		*/
	}


}
