package net.svcret.admin.client.ui.config.svcver;

import net.svcret.admin.client.AdminPortal;
import net.svcret.admin.client.ui.components.HtmlLabel;
import net.svcret.admin.client.ui.components.LoadingSpinner;
import net.svcret.admin.client.ui.components.PButton;
import net.svcret.admin.shared.IAsyncLoadCallback;
import net.svcret.admin.shared.Model;
import net.svcret.admin.shared.model.AddServiceVersionResponse;
import net.svcret.admin.shared.model.BaseGServiceVersion;
import net.svcret.admin.shared.model.GDomain;
import net.svcret.admin.shared.model.GDomainList;
import net.svcret.admin.shared.model.GService;
import net.svcret.admin.shared.model.GServiceList;
import net.svcret.admin.shared.model.GSoap11ServiceVersion;
import net.svcret.admin.shared.util.StringUtil;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractServiceVersionPanel extends FlowPanel {

	Widget myBottomContents;
	protected FlowPanel myBottomPanel;
	private FlowPanel myContentPanel;
	private ListBox myDomainListBox;
	Long myDomainPid;
	protected LoadingSpinner myLoadingSpinner;
	private HtmlLabel myNewDomainLabel;
	private TextBox myNewDomainNameTextBox;
	private HtmlLabel myNewServiceLabel;
	private TextBox myNewServiceNameTextBox;
	protected Grid myParentsGrid;
	private ListBox myServiceListBox;
	Long myServicePid;
	private FlowPanel myTopPanel;
	protected ListBox myTypeComboBox;
	Long myUncommittedSessionId;
	private boolean myUpdating;
	BaseGServiceVersion myVersion;
	TextBox myVersionTextBox;

	public AbstractServiceVersionPanel() {
		this(null, null, null);
	}

	protected AbstractServiceVersionPanel(Long theDomainPid, Long theServicePid, Long theUncommittedSessionId) {
		myDomainPid = theDomainPid;
		myServicePid = theServicePid;
		myUncommittedSessionId = theUncommittedSessionId;

		myTopPanel = new FlowPanel();
		add(myTopPanel);

		myTopPanel.setStylePrimaryName("mainPanel");

		Label titleLabel = new Label(getDialogTitle());
		titleLabel.setStyleName("mainPanelTitle");
		myTopPanel.add(titleLabel);

		myContentPanel = new FlowPanel();
		myContentPanel.addStyleName("contentInnerPanel");
		myTopPanel.add(myContentPanel);

		Label intro = new Label(getDialogDescription());
		myContentPanel.add(intro);

		myParentsGrid = new Grid(4, 4);
		myContentPanel.add(myParentsGrid);

		/*
		 * Parents
		 */
		HtmlLabel domainLbl = new HtmlLabel("Domain", "cbDomain");
		myParentsGrid.setWidget(0, 0, domainLbl);
		myDomainListBox = new ListBox(false);
		myDomainListBox.getElement().setId("cbDomain");
		myDomainListBox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent theEvent) {
				handleDomainListChange();
			}
		});
		myParentsGrid.setWidget(0, 1, myDomainListBox);

		myNewDomainLabel = new HtmlLabel(" Name:", "cbNewDomainName");
		myParentsGrid.setWidget(0, 2, myNewDomainLabel);
		myNewDomainNameTextBox = new TextBox();
		myNewDomainNameTextBox.getElement().setId("cbNewDomainName");
		myNewDomainNameTextBox.setValue("Untitled Domain");
		myParentsGrid.setWidget(0, 3, myNewDomainNameTextBox);

		HtmlLabel svcLbl = new HtmlLabel("Service", "cbSvc");
		myParentsGrid.setWidget(1, 0, svcLbl);
		myServiceListBox = new ListBox(false);
		myServiceListBox.getElement().setId("cbSvc");
		myServiceListBox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent theEvent) {
				handleServiceListChange();
			}
		});
		myParentsGrid.setWidget(1, 1, myServiceListBox);

		myNewServiceLabel = new HtmlLabel(" Name:", "cbNewServiceName");
		myParentsGrid.setWidget(1, 2, myNewServiceLabel);
		myNewServiceNameTextBox = new TextBox();
		myNewServiceNameTextBox.getElement().setId("cbNewServiceName");
		myNewServiceNameTextBox.setValue("Untitled Service");
		myParentsGrid.setWidget(1, 3, myNewServiceNameTextBox);

		/*
		 * Version
		 */

		HtmlLabel versionLabel = new HtmlLabel("Version", "tbVer");
		myParentsGrid.setWidget(2, 0, versionLabel);
		myVersionTextBox = new TextBox();
		myVersionTextBox.setValue("1.0");
		myVersionTextBox.getElement().setId("tbVer");
		myVersionTextBox.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> theEvent) {
				if (myVersion != null) {
					myVersion.setId(myVersionTextBox.getValue());
				}
			}
		});
		myParentsGrid.setWidget(2, 1, myVersionTextBox);

		addTypeSelector();

		PButton saveButton = new PButton("Save");
		myContentPanel.add(saveButton);
		saveButton.addClickHandler(new SaveClickHandler());

		myLoadingSpinner = new LoadingSpinner();
		myContentPanel.add(myLoadingSpinner);

		/*
		 * The following panel contains the rest of the screen (i.e. no
		 * background, so that it can have lots of contents
		 */

		myBottomPanel = new FlowPanel();
		add(myBottomPanel);

	}

	protected void setServiceVersion(BaseGServiceVersion theResult) {
		myLoadingSpinner.hide();

		myVersion = theResult;
		myVersionTextBox.setValue(myVersion.getId(), false);

		switch (theResult.getProtocol()) {
		case SOAP11:
			myBottomContents = new SoapDetailPanel(AbstractServiceVersionPanel.this, (GSoap11ServiceVersion) theResult);
			break;
		}

		myBottomPanel.clear();
		myBottomPanel.add(myBottomContents);
	}

	protected abstract void addTypeSelector();

	protected abstract boolean allowTypeSelect();

	protected abstract String getDialogDescription();

	protected abstract String getDialogTitle();

	public Long getDomainPid() {
		return myDomainPid;
	}

	/**
	 * @return the servicePid
	 */
	public Long getServicePid() {
		return myServicePid;
	}

	private void handleDomainListChange() {
		if (myUpdating) {
			return;
		}

		if (myDomainListBox.getSelectedIndex() == 0) {
			myNewDomainLabel.setVisible(true);
			myNewDomainNameTextBox.setVisible(true);
		} else {
			myNewDomainLabel.setVisible(false);
			myNewDomainNameTextBox.setVisible(false);
		}

		String domainPidString = myDomainListBox.getValue(myDomainListBox.getSelectedIndex());
		if (StringUtil.isBlank(domainPidString)) {
			myDomainPid = null;
		} else {
			myDomainPid = Long.parseLong(domainPidString);
		}

		IAsyncLoadCallback<GServiceList> callback = new IAsyncLoadCallback<GServiceList>() {
			@Override
			public void onSuccess(GServiceList theResult) {
				processServiceList(theResult);
			}

		};

		if (myDomainPid != null) {
			Model.getInstance().loadServiceList(myDomainPid, callback);
		} else {
			processServiceList(new GServiceList());
		}
	}

	private void handleServiceListChange() {
		boolean showEdit = myServiceListBox.getSelectedIndex() == 0;

		myNewServiceLabel.setVisible(showEdit);
		myNewServiceNameTextBox.setVisible(showEdit);

		String pidString = myServiceListBox.getValue(myServiceListBox.getSelectedIndex());
		if (StringUtil.isBlank(pidString)) {
			myServicePid = null;
		} else {
			myServicePid = Long.parseLong(pidString);
		}

	}

	void initParents(GDomainList theDomainList) {
		myUpdating = true;

		myDomainListBox.clear();
		myDomainListBox.addItem("New...", "");
		for (GDomain nextDomain : theDomainList) {
			Long value = nextDomain.getPid();
			myDomainListBox.addItem(nextDomain.getName(), value.toString());
			if (value.equals(myDomainPid)) {
				myDomainListBox.setSelectedIndex(myDomainListBox.getItemCount() - 1);
			}
		}

		if (myDomainPid == null && myDomainListBox.getItemCount() > 1) {
			myDomainListBox.setSelectedIndex(1);
		}

		myUpdating = false;

		handleDomainListChange();

	}

	private void processServiceList(GServiceList theResult) {
		myServiceListBox.clear();
		myServiceListBox.addItem("New...", "");
		for (GService nextService : theResult) {
			Long value = nextService.getPid();
			myServiceListBox.addItem(nextService.getName(), value.toString());
			if (value.equals(myServicePid)) {
				myServiceListBox.setSelectedIndex(myServiceListBox.getSelectedIndex() - 1);
			}
		}

		if (myServicePid == null && myServiceListBox.getItemCount() > 1) {
			myServiceListBox.setSelectedIndex(1);
		}

		handleServiceListChange();
	}

	public class SaveClickHandler implements ClickHandler {

		@Override
		public void onClick(ClickEvent theEvent) {
			final String domainId;
			if (myDomainListBox.getSelectedIndex() == 0) {
				domainId = myNewDomainNameTextBox.getValue();
				if (StringUtil.isBlank(domainId)) {
					myLoadingSpinner.showMessage("Please select an existing Domain, or enter a name for a new one to be created.", false);
					myNewDomainNameTextBox.setFocus(true);
					return;
				}
			} else {
				domainId = null;
			}

			String serviceId;
			if (myServiceListBox.getSelectedIndex() == 0) {
				serviceId = myNewServiceNameTextBox.getValue();
				if (StringUtil.isBlank(serviceId)) {
					myLoadingSpinner.showMessage("Please select an existing Service, or enter a name for a new one to be created.", false);
					myNewDomainNameTextBox.setFocus(true);
					return;
				}
			} else {
				serviceId = null;
			}

			AsyncCallback<AddServiceVersionResponse> callback = new AsyncCallback<AddServiceVersionResponse>() {
				@Override
				public void onFailure(Throwable theCaught) {
					Model.handleFailure(theCaught);
				}

				@Override
				public void onSuccess(AddServiceVersionResponse theResult) {
					if (theResult.getNewDomain() != null) {
						Model.getInstance().addDomain(theResult.getNewDomain());
						myDomainPid = theResult.getNewDomain().getPid();
					}
					if (theResult.getNewService() != null) {
						Model.getInstance().addService(myDomainPid, theResult.getNewService());
						myServicePid = theResult.getNewService().getPid();
					}
					Model.getInstance().addOrUpdateServiceVersion(myDomainPid, myServicePid, theResult.getNewServiceVersion());
					handleDoneSaving(theResult);
				}
			};

			myLoadingSpinner.show();

			AdminPortal.MODEL_SVC.addServiceVersion(myDomainPid, domainId, myServicePid, serviceId, myVersion, callback);
		}


	}

	protected abstract void handleDoneSaving(AddServiceVersionResponse theResult);
}