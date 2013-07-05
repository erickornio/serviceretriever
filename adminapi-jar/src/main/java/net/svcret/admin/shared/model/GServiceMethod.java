package net.svcret.admin.shared.model;

public class GServiceMethod extends BaseGDashboardObject<GServiceMethod> {

	private static final long serialVersionUID = 1L;

	private transient boolean myEditMode;
	
	private String myRootElements;
	
	public String getRootElements() {
		return myRootElements;
	}

	public void setRootElements(String theRootElements) {
		myRootElements = theRootElements;
	}

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

	@Override
	public void merge(GServiceMethod theObject) {
		myRootElements=theObject.getRootElements();
		super.merge((BaseGDashboardObject<GServiceMethod>) theObject);
	}

}
