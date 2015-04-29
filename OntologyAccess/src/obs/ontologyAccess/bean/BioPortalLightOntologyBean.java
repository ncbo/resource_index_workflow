package obs.ontologyAccess.bean;

/**
 * This class is a JavaBean representation of a light ontology bean returned by BioPortal.
 * This bean are created by hitting the URL: 
 * http://rest.bioontology.org/bioportal/rest/ontologies <br>
 * 
 * This URL returns only the latest version of the ontology in BioPortal.
 *    
 * @author Clement Jonquet
 * @version 1.0
 * @created 27-Oct-2008
 */
public class BioPortalLightOntologyBean {

	private Integer id;
	private Integer ontologyId;
	private String versionNumber;
	private String displayLabel;
	private Integer statusId;
	private String format;
	
	public BioPortalLightOntologyBean(Integer id, String displayLabel,
			Integer ontologyId, String versionNumber, Integer statusId,
			String format) {
		super();
		this.id = id;
		this.displayLabel = displayLabel;
		this.ontologyId = ontologyId;
		this.versionNumber = versionNumber;
		this.statusId = statusId;
		this.format = format;
	}

	public Integer getId() {
		return id;
	}
	
	public String getDisplayLabel() {
		return displayLabel;
	}
	
	public Integer getOntologyId() {
		return ontologyId;
	}
	
	public String getVersionNumber() {
		return versionNumber;
	}
	
	public Integer getStatusId() {
		return statusId;
	}
	
	public String getFormat() {
		return format;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("BioPortalLightOntologyBean [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\tid = ").append(this.id).append(TAB)
	        .append("\tdisplayLabel = ").append(this.displayLabel).append(TAB)
	        .append("\tontologyId = ").append(this.ontologyId).append(TAB)
	        .append("\tversionNumber = ").append(this.versionNumber).append(TAB)
	        .append("\tstatusId = ").append(this.statusId).append(TAB)
	        .append("\tformat = ").append(this.format).append(TAB)
	        .append("]");
	    return retValue.toString();
	}
	
	
	
}
