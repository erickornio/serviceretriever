package net.svcret.admin.shared.model;

import java.util.Date;

public class GRecentMessage extends BaseGObject<GRecentMessage> {

	private static final long serialVersionUID = 1L;

	private AuthorizationOutcomeEnum myAuthorizationOutcome;
	private String myImplementationUrl;
	private String myRequestHostIp;
	private String myRequestMessage;
	private String myRequestUsername;

	private Long myRequestUserPid;

	private String myResponseMessage;

	private long myTransactionMillis;

	private Date myTransactionTime;

	public GRecentMessage() {
		super();
	}

	public GRecentMessage(long thePid, Date theDate, String theUrl, String theRequestHostIp, String theRequestMessage, String theResponseMessage) {
		setPid(thePid);
		myTransactionTime = theDate;
		myImplementationUrl = theUrl;
		myRequestHostIp = theRequestHostIp;
		myRequestMessage = theRequestMessage;
		myResponseMessage = theResponseMessage;
	}

	/**
	 * @return the implementationUrl
	 */
	public String getImplementationUrl() {
		return myImplementationUrl;
	}

	/**
	 * @return the requestHostIp
	 */
	public String getRequestHostIp() {
		return myRequestHostIp;
	}

	public String getRequestMessage() {
		return myRequestMessage;
	}

	/**
	 * @return the requestUsername
	 */
	public String getRequestUsername() {
		return myRequestUsername;
	}

	/**
	 * @return the authorizationOutcome
	 */
	public AuthorizationOutcomeEnum getAuthorizationOutcome() {
		return myAuthorizationOutcome;
	}

	/**
	 * @return the requestUserPid
	 */
	public Long getRequestUserPid() {
		return myRequestUserPid;
	}

	/**
	 * @return the responseMessage
	 */
	public String getResponseMessage() {
		return myResponseMessage;
	}

	/**
	 * @return the transactionMillis
	 */
	public long getTransactionMillis() {
		return myTransactionMillis;
	}

	/**
	 * @return the transactionTime
	 */
	public Date getTransactionTime() {
		return myTransactionTime;
	}

	@Override
	public void merge(GRecentMessage theObject) {

	}

	public void setAuthorizationOutcome(AuthorizationOutcomeEnum theAuthorizationOutcome) {
		myAuthorizationOutcome = theAuthorizationOutcome;
	}

	/**
	 * @param theImplementationUrl
	 *            the implementationUrl to set
	 */
	public void setImplementationUrl(String theImplementationUrl) {
		myImplementationUrl = theImplementationUrl;
	}

	/**
	 * @param theRequestHostIp
	 *            the requestHostIp to set
	 */
	public void setRequestHostIp(String theRequestHostIp) {
		myRequestHostIp = theRequestHostIp;
	}

	/**
	 * @param theRequestMessage
	 *            the requestMessage to set
	 */
	public void setRequestMessage(String theRequestMessage) {
		myRequestMessage = theRequestMessage;
	}

	public void setRequestUsername(String theUsername) {
		myRequestUsername = theUsername;
	}

	public void setRequestUserPid(Long thePid) {
		myRequestUserPid = thePid;
	}

	/**
	 * @param theResponseMessage
	 *            the responseMessage to set
	 */
	public void setResponseMessage(String theResponseMessage) {
		myResponseMessage = theResponseMessage;
	}

	public void setTransactionMillis(long theTransactionMillis) {
		myTransactionMillis = theTransactionMillis;
	}

	/**
	 * @param theTransactionTime
	 *            the transactionTime to set
	 */
	public void setTransactionTime(Date theTransactionTime) {
		myTransactionTime = theTransactionTime;
	}

}
