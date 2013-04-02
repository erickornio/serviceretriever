package net.svcret.admin.shared.model;

public class GServiceVersionUrl extends BaseGObject<GServiceVersionUrl> {

	private static final long serialVersionUID = 1L;
	
	private String myId;
	private String myUrl;
	private transient boolean myEditMode;

	/**
	 * @return the editMode
	 */
	public boolean isEditMode() {
		return myEditMode;
	}

	/**
	 * @param theEditMode the editMode to set
	 */
	public void setEditMode(boolean theEditMode) {
		myEditMode = theEditMode;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return myId;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return myUrl;
	}

	@Override
	public void merge(GServiceVersionUrl theObject) {
		setPid(theObject.getPid());
		setId(theObject.getId());
		setUrl(theObject.getUrl());
	}

	/**
	 * @param theId the id to set
	 */
	public void setId(String theId) {
		myId = theId;
	}

	/**
	 * @param theUrl the url to set
	 */
	public void setUrl(String theUrl) {
		myUrl = theUrl;
	}

}