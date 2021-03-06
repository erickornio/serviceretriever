package net.svcret.admin.client.ui.dash.model;

import java.util.Date;

import net.svcret.admin.client.MyResources;
import net.svcret.admin.client.ui.components.IProvidesTooltip;
import net.svcret.admin.shared.DateUtil;
import net.svcret.admin.shared.model.BaseDtoDashboardObject;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public final class UsageSparklineTooltipProvider<T extends BaseDtoDashboardObject> implements IProvidesTooltip<T> {

	@Override
	public Widget getTooltip(BaseDtoDashboardObject theObject) {
		FlowPanel retVal = new FlowPanel();
		SafeHtmlBuilder b = new SafeHtmlBuilder();
		if (theObject.getName() != null) {
			b.appendEscaped(theObject.getName());
			b.appendHtmlConstant("<br/>");
		}
		b.appendHtmlConstant("Usage in last 60 minutes");
		HTML header = new HTML(b.toSafeHtml());
		header.addStyleName(MyResources.CSS.usageTooltipTableHeaderLabel());
		retVal.add(header);

		FlexTable grid = new FlexTable();
		grid.addStyleName(MyResources.CSS.usageTooltipTable());
		retVal.add(grid);

		grid.setText(0, 0, "");
		grid.setText(0, 1, "Successful Invocations");
		grid.getFlexCellFormatter().addStyleName(0, 1, MyResources.CSS.usageTooltipTableSuccessColumn());

		grid.setText(0, 2, "Fault Invocations");
		grid.getFlexCellFormatter().addStyleName(0, 2, MyResources.CSS.usageTooltipTableFaultColumn());

		grid.setText(0, 3, "Failed Invocations");
		grid.getFlexCellFormatter().addStyleName(0, 3, MyResources.CSS.usageTooltipTableFailColumn());

		grid.setText(0, 4, "Security Failures");
		grid.getFlexCellFormatter().addStyleName(0, 4, MyResources.CSS.usageTooltipTableSecFailColumn());

		grid.setText(0, 5, "Avg Latency");
		grid.getFlexCellFormatter().addStyleName(0, 5, MyResources.CSS.usageTooltipTableLatencyColumn());

		Date nextDate = theObject.getStatistics60MinuteFirstDate();
		int row = 1;
		int incrementMins = 10;
		for (int i = 0; i < 60; i += incrementMins, row++) {

			int success = 0;
			int fault = 0;
			int fail = 0;
			int secFail = 0;
			int latency = 0;
			for (int j = i; j < i + incrementMins; j++) {
				success += theObject.getTransactions60mins()[j];
				fault += theObject.getTransactionsFault60mins()[j];
				fail += theObject.getTransactionsFail60mins()[j];
				secFail += theObject.getTransactionsSecurityFail60mins()[j];
				latency += theObject.getLatency60mins()[j];
			}
			
			success = fixIntegerValue(success);
			fault = fixIntegerValue(fault);
			fail = fixIntegerValue(fail);
			secFail= fixIntegerValue(secFail);
			
			latency = latency / incrementMins;
			
			Date nextEndDate = new Date(nextDate.getTime() + ((incrementMins - 1) * DateUtil.MILLIS_PER_MINUTE));
			grid.setText(row, 0, DateUtil.formatTimeOnly(nextDate) + " - " + DateUtil.formatTimeOnly(nextEndDate));
			grid.getFlexCellFormatter().addStyleName(row, 0, MyResources.CSS.usageTooltipTableDateColumn());

			grid.setText(row, 1, Integer.toString(success));
			grid.getFlexCellFormatter().addStyleName(row, 1, MyResources.CSS.usageTooltipTableValueColumn());

			grid.setText(row, 2, Integer.toString(fault));
			grid.getFlexCellFormatter().addStyleName(row, 2, MyResources.CSS.usageTooltipTableValueColumn());

			grid.setText(row, 3, Integer.toString(fail));
			grid.getFlexCellFormatter().addStyleName(row, 3, MyResources.CSS.usageTooltipTableValueColumn());

			grid.setText(row, 4, Integer.toString(secFail));
			grid.getFlexCellFormatter().addStyleName(row, 4, MyResources.CSS.usageTooltipTableValueColumn());

			grid.setText(row, 5, Integer.toString(latency)+"ms");
			grid.getFlexCellFormatter().addStyleName(row, 5, MyResources.CSS.usageTooltipTableValueColumn());

			nextDate = new Date(nextDate.getTime() + (incrementMins * 60 * 1000L));
		}

		return retVal;
	}

	private int fixIntegerValue(int theVal) {
		// This is a fix for Javascript NaN which result from
		// unitialized array values
		if (!(theVal > 0)) {
			return 0;
		}
		return theVal;
	}
}
