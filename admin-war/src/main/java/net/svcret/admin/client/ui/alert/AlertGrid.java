package net.svcret.admin.client.ui.alert;

import java.util.Date;
import java.util.List;

import net.svcret.admin.client.AdminPortal;
import net.svcret.admin.client.ui.stats.DateUtil;
import net.svcret.admin.shared.Model;
import net.svcret.admin.shared.model.GDomainList;
import net.svcret.admin.shared.model.GMonitorRuleFiring;
import net.svcret.admin.shared.model.GMonitorRuleFiringProblem;
import net.svcret.admin.shared.model.GServiceVersionUrl;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;

public class AlertGrid extends FlowPanel {

	private GDomainList myDomainList;

	public AlertGrid(GDomainList theDomainList, Long theDomainPid, Long theServicePid, Long theServiceVersionPid) {
		myDomainList = theDomainList;

//		DataGrid<GMonitorRuleFiring> grid = new DataGrid<GMonitorRuleFiring>();
		final CellTable<GMonitorRuleFiring> grid = new CellTable<GMonitorRuleFiring>();
		add(grid);
		
		grid.setEmptyTableWidget(new Label("No firings"));

		// Create a Pager to control the table.
		SimplePager.Resources pagerResources = GWT.create(SimplePager.Resources.class);
		SimplePager pager = new SimplePager(TextLocation.CENTER, pagerResources, false, 0, true);
		pager.setDisplay(grid);
		pager.setPageSize(5);
		add(pager);
		
		// Add a selection model so we can select cells.
		// final SelectionModel<GMonitorRuleFiring> selectionModel =
		// new
		// MultiSelectionModel<GMonitorRuleFiring>(ContactDatabase.ContactInfo.KEY_PROVIDER);
		// dataGrid.setSelectionModel(selectionModel,
		// DefaultSelectionEventManager
		// .<ContactInfo> createCheckboxManager());

		Column<GMonitorRuleFiring, SafeHtml> startedColumn = new Column<GMonitorRuleFiring, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(GMonitorRuleFiring theObject) {
				Date time = theObject.getStartDate();
				return SafeHtmlUtils.fromTrustedString(DateUtil.formatTimeElapsedForMessage(time));
			}
		};
		grid.addColumn(startedColumn, "Problem Started");

		Column<GMonitorRuleFiring, SafeHtml> endedColumn = new Column<GMonitorRuleFiring, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(GMonitorRuleFiring theObject) {
				Date time = theObject.getEndDate();
				if (time==null) {
					return SafeHtmlUtils.fromTrustedString("Still active");
				}
				return SafeHtmlUtils.fromTrustedString(DateUtil.formatTimeElapsedForMessage(time));
			}
		};
		grid.addColumn(endedColumn, "Problem Ended");

		Column<GMonitorRuleFiring, SafeHtml> problemsColumn = new Column<GMonitorRuleFiring, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(GMonitorRuleFiring theObject) {
				SafeHtmlBuilder b = new SafeHtmlBuilder();

				if (theObject.getProblems().size() == 0) {
					b.appendHtmlConstant("No problems associated with this firing");
				} else if (theObject.getProblems().size() == 1) {
					append(b, theObject.getProblems().get(0));
				}

				return b.toSafeHtml();
			}
		};
		grid.addColumn(problemsColumn, "Issue(s) Detected");

		final ListDataProvider<GMonitorRuleFiring> dp = new ListDataProvider<GMonitorRuleFiring>();
		dp.addDataDisplay(grid);
		
		int start = 0;
		AdminPortal.MODEL_SVC.loadMonitorRuleFirings(theDomainPid, theServicePid, theServiceVersionPid, start, new AsyncCallback<List<GMonitorRuleFiring>>() {

			@Override
			public void onFailure(Throwable theCaught) {
				Model.handleFailure(theCaught);
			}

			@Override
			public void onSuccess(List<GMonitorRuleFiring> theResult) {
				dp.getList().addAll(theResult);
				dp.refresh();
				
				if (theResult.size() == 0) {
					grid.setRowCount(dp.getList().size(), true);
				}else {
					grid.setRowCount(dp.getList().size(), false);
				}
				
			}
		});
	}

	protected void append(SafeHtmlBuilder theB, GMonitorRuleFiringProblem theProblem) {
		if (theProblem.getFailedLatencyAverageMillisPerCall() != null) {
			theB.appendHtmlConstant("Latency ");
			theB.append(theProblem.getFailedLatencyAverageMillisPerCall());
			theB.appendHtmlConstant("ms/call (Threshold ");
			theB.append(theProblem.getFailedLatencyThreshold());
			theB.appendHtmlConstant("ms/call)");
			if (theProblem.getFailedLatencyAverageOverMinutes() != null) {
				theB.appendHtmlConstant(" over ");
				theB.append(theProblem.getFailedLatencyAverageOverMinutes());
				theB.appendHtmlConstant(" minutes");
			}
		}

		if (theProblem.getFailedUrlPid() != null) {
			GServiceVersionUrl url = myDomainList.getUrlByPid(theProblem.getFailedUrlPid());
			theB.appendHtmlConstant("URL ");
			theB.appendHtmlConstant("<a href=\"");
			theB.appendHtmlConstant(url.getUrl());
			theB.appendHtmlConstant("\">");
			theB.appendEscaped(url.getId());
			theB.appendHtmlConstant("</a> failed with message: <i>");
			theB.appendHtmlConstant(theProblem.getFailedUrlMessage());
			theB.appendHtmlConstant("</i>");
		}

	}

}
