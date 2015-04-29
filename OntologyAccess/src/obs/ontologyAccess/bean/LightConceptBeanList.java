package obs.ontologyAccess.bean;

import java.util.ArrayList;

/**
 * This class is a JavaBean representation of a light concept bean list returned by BioPortal.
 *    
 * @author Clement Jonquet
 * @version 1.0
 * @created 28-Oct-2008
 */
public class LightConceptBeanList {

	private ArrayList<BioPortalLightConceptBean> concepts;
	
	public LightConceptBeanList(ArrayList<BioPortalLightConceptBean> concepts) {
		super();
		this.concepts = concepts;
	}

	public ArrayList<BioPortalLightConceptBean> getConcepts() {
		return concepts;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("LightConceptBeanList [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\tconcepts = ").append(this.concepts).append(TAB)
	        .append("]");
	    return retValue.toString();
	}

}
