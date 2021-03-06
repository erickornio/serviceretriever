package net.svcret.admin.client.ui.alert;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.svcret.admin.client.AdminPortal;
import net.svcret.admin.client.nav.NavProcessor;
import net.svcret.admin.client.ui.components.PCellTable;
import net.svcret.admin.shared.DateUtil;
import net.svcret.admin.shared.Model;
import net.svcret.admin.shared.model.BaseDtoMonitorRule;
import net.svcret.admin.shared.model.BaseDtoServiceVersion;
import net.svcret.admin.shared.model.DtoDomain;
import net.svcret.admin.shared.model.DtoDomainList;
import net.svcret.admin.shared.model.GMonitorRuleFiring;
import net.svcret.admin.shared.model.GMonitorRuleFiringProblem;
import net.svcret.admin.shared.model.GMonitorRuleList;
import net.svcret.admin.shared.model.GService;
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

	private DtoDomainList myDomainList;
	private Long myDomainPid;
	private Long myServicePid;
	private Long myServiceVersionPid;
	private GMonitorRuleList myRuleList;

	public AlertGrid(DtoDomainList theDomainList, Long theDomainPid, Long theServicePid, Long theServiceVersionPid) {
		myDomainList = theDomainList;
		myDomainPid = theDomainPid;
		myServicePid = theServicePid;
		myServiceVersionPid = theServiceVersionPid;

		AdminPortal.MODEL_SVC.loadMonitorRuleList(new AsyncCallback<GMonitorRuleList>() {
			@Override
			public void onFailure(Throwable theCaught) {
				Model.handleFailure(theCaught);
			}

			@Override
			public void onSuccess(GMonitorRuleList theResult) {
				myRuleList = theResult;
				initUi();
			}
		});
		
	}

	private void initUi() {
		// DataGrid<GMonitorRuleFiring> grid = new DataGrid<GMonitorRuleFiring>();
		final CellTable<GMonitorRuleFiring> grid = new PCellTable<>();
		add(grid);

		grid.setEmptyTableWidget(new Label("No firings"));

		// Create a Pager to control the table.
		SimplePager.Resources pagerResources = GWT.create(SimplePager.Resources.class);
		SimplePager pager = new SimplePager(TextLocation.CENTER, pagerResources, false, 0, true);
		pager.setDisplay(grid);
		pager.setPageSize(5);
		add(pager);

		Column<GMonitorRuleFiring, SafeHtml> startedColumn = new Column<GMonitorRuleFiring, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(GMonitorRuleFiring theObject) {
				Date time = theObject.getStartDate();
				return (DateUtil.formatTimeElapsedForMessage(time));
			}
		};
		grid.addColumn(startedColumn, "Problem Started");

		Column<GMonitorRuleFiring, SafeHtml> endedColumn = new Column<GMonitorRuleFiring, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(GMonitorRuleFiring theObject) {
				Date time = theObject.getEndDate();
				if (time == null) {
					return SafeHtmlUtils.fromTrustedString("Still active");
				}
				return (DateUtil.formatTimeElapsedForMessage(time));
			}
		};
		grid.addColumn(endedColumn, "Problem Ended");

		Column<GMonitorRuleFiring, SafeHtml> ruleColumn = new Column<GMonitorRuleFiring, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(GMonitorRuleFiring theObject) {
				SafeHtmlBuilder b = new SafeHtmlBuilder();
				BaseDtoMonitorRule rule = myRuleList.getRuleByPid(theObject.getRulePid());
				if (rule!=null) {
					b.appendHtmlConstant("<a href=\"#" + NavProcessor.getTokenEditMonitorRule(theObject.getRulePid()) + "\">");
					b.appendEscaped(rule.getName());
					b.appendHtmlConstant("</a> ");
					b.appendEscaped("(" + rule.getRuleType().getFriendlyName() + ")");
				}
				return b.toSafeHtml();
			}
		};
		grid.addColumn(ruleColumn, "Rule");

		Column<GMonitorRuleFiring, SafeHtml> problemsColumn = new Column<GMonitorRuleFiring, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(GMonitorRuleFiring theObject) {
				SafeHtmlBuilder b = new SafeHtmlBuilder();

				if (theObject.getProblems().size() == 0) {
					b.appendHtmlConstant("No problems associated with this firing");
				} else if (theObject.getProblems().size() == 1) {
					append(b, theObject.getProblems().get(0));
				} else {
					b.appendHtmlConstant("<ul>");
					for (GMonitorRuleFiringProblem next : theObject.getProblems()) {
						b.appendHtmlConstant("<li>");
						append(b, next);
						b.appendHtmlConstant("</li>");
					}
					b.appendHtmlConstant("</ul>");
					
				}

				return b.toSafeHtml();
			}
			
		};
		grid.addColumn(problemsColumn, "Issue(s) Detected");

		Column<GMonitorRuleFiring, SafeHtml> affectsColumn = new Column<GMonitorRuleFiring, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(GMonitorRuleFiring theObject) {
				SafeHtmlBuilder b = new SafeHtmlBuilder();

				Set<Long> affectedSvcVerPids = new HashSet<>();
				for (GMonitorRuleFiringProblem next : theObject.getProblems()) {
					affectedSvcVerPids.add(next.getServiceVersionPid());
				}

				b.appendHtmlConstant("<ul>");

				DtoDomainList domainList = myDomainList;
				createAppliesToHtml(b, affectedSvcVerPids, domainList);

				b.appendHtmlConstant("</ul>");
				return b.toSafeHtml();
			}


		};
		grid.addColumn(affectsColumn, "Affects Services");

		final ListDataProvider<GMonitorRuleFiring> dp = new ListDataProvider<>();
		dp.addDataDisplay(grid);

		int start = 0;
		AdminPortal.MODEL_SVC.loadMonitorRuleFirings(myDomainPid, myServicePid, myServiceVersionPid, start, new AsyncCallback<List<GMonitorRuleFiring>>() {

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
				} else {
					grid.setRowCount(dp.getList().size(), false);
				}

			}
		});
		
	}
	
	private void append(SafeHtmlBuilder theB, GMonitorRuleFiringProblem theProblem) {
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

		if (theProblem.getFailedUrlMessage() != null) {
			GServiceVersionUrl url = myDomainList.getUrlByPid(theProblem.getUrlPid());
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

	
	public static void createAppliesToHtml(SafeHtmlBuilder theSafeHtmlBuilder, Set<Long> theSvcVerPids, DtoDomainList theDomainList) {
		for (DtoDomain nextDomain : theDomainList) {
			for (GService nextSvc : nextDomain.getServiceList()) {
				if (nextSvc.anyVersionPidsInThisServiceAreAmongThesePids(theSvcVerPids)) {
					theSafeHtmlBuilder.appendHtmlConstant("<li>");
					theSafeHtmlBuilder.appendHtmlConstant("<a href=\"#" + NavProcessor.getTokenEditDomain(nextDomain.getPid()) + "\">");
					theSafeHtmlBuilder.appendEscaped(nextDomain.getId());
					theSafeHtmlBuilder.appendHtmlConstant("</a> / <a href=\"#" + NavProcessor.getTokenEditService(nextDomain.getPid(), nextSvc.getPid()) + "\">");
					theSafeHtmlBuilder.appendEscaped(nextSvc.getId());
					theSafeHtmlBuilder.appendHtmlConstant("</a>");
					if (nextSvc.allVersionPidsInThisServiceAreAmongThesePids(theSvcVerPids)) {
						theSafeHtmlBuilder.appendHtmlConstant(" (all versions)");
					} else {
						theSafeHtmlBuilder.appendHtmlConstant("<ul>");
						for (BaseDtoServiceVersion nextSvcVer : nextSvc.getVersionList()) {
							theSafeHtmlBuilder.appendHtmlConstant("<li>");
							theSafeHtmlBuilder.appendHtmlConstant("<a href=\"#" + NavProcessor.getTokenEditServiceVersion(nextSvcVer.getPid()) + "\">");
							theSafeHtmlBuilder.appendEscaped(nextSvcVer.getId());
							theSafeHtmlBuilder.appendHtmlConstant("</a></li>");
						}
						theSafeHtmlBuilder.appendHtmlConstant("</ul>");
					}
				}
			}
		}
	}

}
