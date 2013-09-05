package net.svcret.admin.client.ui.config.svcver;

import static net.svcret.admin.client.AdminPortal.IMAGES;
import static net.svcret.admin.client.AdminPortal.MSGS;

import java.util.Date;
import java.util.List;

import net.svcret.admin.client.AdminPortal;
import net.svcret.admin.client.ui.components.CssConstants;
import net.svcret.admin.client.ui.components.EditableField;
import net.svcret.admin.client.ui.components.FlowPanelWithTooltip;
import net.svcret.admin.client.ui.components.HtmlBr;
import net.svcret.admin.client.ui.components.HtmlLabel;
import net.svcret.admin.client.ui.components.IProvidesTooltip;
import net.svcret.admin.client.ui.components.PButton;
import net.svcret.admin.client.ui.dash.model.BaseDashModel;
import net.svcret.admin.client.ui.stats.DateUtil;
import net.svcret.admin.shared.Model;
import net.svcret.admin.shared.enm.ResponseTypeEnum;
import net.svcret.admin.shared.model.BaseGServiceVersion;
import net.svcret.admin.shared.model.GServiceVersionUrl;
import net.svcret.admin.shared.model.ModelUpdateRequest;
import net.svcret.admin.shared.model.ModelUpdateResponse;
import net.svcret.admin.shared.util.StringUtil;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class UrlGrid extends FlowPanel {
	private static final int NUM_COLS = 6;
	private static final int COL_URL_ID = 1;
	private static final int COL_URL_URL = 2;
	private static final int COL_URL_STATUS = 3;
	private static final int COL_URL_USAGE = 4;
	private static final int COL_URL_LAST_TRANSACTION = 5;

	private Grid myUrlGrid;
	private BaseGServiceVersion myServiceVersion;
	private Label myNoUrlsLabel;

	public UrlGrid(BaseGServiceVersion theServiceVersion) {
		init(theServiceVersion);
	}

	public UrlGrid() {
	}

	public void init(BaseGServiceVersion theServiceVersion) {
		myServiceVersion = theServiceVersion;

		this.add(new Label("Each proxied service will have one or more implementation URLs. " + "When a client attempts to invoke a service that has been proxied, the ServiceProxy will "
				+ "forward this request to one of these implementations. Specifying more than one " + "implementation URL means that if one is unavailable, another can be tried (i.e. redundancy)."));

		myUrlGrid = new Grid(1, NUM_COLS);
		myUrlGrid.addStyleName(CssConstants.PROPERTY_TABLE);
		this.add(myUrlGrid);

		// myUrlGrid.setWidget(0, 0, new Label("Action"));
		myUrlGrid.setWidget(0, COL_URL_ID, new Label("ID"));
		myUrlGrid.setWidget(0, COL_URL_URL, new Label("URL"));
		myUrlGrid.setWidget(0, COL_URL_STATUS, new Label("Status"));
		myUrlGrid.setWidget(0, COL_URL_USAGE, new Label("60 Min Activity"));
		myUrlGrid.setWidget(0, COL_URL_LAST_TRANSACTION, new Label("Last Transaction"));

		myNoUrlsLabel = new Label("No URLs Defined");
		this.add(myNoUrlsLabel);

		this.add(new HtmlBr());

		PButton addButton = new PButton(IMAGES.iconAdd(), MSGS.actions_Add());
		this.add(addButton);
		HtmlLabel addNameLabel = new HtmlLabel("URL:", "addUrlTb");
		this.add(addNameLabel);
		final TextBox addText = new TextBox();
		addText.getElement().setId("addUrlTb");
		this.add(addText);
		addButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent theEvent) {
				String urlText = addText.getValue();
				if (StringUtil.isBlank(urlText)) {
					Window.alert("Please enter a URL");
					addText.setFocus(true);
					return;
				}
				if (myServiceVersion.hasUrlWithName(urlText)) {
					Window.alert("Duplicate URL: " + urlText);
					return;
				}

				GServiceVersionUrl url = new GServiceVersionUrl();
				url.setUncommittedSessionId(BaseDetailPanel.newUncommittedSessionId());
				url.setUrl(urlText);

				myServiceVersion.getUrlList().add(url);

				for (int urlNum = myServiceVersion.getUrlList().size();; urlNum++) {
					String name = "url" + urlNum;
					if (myServiceVersion.getUrlList().getUrlWithId(name) == null) {
						url.setId(name);
						break;
					}
				}

				updateUrlPanel();

				addText.setValue("");
			}
		});

		updateUrlPanel();
	}

	public BaseGServiceVersion getServiceVersion() {
		return myServiceVersion;
	}

	private final class MyPanel extends FlowPanelWithTooltip implements IProvidesTooltip {
		private ResponseTypeEnum myResponseType;
		private GServiceVersionUrl myStatus;

		private MyPanel(GServiceVersionUrl theStatus, ResponseTypeEnum theResponseType) {
			setTooltipProvider(this);
			myResponseType = theResponseType;
			myStatus = theStatus;

			String message = null;
			switch (myResponseType) {
			case FAIL:
				message = "Failed to Invoke: " + DateUtil.formatTimeElapsedForLastInvocation(myStatus.getStatsLastFailure());
				break;
			case FAULT:
				message = "Fault Response: " + DateUtil.formatTimeElapsedForLastInvocation(myStatus.getStatsLastFault());
				break;
			case SECURITY_FAIL:
			case THROTTLE_REJ:
				throw new IllegalArgumentException();
			case SUCCESS:
				message = "Success: " + DateUtil.formatTimeElapsedForLastInvocation(myStatus.getStatsLastSuccess());
				break;
			}

			Label label = new Label(message);
			label.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
			add(label);

			Image image = new Image(AdminPortal.IMAGES.iconI16());
			image.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
			add(image);

			getElement().getStyle().setWhiteSpace(WhiteSpace.NOWRAP);
		}

		@Override
		public String getTooltip() {
			String message = null;
			Date date = null;
			String desc = null;

			switch (myResponseType) {
			case FAIL:
				message = myStatus.getStatsLastFailureMessage();
				date = myStatus.getStatsLastFailure();
				desc = "failure";
				break;
			case FAULT:
				message = myStatus.getStatsLastFaultMessage();
				date = myStatus.getStatsLastFault();
				desc = "fault";
				break;
			case SECURITY_FAIL:
			case THROTTLE_REJ:
				// not applicable
				break;
			case SUCCESS:
				message = myStatus.getStatsLastSuccessMessage();
				date = myStatus.getStatsLastSuccess();
				desc = "success";
				break;
			}

			if (message != null || date != null) {
				return "Last " + desc + "<br/><ul><li>" + "<b>Date:</b> " + DateUtil.formatTime(date) + "</li><li>Message: " + message + "</li></ul>";
			}

			return null;
		}
	}

	private final class UrlEditButtonPanel extends FlowPanel implements ClickHandler {
		private PButton myDeleteButton;
		private GServiceVersionUrl myUrl;

		private UrlEditButtonPanel(GServiceVersionUrl theUrl) {
			myUrl = theUrl;

			myDeleteButton = new PButton(IMAGES.iconRemove());
			myDeleteButton.addClickHandler(this);
			add(myDeleteButton);
		}

		@Override
		public void onClick(ClickEvent theEvent) {
			PButton source = (PButton) theEvent.getSource();
			if (source == myDeleteButton) {
				if (Window.confirm("Delete - Are you sure?")) {
					myServiceVersion.getUrlList().remove(myUrl);
					updateUrlPanel();
				}
			}

			source.setEnabled(false);
		}
	}

	protected void updateUrlPanel() {
		if (myServiceVersion.getPidOrNull() == null) {
			doUpdateUrlPanel();
			return;
		}

		ModelUpdateRequest request = new ModelUpdateRequest();
		for (GServiceVersionUrl next : myServiceVersion.getUrlList()) {
			if (next.getPidOrNull() != null) {
				request.addUrlToLoadStats(next.getPidOrNull());
			}
		}

		AsyncCallback<ModelUpdateResponse> callback = new AsyncCallback<ModelUpdateResponse>() {

			@Override
			public void onSuccess(ModelUpdateResponse theResult) {
				myServiceVersion = theResult.getDomainList().getServiceVersionByPid(myServiceVersion.getPid());
				doUpdateUrlPanel();
			}

			@Override
			public void onFailure(Throwable theCaught) {
				Model.handleFailure(theCaught);
			}
		};
		AdminPortal.MODEL_SVC.loadModelUpdate(request, callback);

	}

	private void doUpdateUrlPanel() {
		myUrlGrid.resize(myServiceVersion.getUrlList().size() + 1, NUM_COLS);

		int row = 0;

		for (final GServiceVersionUrl next : myServiceVersion.getUrlList()) {
			row++;

			myUrlGrid.setWidget(row, 0, new UrlEditButtonPanel(next));

			EditableField idField = new EditableField();
			idField.setMultiline(false);
			idField.setTransparent(true);
			idField.setProcessHtml(false);
			idField.setValue(next.getId());
			if (StringUtil.isBlank(next.getId())) {
				idField.setEditorMode();
			}
			idField.addValueChangeHandler(new ValueChangeHandler<String>() {
				@Override
				public void onValueChange(ValueChangeEvent<String> theEvent) {
					next.setId(theEvent.getValue());
				}
			});
			myUrlGrid.setWidget(row, COL_URL_ID, idField);

			EditableField urlField = new EditableField();
			urlField.setMultiline(false);
			urlField.setTransparent(true);
			urlField.setProcessHtml(false);
			urlField.setMaxFieldWidth(180);
			urlField.setLabelIsPlainText(true);
			urlField.setShowTooltip(true);
			urlField.setValue(next.getUrl());
			if (StringUtil.isBlank(next.getId())) {
				urlField.setEditorMode();
			}
			urlField.addValueChangeHandler(new ValueChangeHandler<String>() {
				@Override
				public void onValueChange(ValueChangeEvent<String> theEvent) {
					next.setUrl(theEvent.getValue());
				}
			});
			myUrlGrid.setWidget(row, COL_URL_URL, urlField);

			// Status
			if (next.isStatsInitialized() == false) {
				myUrlGrid.setWidget(row, COL_URL_STATUS, null);
				myUrlGrid.setWidget(row, COL_URL_LAST_TRANSACTION, null);
			} else {

				// Status
				{
					FlowPanel statusPanel = new FlowPanel();
					statusPanel.setStyleName(CssConstants.UNSTYLED_TABLE);
					statusPanel.getElement().getStyle().setTextAlign(com.google.gwt.dom.client.Style.TextAlign.CENTER);
					Widget imageForStatus = BaseDashModel.returnImageForStatus(next.getStatus());
					imageForStatus.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
					statusPanel.add(imageForStatus);
					String text = null;
					PButton cbResetButton = null;
					switch (next.getStatus()) {
					case ACTIVE:
						text = "Ok";
						break;
					case DOWN:
						if (next.getNextCircuitBreakerReset() != null) {
							text = "Down (next circuit breaker reset at " + DateUtil.formatTimeOnly(next.getNextCircuitBreakerReset()) + ")";
							cbResetButton = new PButton("Reset CB");
							cbResetButton.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
						} else {
							text = "Down";
						}
						break;
					case UNKNOWN:
						text = "Unknown (no requests)";
						break;
					}
					Label urlStatusLabel = new Label(text, true);
					urlStatusLabel.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
					statusPanel.add(urlStatusLabel);

					if (cbResetButton != null) {
						statusPanel.add(cbResetButton);
					}

					myUrlGrid.setWidget(row, COL_URL_STATUS, statusPanel);
				}

				// Usage
				{
					FlowPanel grid = new FlowPanel();
					myUrlGrid.setWidget(row, COL_URL_USAGE, grid);

					Widget transactionsSparkline = BaseDashModel.returnSparklineFor60MinsUsage(next.getTransactions60mins(), next.getStatsInitialized(), next.getAverageTransactionsPerMin60min(),
							next.getMaxTransactionsPerMin60min());
					transactionsSparkline.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
					grid.add(transactionsSparkline);

					Widget latencySparkline = BaseDashModel.returnSparklineFor60minsLatency(next.getLatency60mins(), next.getStatsInitialized(), next.getAverageLatency60min(),
							next.getMaxLatency60min());
					if (latencySparkline != null) {
						latencySparkline.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
						grid.add(latencySparkline);
					}
				}

				// Last transaction
				{
					FlowPanel lastXPanel = new FlowPanel();

					List<ResponseTypeEnum> lastResponseTypes = next.getStatsLastResponseTypesFromMostRecentToLeast();
					if (lastResponseTypes.isEmpty()) {
						lastXPanel.add(new Label("No usage"));
					} else {
						for (ResponseTypeEnum lastResponseType : lastResponseTypes) {
							lastXPanel.add(new MyPanel(next, lastResponseType));
						}
					}

					myUrlGrid.setWidget(row, COL_URL_LAST_TRANSACTION, lastXPanel);
				}
			}

		}

		myNoUrlsLabel.setVisible(myServiceVersion.getUrlList().size() == 0);
	}

}
