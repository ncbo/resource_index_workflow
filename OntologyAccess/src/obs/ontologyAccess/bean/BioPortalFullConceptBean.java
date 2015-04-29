package obs.ontologyAccess.bean;

import java.util.ArrayList;

/**
 * This class is a JavaBean representation of a full concept bean returned by BioPortal.
 * This bean are created by hitting the URL: 
 * http://rest.bioontology.org/bioportal/rest/concepts/{localOntologyID}/{BPConceptID} <br>
 * 
 * For instance: http://rest.bioontology.org/bioportal/rest/concepts/38677/DOID:1909 <br>
 *   
 * We extract from that URL only the information we need to implement the BioPortalAccessTool.
 *    
 * @author Clement Jonquet 
 * @version 1.0
 * @created 27-Oct-2008 2:42:11 PM
 */
public class BioPortalFullConceptBean extends BioPortalLightConceptBean {

	private ArrayList<String> exactSynonyms;
	private ArrayList<String> narrowSynonyms;
	private ArrayList<String> broadSynonyms;
	private ArrayList<String> relatedSynonyms;
	private ArrayList<String> bpSynonyms;
	
	private ArrayList<BioPortalLightConceptBean> superClass;
	private ArrayList<BioPortalLightConceptBean> subClass;
	
	public BioPortalFullConceptBean(String conceptId, Integer ontologyId,
			String label, ArrayList<String> exactSynonyms,
			ArrayList<String> narrowSynonyms, ArrayList<String> broadSynonyms,
			ArrayList<String> relatedSynonyms, ArrayList<String> bpSynonyms, 
			ArrayList<BioPortalLightConceptBean> superClass,
			ArrayList<BioPortalLightConceptBean> subClass) {
		super(conceptId, label);
		this.exactSynonyms = exactSynonyms;
		this.narrowSynonyms = narrowSynonyms;
		this.broadSynonyms = broadSynonyms;
		this.relatedSynonyms = relatedSynonyms;
		this.bpSynonyms = bpSynonyms;
		this.superClass = superClass;
		this.subClass = subClass;
	}

	public ArrayList<String> getExactSynonyms() {
		return exactSynonyms;
	}
	
	public ArrayList<String> getNarrowSynonyms() {
		return narrowSynonyms;
	}
	
	public ArrayList<String> getBroadSynonyms() {
		return broadSynonyms;
	}
	
	public ArrayList<String> getRelatedSynonyms() {
		return relatedSynonyms;
	}

	public ArrayList<String> getBpSynonyms() {
		return bpSynonyms;
	}

	public ArrayList<BioPortalLightConceptBean> getSuperClass() {
		return superClass;
	}
	
	public ArrayList<BioPortalLightConceptBean> getSubClass() {
		return subClass;
	}

	public void setExactSynonyms(ArrayList<String> exactSynonyms) {
		this.exactSynonyms = exactSynonyms;
	}

	public void setNarrowSynonyms(ArrayList<String> narrowSynonyms) {
		this.narrowSynonyms = narrowSynonyms;
	}

	public void setBroadSynonyms(ArrayList<String> broadSynonyms) {
		this.broadSynonyms = broadSynonyms;
	}

	public void setRelatedSynonyms(ArrayList<String> relatedSynonyms) {
		this.relatedSynonyms = relatedSynonyms;
	}

	public void setBpSynonyms(ArrayList<String> bpSynonyms) {
		this.bpSynonyms = bpSynonyms;
	}

	public void setSuperClass(ArrayList<BioPortalLightConceptBean> superClass) {
		this.superClass = superClass;
	}

	public void setSubClass(ArrayList<BioPortalLightConceptBean> subClass) {
		this.subClass = subClass;
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("BioPortalFullConceptBean [ ").append(TAB)
	        .append(super.toString()).append(TAB)
	        .append("\texactSynonyms = ").append(this.exactSynonyms).append(TAB)
	        .append("\tnarrowSynonyms = ").append(this.narrowSynonyms).append(TAB)
	        .append("\tbroadSynonyms = ").append(this.broadSynonyms).append(TAB)
	        .append("\trelatedSynonyms = ").append(this.relatedSynonyms).append(TAB)
	        .append("\tbpSynonyms = ").append(this.bpSynonyms).append(TAB)
	        .append("\tsuperClass = ").append(this.superClass).append(TAB)
	        .append("\tsubClass = ").append(this.subClass).append(TAB)
	        .append("]");
	    return retValue.toString();
	}
	
}
