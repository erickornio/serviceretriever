package net.svcret.admin.client.ui.dash.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.svcret.admin.client.AdminPortal;
import net.svcret.admin.client.ui.components.CssConstants;
import net.svcret.admin.client.ui.components.PButton;
import net.svcret.admin.client.ui.components.Sparkline;
import net.svcret.admin.client.ui.stats.DateUtil;
import net.svcret.admin.shared.model.BaseGDashboardObject;
import net.svcret.admin.shared.model.BaseGDashboardObjectWithUrls;
import net.svcret.admin.shared.model.StatusEnum;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class BaseDashModel implements IDashModel {

	private static NumberFormat ourDecimalFormat = NumberFormat.getFormat("0.0");

	private BaseGDashboardObject<?> myModel;

	public BaseDashModel(BaseGDashboardObject<?> theModel) {
		myModel = theModel;
	}

	@Override
	public Widget renderLastInvocation() {
		if (!(myModel instanceof BaseGDashboardObjectWithUrls<?>)) {
			return null;
		}

		Date lastInvoc = myModel.getLastSuccessfulInvocation();
		String text = DateUtil.formatTimeElapsedForLastInvocation(lastInvoc);

		Label label = new Label(text);

		if (lastInvoc != null && lastInvoc.getTime() > (System.currentTimeMillis() - DateUtil.MILLIS_PER_HOUR)) {
			label.addStyleName(CssConstants.DASHBOARD_LAST_USAGE_RECENT);
		} else {
			label.addStyleName(CssConstants.DASHBOARD_LAST_USAGE);
		}

		return label;

	}

	@Override
	public final Widget renderLatency() {
		return returnSparklineFor60minsLatency(myModel.getLatency60mins(), myModel.getStatsInitialized(), myModel.getAverageLatency60min(), myModel.getMaxLatency60min(), "ms");
	}

	static void createBackButton(final PopupPanel theActionPopup, final FlowPanel thePreviousContent, final FlowPanel content) {
		PButton backButton = new ActionPButton("Back");
		backButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent theEvent) {
				theActionPopup.remove(content);
				theActionPopup.add(thePreviousContent);
			}
		});
		content.add(backButton.toBackwardNavButtonPanel());
	}

	protected Widget renderName(String thePrefix, String theName, String thePostFix) {
		HorizontalPanel hp = new HorizontalPanel();
		hp.setStyleName(CssConstants.UNSTYLED_TABLE);
		if (thePrefix != null) {
			hp.add(new HTML(thePrefix));
		}
		hp.add(new HTML(theName));
		if (thePostFix != null) {
			hp.add(new HTML(thePostFix));
		}
		return hp;
	}

	@Override
	public Widget renderSecurity() {
		if (!(myModel instanceof BaseGDashboardObjectWithUrls<?>)) {
			return null;
		}

		ImageResource image = null;
		String text = null;
		String clazz = null;

		BaseGDashboardObjectWithUrls<?> obj = (BaseGDashboardObjectWithUrls<?>) myModel;
		switch (obj.getServerSecured()) {
		case FULLY:
			image = AdminPortal.IMAGES.dashSecure();
			text = AdminPortal.MSGS.dashboard_SecuredFully();
			clazz = CssConstants.DASHBOARD_SECURITY_PANEL;
			break;
		case PARTIALLY:
			image = AdminPortal.IMAGES.dashSecure();
			text = AdminPortal.MSGS.dashboard_SecuredPartial();
			clazz = CssConstants.DASHBOARD_SECURITY_PANEL;
			break;
		case NONE:
			text = AdminPortal.MSGS.dashboard_SecuredNot();
			clazz = CssConstants.DASHBOARD_SECURITY_PANEL_NOTSECURED;
			break;
		}

		FlowPanel retVal = new FlowPanel();
		retVal.setStyleName(clazz);

		if (image != null) {
			Image img = new Image(image);
			img.getElement().getStyle().setDisplay(Display.INLINE);
			retVal.add(img);
		}

		Label lbl = new Label(text);
		lbl.getElement().getStyle().setDisplay(Display.INLINE);
		retVal.add(lbl);

		return retVal;
	}

	@Override
	public final Widget renderUsage() {
		int[] list = myModel.getTransactions60mins();
		double averagePerMin = myModel.getAverageTransactionsPerMin60min();
		double maxPerMin = myModel.getMaxTransactionsPerMin60min();
		return returnSparklineFor60MinsUsage(list, myModel.getStatsInitialized(), averagePerMin, maxPerMin);
	}

	private static String formatDouble(double theNumber) {
		return ourDecimalFormat.format(theNumber);
	}

	private static Widget returnBarSparklineFor60mins(int[] theList, Date theStatsInitialized, String theAvgValue, String theMaxValue, String theUnitDesc) {
		if (theList == null) {
			GWT.log(new Date() + " - No 60 minutes data");
			return null;
		}
		String text = "Avg:" + theAvgValue + " Max:" + theMaxValue + " " + theUnitDesc;

		List<Long> dates = new ArrayList<Long>();
		long nextDate = theStatsInitialized.getTime() - (60 * 60 * 1000L);
		for (int i = 0; i < 60; i++) {
			dates.add(nextDate);
			nextDate += (60 * 1000L);
		}

		Sparkline retVal = new Sparkline(theList, dates, text);
		retVal.setBar(true);
		retVal.setWidth("100px");
		retVal.addStyleName(CssConstants.DASHBOARD_SPARKLINE);
		return retVal;
	}

	public static Widget returnImageForStatus(BaseGDashboardObjectWithUrls<?> theObject) {
		String text;
		ImageResource image;
		String clazz;
		if (theObject.getFailingApplicableRulePids().size() > 0) {
			image = AdminPortal.IMAGES.dashMonitorAlert();
			text = theObject.getFailingApplicableRulePids().size() + " failures!";
			clazz=CssConstants.DASHBOARD_MONITOR_FAILURES;
		} else if (theObject.getMonitorRulePids().size() > 0) {
			image = AdminPortal.IMAGES.dashMonitorOk();
			text = theObject.getMonitorRulePids().size() + " rules ok";
			clazz=CssConstants.DASHBOARD_MONITOR;
		} else {
			image = AdminPortal.IMAGES.dashMonitorNorules();
			text = "No rules";
			clazz=CssConstants.DASHBOARD_MONITOR_NORULES;
		}

		FlowPanel flowPanel = new FlowPanel();
		flowPanel.setStylePrimaryName(clazz);

		Image w = new Image(image);
		w.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
		flowPanel.add(w);

		HTML w2 = new HTML("<nobr>" + text + "</nobr>");
		w2.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
		flowPanel.add(w2);
		return flowPanel;
	}

	public static Widget returnImageForStatus(StatusEnum status) {
		if (status == null) {
			GWT.log("Status is null");
			return null;
		}

		switch (status) {
		case ACTIVE:
			return new Image("images/icon_check_16.png");
		case DOWN:
			return new Image("images/icon_warn_16.png");
		case UNKNOWN:
			return new Image("images/icon_unknown_16.png");
		}
		return null;
	}

	public static Widget returnSparklineFor60minsLatency(int[] theList, Date theStatsInitialized, int theAvgValue, int theMaxValue, String theUnitDesc) {
		if (theList == null) {
			GWT.log(new Date() + " - No 60 minutes data");
			return null;
		}

		if (theMaxValue == 0.0) {
			return null;
		}

		String text = "Avg:" + theAvgValue + " Max:" + theMaxValue + " " + theUnitDesc;

		List<Long> dates = new ArrayList<Long>();
		long nextDate = theStatsInitialized.getTime() - (60 * 60 * 1000L);
		for (int i = 0; i < 60; i++) {
			dates.add(nextDate);
			nextDate += (60 * 1000L);
		}

		Sparkline retVal = new Sparkline(theList, dates, text);
		retVal.setWidth("100px");
		retVal.addStyleName(CssConstants.DASHBOARD_SPARKLINE);
		return retVal;
	}

	public static Widget returnSparklineFor60MinsUsage(int[] list, Date theStatsInitialized, double averagePerMin, double theMaxPerMin) {
		if (theMaxPerMin == 0.0) {
			Label retVal = new Label("No usage");
			retVal.addStyleName(CssConstants.DASHBOARD_SPARKLINE_NOUSAGE);
			return retVal;
		} else if (averagePerMin < 0.1 || theMaxPerMin < 0.1) {
			return returnBarSparklineFor60mins(list, theStatsInitialized, formatDouble(averagePerMin * 60), formatDouble(theMaxPerMin * 60), "/hr");
		} else {
			return returnBarSparklineFor60mins(list, theStatsInitialized, formatDouble(averagePerMin), formatDouble(theMaxPerMin), "/min");
		}
	}

}
