package obs.ontologyAccess.bean;

import java.util.Calendar;
import java.util.Date;

public class BioPortalErrorBean {

	private String accessedResource;
	private Date accessDate = Calendar.getInstance().getTime();
	private String shortMessage;
	private String longMessage;
	private String errorCode;
	
	public BioPortalErrorBean(String accessedResource, Date accessDate,
			String shortMessage, String longMessage, String errorCode) {
		super();
		this.accessedResource = accessedResource;
		this.accessDate = accessDate;
		this.shortMessage = shortMessage;
		this.longMessage = longMessage;
		this.errorCode = errorCode;
	}
	
	/**
	 * @return the accessedResource 
	 */
	public String getAccessedResource() {
		return accessedResource;
	}
	/**
	 * @param accessedResource the accessedResource to set
	 */
	public void setAccessedResource(String accessedResource) {
		this.accessedResource = accessedResource;
	}
	/**
	 * @return the accessDate
	 */
	public Date getAccessDate() {
		return accessDate;
	}
	/**
	 * @param accessDate the accessDate to set
	 */
	public void setAccessDate(Date accessDate) {
		this.accessDate = accessDate;
	}
	/**
	 * @return the shortMessage
	 */
	public String getShortMessage() {
		return shortMessage;
	}
	/**
	 * @param shortMessage the shortMessage to set
	 */
	public void setShortMessage(String shortMessage) {
		this.shortMessage = shortMessage;
	}
	/**
	 * @return the longMessage
	 */
	public String getLongMessage() {
		return longMessage;
	}
	/**
	 * @param longMessage the longMessage to set
	 */
	public void setLongMessage(String longMessage) {
		this.longMessage = longMessage;
	}
	/**
	 * @return the errorCode
	 */
	public String getErrorCode() {
		return errorCode;
	}
	/**
	 * @param errorCode the errorCode to set
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("BioPortalErrorBean [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\taccessedResource = ").append(this.accessedResource).append(TAB)
	        .append("\taccessDate = ").append(this.accessDate).append(TAB)
	        .append("\tshortMessage = ").append(this.shortMessage).append(TAB)
	        .append("\tlongMessage = ").append(this.longMessage).append(TAB)
	        .append("\terrorCode = ").append(this.errorCode).append(TAB)
	        .append("]");
	    return retValue.toString();
	}
	
	
}
