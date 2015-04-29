package obs.ontologyAccess.bean;

/**
 * This class is a JavaBean representation of a light concept bean returned by BioPortal.
 * Light concepts bean are for example the beans returned in the SubClass/SuperClass relation 
 * of a full concept bean.
 *    
 * @author Clement Jonquet
 * @version 1.0
 * @created 27-Oct-2008
 */
public class BioPortalLightConceptBean {
	
	private String id;
	private String label;
	
	public BioPortalLightConceptBean(String id, String label) {
		super();
		this.id = id;
		this.label = label;
	}

	public String getId() {
		return id;
	}
	
	public String getLabel() {
		return label;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("BioPortalLightConceptBean [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\tid = ").append(this.id).append(TAB)
	        .append("\tlabel = ").append(this.label).append(TAB)
	        .append("]");
	    return retValue.toString();
	}
	
	
	
}
