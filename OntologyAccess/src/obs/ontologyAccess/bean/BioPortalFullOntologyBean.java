package obs.ontologyAccess.bean;

/**
 * This class is a JavaBean representation of a full ontology bean returned by BioPortal.
 * This bean are created by hitting the URL: 
 * http://rest.bioontology.org/bioportal/rest/ontologies/{localOntologyID} <br>
 * 
 * For instance: http://rest.bioontology.org/bioportal/rest/ontologies/38677 <br>
 *   
 * We extract from that URL only the information we need to implement the BioPortalAccessTool.<br>
 *       
 * @author Clement Jonquet
 * @version 1.0
 * @created 27-Oct-2008
 */
public class BioPortalFullOntologyBean extends BioPortalLightOntologyBean {

	private String oboFoundryId;
	private String contactName;
	private String contactEmail;
	private String homepage;
	private String documentation;
	
	public BioPortalFullOntologyBean(Integer id, String displayLabel,
			Integer ontologyId, String versionNumber, Integer statusId,
			String format, String oboFoundryId, String contactName,
			String contactEmail, String homepage, String documentation) {
		super(id, displayLabel, ontologyId, versionNumber, statusId, format);
		this.oboFoundryId = oboFoundryId;
		this.contactName = contactName;
		this.contactEmail = contactEmail;
		this.homepage = homepage;
		this.documentation = documentation;
	}

	public String getOboFoundryId() {
		return oboFoundryId;
	}

	public String getContactName() {
		return contactName;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public String getHomepage() {
		return homepage;
	}

	public String getDocumentation() {
		return documentation;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("BioPortalFullOntologyBean [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\toboFoundryId = ").append(this.oboFoundryId).append(TAB)
	        .append("\tcontactName = ").append(this.contactName).append(TAB)
	        .append("\tcontactEmail = ").append(this.contactEmail).append(TAB)
	        .append("\thomepage = ").append(this.homepage).append(TAB)
	        .append("\tdocumentation = ").append(this.documentation).append(TAB)
	        .append("]");
	    return retValue.toString();
	}
	
	
}
