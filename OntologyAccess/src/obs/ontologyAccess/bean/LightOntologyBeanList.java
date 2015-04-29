package obs.ontologyAccess.bean;

import java.util.ArrayList;

/**
* This class is a JavaBean representation of a light ontology bean list returned by BioPortal.
*    
* @author Clement Jonquet
* @version 1.0
* @created 28-Oct-2008 
*/
public class LightOntologyBeanList {
	
	private ArrayList<BioPortalLightOntologyBean> ontologies;

	public LightOntologyBeanList(
			ArrayList<BioPortalLightOntologyBean> ontologies) {
		super();
		this.ontologies = ontologies;
	}

	public ArrayList<BioPortalLightOntologyBean> getOntologies() {
		return ontologies;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("LightOntologyBeanList [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\tontologies = ").append(this.ontologies).append(TAB)
	        .append("]");
	    return retValue.toString();
	}

	
	
}
