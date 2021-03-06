package net.svcret.core.ejb.nodecomm;

import net.svcret.admin.api.UnexpectedFailureException;
import net.svcret.core.model.entity.PersStickySessionUrlBinding;

public interface IBroadcastSender {

	void notifyUserCatalogChanged() throws UnexpectedFailureException;
	
	void notifyServiceCatalogChanged() throws UnexpectedFailureException;

	void notifyConfigChanged() throws UnexpectedFailureException;

	void notifyMonitorRulesChanged() throws UnexpectedFailureException;

	void notifyUrlStatusChanged(Long thePid) throws UnexpectedFailureException;

	void notifyNewStickySession(PersStickySessionUrlBinding theExisting) throws UnexpectedFailureException;

	void requestFlushTransactionLogs();

	void requestFlushQueuedStats();
	
}
