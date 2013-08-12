package net.svcret.admin.shared.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class DtoLibraryMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private Set<Long> myAppliesToServiceVersionPids;
	private String myContentType;
	private String myDescription;
	private String myMessage;
	private int myMessageLength;

	private Long myPid;

	public Set<Long> getAppliesToServiceVersionPids() {
		if (myAppliesToServiceVersionPids == null) {
			myAppliesToServiceVersionPids = new HashSet<Long>();
		}
		return myAppliesToServiceVersionPids;
	}

	public String getContentType() {
		return myContentType;
	}

	public String getDescription() {
		return myDescription;
	}

	public String getMessage() {
		return myMessage;
	}

	public int getMessageLength() {
		return myMessageLength;
	}

	public Long getPid() {
		return myPid;
	}

	public void setAppliesToServiceVersionPids(Long... theAppliesToServiceVersionPids) {
		getAppliesToServiceVersionPids().clear();
		for (long l : theAppliesToServiceVersionPids) {
			getAppliesToServiceVersionPids().add(l);
		}
	}

	public void setContentType(String theContentType) {
		myContentType = theContentType;
	}

	public void setDescription(String theDescription) {
		myDescription = theDescription;
	}

	public void setMessage(String theMessage) {
		myMessage = theMessage;
	}

	public void setMessageLength(int theMessageLength) {
		myMessageLength = theMessageLength;
	}

	public void setPid(Long thePid) {
		myPid = thePid;
	}

}
