package net.svcret.ejb.ejb;

import static net.svcret.ejb.model.entity.InvocationStatsIntervalEnum.*;

import java.net.HttpCookie;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import net.svcret.admin.shared.enm.ResponseTypeEnum;
import net.svcret.admin.shared.model.DtoStickySessionUrlBinding;
import net.svcret.admin.shared.model.StatusEnum;
import net.svcret.ejb.Messages;
import net.svcret.ejb.api.HttpResponseBean;
import net.svcret.ejb.api.HttpResponseBean.Failure;
import net.svcret.ejb.api.IBroadcastSender;
import net.svcret.ejb.api.IConfigService;
import net.svcret.ejb.api.IDao;
import net.svcret.ejb.api.IRuntimeStatus;
import net.svcret.ejb.api.IServiceRegistry;
import net.svcret.ejb.api.InvocationResponseResultsBean;
import net.svcret.ejb.api.UrlPoolBean;
import net.svcret.ejb.ex.ProcessingException;
import net.svcret.ejb.model.entity.BasePersInvocationStats;
import net.svcret.ejb.model.entity.BasePersInvocationStatsPk;
import net.svcret.ejb.model.entity.BasePersServiceVersion;
import net.svcret.ejb.model.entity.BasePersStats;
import net.svcret.ejb.model.entity.BasePersStatsPk;
import net.svcret.ejb.model.entity.IThrottleable;
import net.svcret.ejb.model.entity.InvocationStatsIntervalEnum;
import net.svcret.ejb.model.entity.PersConfig;
import net.svcret.ejb.model.entity.PersHttpClientConfig;
import net.svcret.ejb.model.entity.PersInvocationMethodSvcverStatsPk;
import net.svcret.ejb.model.entity.PersInvocationMethodUserStatsPk;
import net.svcret.ejb.model.entity.PersInvocationUrlStats;
import net.svcret.ejb.model.entity.PersInvocationUrlStatsPk;
import net.svcret.ejb.model.entity.PersNodeStats;
import net.svcret.ejb.model.entity.PersServiceVersionMethod;
import net.svcret.ejb.model.entity.PersServiceVersionResource;
import net.svcret.ejb.model.entity.PersServiceVersionStatus;
import net.svcret.ejb.model.entity.PersServiceVersionUrl;
import net.svcret.ejb.model.entity.PersServiceVersionUrlStatus;
import net.svcret.ejb.model.entity.PersStaticResourceStats;
import net.svcret.ejb.model.entity.PersStaticResourceStatsPk;
import net.svcret.ejb.model.entity.PersStickySessionUrlBinding;
import net.svcret.ejb.model.entity.PersStickySessionUrlBindingPk;
import net.svcret.ejb.model.entity.PersUser;
import net.svcret.ejb.model.entity.PersUserMethodStatus;
import net.svcret.ejb.model.entity.PersUserStatus;
import net.svcret.ejb.util.Validate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.google.common.annotations.VisibleForTesting;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class RuntimeStatusBean implements IRuntimeStatus {

	private static final int MAX_STATS_TO_FLUSH_AT_ONCE = 100;
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(RuntimeStatusBean.class);

	private final ReentrantLock myCollapseLock = new ReentrantLock();

	@EJB
	private IConfigService myConfigSvc;

	@EJB
	private IDao myDao;

	private ReentrantLock myFlushLock = new ReentrantLock();
	private AtomicLong myUnflushedNodeSuccessMethodInvocations=new AtomicLong();
	private AtomicLong myUnflushedNodeFaultMethodInvocations=new AtomicLong();
	private AtomicLong myUnflushedNodeFailMethodInvocations=new AtomicLong();
	private AtomicLong myUnflushedNodeSecFailMethodInvocations=new AtomicLong();
	private Date myNodeStatisticsDate;
	private final ReentrantLock myNodeStatisticsLock = new ReentrantLock();
	private Date myNowForUnitTests;
	private Map<PersStickySessionUrlBindingPk, PersStickySessionUrlBinding> myStickySessionUrlBindings;
	private final DateFormat myTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
	private final ConcurrentHashMap<BasePersStatsPk<?, ?>, BasePersStats<?, ?>> myUnflushedInvocationStats;
	private final ConcurrentHashMap<Long, PersServiceVersionStatus> myUnflushedServiceVersionStatus;
	private final ConcurrentHashMap<PersUser, PersUserStatus> myUnflushedUserStatus;

	private final ConcurrentHashMap<Long, PersServiceVersionUrlStatus> myUrlStatus;

	public RuntimeStatusBean() {
		myUnflushedInvocationStats = new ConcurrentHashMap<BasePersStatsPk<?, ?>, BasePersStats<?, ?>>();
		myUnflushedServiceVersionStatus = new ConcurrentHashMap<Long, PersServiceVersionStatus>();
		myUnflushedUserStatus = new ConcurrentHashMap<PersUser, PersUserStatus>();
		myUrlStatus = new ConcurrentHashMap<Long, PersServiceVersionUrlStatus>();
		myStickySessionUrlBindings = new HashMap<PersStickySessionUrlBindingPk, PersStickySessionUrlBinding>();
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	@Override
	public UrlPoolBean buildUrlPool(BasePersServiceVersion theServiceVersion, Map<String, List<String>> theRequestHeaders) throws ProcessingException {
		UrlPoolBean retVal = new UrlPoolBean();

		PersHttpClientConfig clientConfig = theServiceVersion.getHttpClientConfig();
		switch (clientConfig.getUrlSelectionPolicy()) {
		case PREFER_LOCAL:
			choseUrlPreferLocal(theServiceVersion, retVal);
			break;
		case ROUND_ROBIN:
			chooseUrlRoundRobin(theServiceVersion, retVal);
			break;
		case RR_STICKY_SESSION:
			chooseUrlRoundRobin(theServiceVersion, retVal);
			shuffleUrlPoolBasedOnStickySessionPolicy(retVal, theRequestHeaders, theServiceVersion);
			break;
		}

		return retVal;
	}

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	@Override
	public void collapseStats() throws ProcessingException {
		/*
		 * Make sure the flush only happens once per minute
		 */
		if (!myCollapseLock.tryLock()) {
			return;
		}
		try {
			doCollapseStats();
		} finally {
			myCollapseLock.unlock();
		}

	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void flushStatus() {

		/*
		 * Make sure the flush only happens once at a time
		 */
		if (!myFlushLock.tryLock()) {
			return;
		}
		try {
			doFlushStatus();
		} finally {
			myFlushLock.unlock();
		}

	}

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	@Override
	public void recordInvocationMethod(Date theInvocationTime, int theRequestLengthChars, PersServiceVersionMethod theMethod, PersUser theUser, HttpResponseBean theHttpResponse,
			InvocationResponseResultsBean theInvocationResponseResultsBean, Long theThrottleFullIfAny) throws ProcessingException {
		Validate.notNull(theInvocationTime, "InvocationTime");
		Validate.notNull(theMethod, "Method");
		Validate.notNull(theInvocationResponseResultsBean, "InvocationResponseResults");

		ourLog.trace("Going to record method invocation");

		switch (theInvocationResponseResultsBean.getResponseType()) {
		case FAIL:
			myUnflushedNodeFailMethodInvocations.incrementAndGet();
			break;
		case FAULT:
			myUnflushedNodeFaultMethodInvocations.incrementAndGet();
			break;
		case SECURITY_FAIL:
			myUnflushedNodeSecFailMethodInvocations.incrementAndGet();
			break;
		case SUCCESS:
			myUnflushedNodeSuccessMethodInvocations.incrementAndGet();
			break;
		case THROTTLE_REJ:
			break;
		}
		
		/*
		 * Record method statistics
		 */
		InvocationStatsIntervalEnum interval = MINUTE;
		PersInvocationMethodSvcverStatsPk statsPk = new PersInvocationMethodSvcverStatsPk(interval, theInvocationTime, theMethod);
		doRecordInvocationMethod(theRequestLengthChars, theHttpResponse, theInvocationResponseResultsBean, statsPk, theThrottleFullIfAny);

		/*
		 * Record user/anon method statistics
		 */
		if (theUser != null) {
			PersInvocationMethodUserStatsPk uStatsPk = new PersInvocationMethodUserStatsPk(interval, theInvocationTime, theMethod, theUser);
			doRecordInvocationMethod(theRequestLengthChars, theHttpResponse, theInvocationResponseResultsBean, uStatsPk, theThrottleFullIfAny);

			doUpdateUserStatus(theMethod, theInvocationResponseResultsBean, theUser, theInvocationTime);
		}

		if (theHttpResponse != null) {
			/*
			 * Record URL status for successful URLs
			 */
			PersServiceVersionUrl successfulUrl = theHttpResponse.getSuccessfulUrl();
			if (successfulUrl != null) {
				PersServiceVersionUrlStatus status = getUrlStatus(successfulUrl);
				boolean wasFault = theInvocationResponseResultsBean.getResponseType() == ResponseTypeEnum.FAULT;
				ourLog.debug("Recording successful invocation (fault={}) for URL {}/{}", new Object[] { wasFault, successfulUrl.getPid(), successfulUrl.getUrlId() });

				String message;
				if (wasFault) {
					message = Messages.getString("RuntimeStatusBean.faultUrl", theHttpResponse.getResponseTime(), theInvocationResponseResultsBean.getResponseFaultCode(),
							theInvocationResponseResultsBean.getResponseFaultDescription());
				} else {
					message = Messages.getString("RuntimeStatusBean.successfulUrl", theHttpResponse.getResponseTime());
				}
				doRecordUrlStatus(true, wasFault, status, message, theHttpResponse.getContentType(), theHttpResponse.getCode());

				// Handle sticky sessions if needed
				BasePersServiceVersion svcVer = theMethod.getServiceVersion();
				PersHttpClientConfig clientCfg = svcVer.getHttpClientConfig();
				switch (clientCfg.getUrlSelectionPolicy()) {
				case PREFER_LOCAL:
				case ROUND_ROBIN:
					break;
				case RR_STICKY_SESSION:
					synchronized (myStickySessionUrlBindings) {

						Map<String, List<String>> headers = theInvocationResponseResultsBean.getResponseHeaders();
						PersHttpClientConfig clientConfig = svcVer.getHttpClientConfig();
						if (StringUtils.isNotBlank(clientConfig.getStickySessionHeaderForSessionId())) {
							List<String> sessionKeyValues = headers.get(clientConfig.getStickySessionHeaderForSessionId());
							if (sessionKeyValues.size() > 0) {
								updateStickySession(sessionKeyValues.get(0), svcVer, successfulUrl);
							}
						} else if (StringUtils.isNotBlank(clientConfig.getStickySessionCookieForSessionId())) {
							List<String> cookieHeaders = headers.get("Set-Cookie");
							if (cookieHeaders != null) {
								for (String nextCookieHeader : cookieHeaders) {
									List<HttpCookie> cookies = HttpCookie.parse(nextCookieHeader);
									for (HttpCookie nextCookie : cookies) {
										if (nextCookie.getName().equals(clientConfig.getStickySessionCookieForSessionId())) {
											updateStickySession(nextCookie.getValue(), svcVer, successfulUrl);
											break;
										}
									}
								}
							}
						}

					}
					break;
				}

			}

			/*
			 * Recurd URL status for any failed URLs
			 */
			Map<PersServiceVersionUrl, Failure> failedUrlsMap = theHttpResponse.getFailedUrls();
			for (Entry<PersServiceVersionUrl, Failure> nextFailedUrlEntry : failedUrlsMap.entrySet()) {
				PersServiceVersionUrl nextFailedUrl = nextFailedUrlEntry.getKey();
				Failure failure = nextFailedUrlEntry.getValue();
				PersServiceVersionUrlStatus failedStatus = getUrlStatus(nextFailedUrl);
				doRecordUrlStatus(false, false, failedStatus, failure.getExplanation(), failure.getContentType(), failure.getStatusCode());
			}

			/*
			 * Record URL stats
			 */
			doRecordInvocationForUrls(theRequestLengthChars, theHttpResponse, theInvocationResponseResultsBean, theInvocationTime);
		}

		/*
		 * Record Service Version status
		 */
		PersServiceVersionStatus serviceVersionStatus = theMethod.getServiceVersion().getStatus();
		serviceVersionStatus = getStatusForPk(serviceVersionStatus, serviceVersionStatus.getPid());

		switch (theInvocationResponseResultsBean.getResponseType()) {
		case SUCCESS:
			serviceVersionStatus.setLastSuccessfulInvocation(theInvocationTime);
			break;
		case SECURITY_FAIL:
			serviceVersionStatus.setLastServerSecurityFailure(theInvocationTime);
			break;
		case FAIL:
			serviceVersionStatus.setLastFailInvocation(theInvocationTime);
			break;
		case FAULT:
			serviceVersionStatus.setLastFaultInvocation(theInvocationTime);
			break;
		case THROTTLE_REJ:
			serviceVersionStatus.setLastThrottleReject(theInvocationTime);
			break;
		}

	}

	private void updateStickySession(String theSessionId, BasePersServiceVersion theSvcVer, PersServiceVersionUrl theSuccessfulUrl) throws ProcessingException {
		if (StringUtils.isBlank(theSessionId)) {
			return;
		}
		PersStickySessionUrlBindingPk pk = new PersStickySessionUrlBindingPk(theSessionId, theSvcVer);
		PersStickySessionUrlBinding existing = myStickySessionUrlBindings.get(pk);
		if (existing == null) {
			existing = myDao.getOrCreateStickySessionUrlBinding(pk, theSuccessfulUrl);
			if (existing.isNewlyCreated()) {
				myBroadcastSender.notifyNewStickySession(existing);
			}
		}

		if (!existing.getUrl().equals(theSuccessfulUrl)) {
			ourLog.debug("Changing sticky session URL binding for session {} to {}", theSessionId, theSuccessfulUrl.getPid());
			existing.setUrl(theSuccessfulUrl);
			myDao.saveStickySessionUrlBinding(existing);
			myBroadcastSender.notifyNewStickySession(existing);
		}

	}

	@EJB
	private IBroadcastSender myBroadcastSender;

	@TransactionAttribute(TransactionAttributeType.NEVER)
	@Override
	public void recordInvocationStaticResource(Date theInvocationTime, PersServiceVersionResource theResource) {
		Validate.notNull(theInvocationTime, "InvocationTime");
		Validate.notNull(theResource, "ServiceVersionResource");

		InvocationStatsIntervalEnum interval = MINUTE;

		PersStaticResourceStatsPk statsPk = new PersStaticResourceStatsPk(interval, theInvocationTime, theResource);
		PersStaticResourceStats stats = getStatsForPk(statsPk);

		stats.addAccess();
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override
	public void recordNodeStatistics() {
		if (!myNodeStatisticsLock.tryLock()) {
			return;
		}
		try {
			ourLog.debug("Recording node statistics");

			Date date = InvocationStatsIntervalEnum.MINUTE.truncate(new Date());
			if (date.equals(myNodeStatisticsDate)) {
				return;
			}

			PersNodeStats stats = new PersNodeStats(InvocationStatsIntervalEnum.MINUTE, date, myConfigSvc.getNodeId());
			stats.collectMemoryStats();

			stats.addMethodInvocations(myUnflushedNodeSuccessMethodInvocations.getAndSet(0),myUnflushedNodeFaultMethodInvocations.getAndSet(0),myUnflushedNodeFailMethodInvocations.getAndSet(0),myUnflushedNodeSecFailMethodInvocations.getAndSet(0));
			
			myDao.saveInvocationStats(Collections.singletonList(stats));

			myNodeStatisticsDate = date;
		} finally {
			myNodeStatisticsLock.unlock();
		}
	}

	@Override
	public void recordUrlFailure(PersServiceVersionUrl theUrl, Failure theFailure) {
		Validate.notNull(theUrl, "Url");
		Validate.notNull(theFailure, "Failure");

		PersServiceVersionUrlStatus status = getUrlStatus(theUrl);
		status.setLastFail(new Date());
		status.setLastFailContentType(theFailure.getContentType());
		status.setLastFailMessage(theFailure.getExplanation());
		status.setLastFailStatusCode(theFailure.getStatusCode());

		// Do this last since it triggers a state change
		status.setStatus(StatusEnum.DOWN);
	}

	@Override
	public void reloadUrlStatus(Long thePid) {
		PersServiceVersionUrl url = myDao.getServiceVersionUrlByPid(thePid);

		PersServiceVersionUrlStatus inMemory = getUrlStatus(url);
		PersServiceVersionUrlStatus fromDisk = url.getStatus();

		inMemory.mergeNewer(fromDisk);

	}

	private void chooseUrlRoundRobin(BasePersServiceVersion theServiceVersion, UrlPoolBean theRetVal) {
		AtomicInteger counter = theServiceVersion.getUrlCounter();

		int startIndex = counter.getAndIncrement();
		if (startIndex > 10000) {
			counter.set(0);
		}

		startIndex = startIndex % theServiceVersion.getUrls().size();

		List<PersServiceVersionUrl> urls = new LinkedList<PersServiceVersionUrl>();
		urls.add(theServiceVersion.getUrls().get(startIndex));
		for (int count = startIndex + 1; count < theServiceVersion.getUrls().size(); count++) {
			urls.add(theServiceVersion.getUrls().get(count));
		}
		for (int count = 0; count < startIndex; count++) {
			urls.add(theServiceVersion.getUrls().get(count));
		}

		for (Iterator<PersServiceVersionUrl> iter = urls.iterator(); iter.hasNext();) {
			PersServiceVersionUrl next = iter.next();
			PersServiceVersionUrlStatus status = getUrlStatus(next);
			if (status != null && status.getStatus() == StatusEnum.DOWN) {
				if (theRetVal.getPreferredUrl() == null) {
					if (status.attemptToResetCircuitBreaker()) {
						theRetVal.setPreferredUrl(next);
						iter.remove();
					} else {
						iter.remove();
						continue;
					}
				} else {
					iter.remove();
					continue;
				}
			}

		}

		if (theRetVal.getPreferredUrl() == null && urls.size() > 0) {
			theRetVal.setPreferredUrl(urls.remove(0));
		}

		theRetVal.setAlternateUrls(urls);

	}

	private void choseUrlPreferLocal(BasePersServiceVersion theServiceVersion, UrlPoolBean retVal) {
		List<PersServiceVersionUrl> urls = new ArrayList<PersServiceVersionUrl>(theServiceVersion.getUrls().size());
		retVal.setAlternateUrls(urls);

		List<PersServiceVersionUrl> urlsWithDownFirstThenLocal = new ArrayList<PersServiceVersionUrl>();
		LinkedList<PersServiceVersionUrl> allUrlsCopy = new LinkedList<PersServiceVersionUrl>(theServiceVersion.getUrls());

		for (Iterator<PersServiceVersionUrl> iter = allUrlsCopy.iterator(); iter.hasNext();) {
			PersServiceVersionUrl next = iter.next();
			PersServiceVersionUrlStatus urlStatus = getUrlStatus(next);
			if (urlStatus.getStatus() == StatusEnum.DOWN) {
				urlsWithDownFirstThenLocal.add(next);
				iter.remove();
			}
		}
		for (Iterator<PersServiceVersionUrl> iter = allUrlsCopy.iterator(); iter.hasNext();) {
			PersServiceVersionUrl next = iter.next();
			if (next.isLocal()) {
				urlsWithDownFirstThenLocal.add(next);
				iter.remove();
			}
		}
		for (Iterator<PersServiceVersionUrl> iter = allUrlsCopy.iterator(); iter.hasNext();) {
			PersServiceVersionUrl next = iter.next();
			urlsWithDownFirstThenLocal.add(next);
		}

		for (PersServiceVersionUrl next : urlsWithDownFirstThenLocal) {
			PersServiceVersionUrlStatus status = getUrlStatus(next);

			if (next.isLocal() && retVal.getPreferredUrl() == null) {
				switch (status.getStatus()) {
				case ACTIVE:
				case UNKNOWN:
					if (retVal.getPreferredUrl() != null) {
						urls.add(next);
					} else {
						retVal.setPreferredUrl(next);
					}
					break;
				case DOWN:
					if (status.attemptToResetCircuitBreaker()) {
						retVal.setPreferredUrl(next);
					}
				}
			} else {
				switch (status.getStatus()) {
				case ACTIVE:
				case UNKNOWN:
					retVal.getAlternateUrls().add(next);
					break;
				case DOWN:
					if (retVal.getPreferredUrl() != null) {
						/*
						 * We don't try to reset the circuit breaker on more than one URL at a time
						 */
					} else if (status.attemptToResetCircuitBreaker()) {
						if (retVal.getPreferredUrl() != null) {
							urls.add(retVal.getPreferredUrl());
						}
						retVal.setPreferredUrl(next);
					} else {
						/*
						 * we just won't try this one of it's down and it's not time to try resetting the CB
						 */
					}
				}
			}
		}

		if (retVal.getPreferredUrl() == null && urls.size() > 0) {
			retVal.setPreferredUrl(urls.remove(0));
		}
	}

	private void doCollapseStats() throws ProcessingException {
		ourLog.debug("Doing a stats collapse pass");

		PersConfig config = myConfigSvc.getConfig();

		// TODO: collapse static resource stats

		// Days
		{
			Date daysCutoff = config.getCollapseStatsToDaysCutoff(getNow());

			ourLog.debug("Going to truncate any hourly stats before {}", daysCutoff);

			doCollapseStats(myDao.getInvocationStatsBefore(HOUR, daysCutoff), DAY);
			doCollapseStats(myDao.getInvocationUserStatsBefore(HOUR, daysCutoff), DAY);
			doCollapseStats(myDao.getInvocationUrlStatsBefore(HOUR, daysCutoff), DAY);
			doCollapseStats(myDao.getNodeStatsBefore(HOUR, daysCutoff), DAY);
		}

		// 10 Minutes -> Hours
		{
			Date hoursCutoff = config.getCollapseStatsToHoursCutoff(getNow());

			ourLog.debug("Going to truncate any 10 minute stats before {}", hoursCutoff);

			doCollapseStats(myDao.getInvocationStatsBefore(TEN_MINUTE, hoursCutoff), HOUR);
			doCollapseStats(myDao.getInvocationUserStatsBefore(TEN_MINUTE, hoursCutoff), HOUR);
			doCollapseStats(myDao.getInvocationUrlStatsBefore(TEN_MINUTE, hoursCutoff), HOUR);
			doCollapseStats(myDao.getNodeStatsBefore(TEN_MINUTE, hoursCutoff), HOUR);
		}

		// Minutes -> 10 Minutes
		{
			Date hoursCutoff = config.getCollapseStatsToTenMinutesCutoff(getNow());

			ourLog.debug("Going to truncate any 1 minute stats before {}", hoursCutoff);

			doCollapseStats(myDao.getInvocationStatsBefore(MINUTE, hoursCutoff), TEN_MINUTE);
			doCollapseStats(myDao.getInvocationUserStatsBefore(MINUTE, hoursCutoff), TEN_MINUTE);
			doCollapseStats(myDao.getInvocationUrlStatsBefore(MINUTE, hoursCutoff), TEN_MINUTE);
			doCollapseStats(myDao.getNodeStatsBefore(MINUTE, hoursCutoff), TEN_MINUTE);
		}
	}

	private <P extends BasePersStatsPk<P, O>, O extends BasePersStats<P, O>> void doCollapseStats(List<O> theList, InvocationStatsIntervalEnum toIntervalTyoe) {
		Map<P, O> statsToFlush = new HashMap<P, O>();
		List<BasePersStats<P, O>> statsToDelete = new ArrayList<BasePersStats<P, O>>();
		for (ListIterator<O> iter = theList.listIterator(); iter.hasNext();) {
			O next = iter.next();
			if (next == null) {
				continue;
			}

			P dayPk = next.getPk().newPk(toIntervalTyoe, next.getPk().getStartTime());
			if (!statsToFlush.containsKey(dayPk)) {
				statsToFlush.put(dayPk, dayPk.newObjectInstance());
			}

			O target = statsToFlush.get(dayPk);
			target.mergeUnsynchronizedEvents(next);
			statsToDelete.add(next);

			if (ourLog.isDebugEnabled()) {
				ourLog.debug("Merging stats for {} into {}", next, target);
			}

			if (statsToFlush.size() > MAX_STATS_TO_FLUSH_AT_ONCE || statsToDelete.size() > MAX_STATS_TO_FLUSH_AT_ONCE) {
				myDao.saveInvocationStats(statsToFlush.values(), statsToDelete);
				statsToDelete.clear();
				statsToFlush.clear();
			}
		}

		ourLog.trace("Deleting stats {}", statsToDelete);

		if (statsToFlush.size() > 0 || statsToDelete.size() > 0) {
			myDao.saveInvocationStats(statsToFlush.values(), statsToDelete);
		}
	}

	private void doFlushStatus() {

		ourLog.debug("Going to flush status entries");

		/*
		 * Flush method stats
		 */
		List<BasePersStats<?, ?>> stats = new ArrayList<BasePersStats<?, ?>>();
		HashSet<BasePersStatsPk<?, ?>> keys = new HashSet<BasePersStatsPk<?, ?>>();
		keys.addAll(myUnflushedInvocationStats.keySet());

		if (keys.isEmpty()) {

			ourLog.debug("No status entries to flush");

		} else {

			Date earliest = null;
			Date latest = null;
			for (BasePersStatsPk<?, ?> nextKey : keys) {
				BasePersStats<?, ?> nextStats = myUnflushedInvocationStats.remove(nextKey);
				if (nextStats == null) {
					continue;
				}
				stats.add(nextStats);

				if (earliest == null || earliest.after(nextStats.getPk().getStartTime())) {
					earliest = nextStats.getPk().getStartTime();
				}
				if (latest == null || latest.before(nextStats.getPk().getStartTime())) {
					latest = nextStats.getPk().getStartTime();
				}

			}

			ourLog.info("Going to flush {} stats entries with time range {} - {}", new Object[] { stats.size(), myTimeFormat.format(earliest), myTimeFormat.format(latest) });

			ourLog.trace("Flushing stats: {}", stats);
			for (int index = 0; index < stats.size(); index += MAX_STATS_TO_FLUSH_AT_ONCE) {
				int toIndex = Math.min(index + MAX_STATS_TO_FLUSH_AT_ONCE, stats.size());
				myDao.saveInvocationStats(stats.subList(index, toIndex));
			}
			ourLog.info("Done flushing stats");

			// } catch (PersistenceException e) {
			// ourLog.error("Failed to flush stats to disk, going to re-queue them",
			// e);
			// for (BasePersMethodStats next : stats) {
			//
			// BasePersMethodStats savedStats =
			// myUnflushedInvocationStats.putIfAbsent(next.getPk(), next);
			// if (savedStats != next) {
			// savedStats.mergeUnsynchronizedEvents(next);
			// }
			//
			// }
			// }

		}

		/*
		 * Flush URL statuses
		 */

		ArrayList<PersServiceVersionUrlStatus> urlStatuses = new ArrayList<PersServiceVersionUrlStatus>(myUrlStatus.values());
		ourLog.trace("Going to flush {} URL statuses", urlStatuses.size());

		for (Iterator<PersServiceVersionUrlStatus> iter = urlStatuses.iterator(); iter.hasNext();) {
			PersServiceVersionUrlStatus nextToMerge = iter.next();
			PersServiceVersionUrlStatus nextExisting = myDao.getServiceVersionUrlStatusByPid(nextToMerge.getPid());
			boolean toSave = nextToMerge.mergeNewer(nextExisting);
			if (!toSave) {
				ourLog.trace("Not saving URL status {} because it has no new values", nextToMerge.getPid());
				iter.remove();
			} else {
				ourLog.trace("Going to save URL status {} to DB", nextToMerge.getPid());
			}
		}

		if (!urlStatuses.isEmpty()) {
			ourLog.info("Going to persist {} URL statuses", urlStatuses.size());
			myDao.saveServiceVersionUrlStatus(urlStatuses);
		}

		/*
		 * Flush Service Version Status
		 */

		// TODO: replace this with method status
		ArrayList<PersServiceVersionStatus> serviceVersionStatuses = new ArrayList<PersServiceVersionStatus>(myUnflushedServiceVersionStatus.values());
		for (Iterator<PersServiceVersionStatus> iter = serviceVersionStatuses.iterator(); iter.hasNext();) {
			PersServiceVersionStatus next = iter.next();
			if (!next.isDirty()) {
				iter.remove();
			} else {
				next.setLastSave(new Date());
				next.setDirty(false);
			}
		}

		if (!serviceVersionStatuses.isEmpty()) {
			ourLog.info("Going to persist {} URL statuses", serviceVersionStatuses.size());
			myDao.saveServiceVersionStatuses(serviceVersionStatuses);
		}

		/*
		 * Flush user status
		 */

		List<PersUserStatus> userStatuses = new ArrayList<PersUserStatus>();
		for (IThrottleable next : new HashSet<PersUser>(myUnflushedUserStatus.keySet())) {
			PersUserStatus nextStatus = myUnflushedUserStatus.remove(next);
			userStatuses.add(nextStatus);
		}
		if (userStatuses.size() > 0) {
			myDao.saveUserStatus(userStatuses);
		}

		/*
		 * Flush sticky sessions
		 */
		{
			Date cutoff = new Date(System.currentTimeMillis() - DateUtils.MILLIS_PER_HOUR);
			Collection<PersStickySessionUrlBinding> allStickySessions = myDao.getAllStickySessions();
			for (PersStickySessionUrlBinding nextExisting : allStickySessions) {

				PersStickySessionUrlBinding inMemory = null;
				synchronized (myStickySessionUrlBindings) {
					inMemory = myStickySessionUrlBindings.get(nextExisting.getPk());
				}

				if (inMemory != null) {
					if (inMemory.getLastAccessed().before(cutoff)) {
						synchronized (myStickySessionUrlBindings) {
							myStickySessionUrlBindings.remove(nextExisting.getPk());
						}
					} else if (inMemory.getLastAccessed().after(nextExisting.getLastAccessed())) {
						nextExisting.setLastAccessed(inMemory.getLastAccessed());
						nextExisting.setUrl(inMemory.getUrl());
						nextExisting.setRequestingIp(inMemory.getRequestingIp());
						ourLog.debug("Updating sticky session '{}'", nextExisting);
						myDao.saveStickySessionUrlBinding(nextExisting);
					}
				}

				if (nextExisting.getLastAccessed().before(cutoff)) {
					ourLog.debug("Deleting expired sticky session '{}'", nextExisting);
					myDao.deleteStickySession(nextExisting);
				}
			}
		}
	}

	private void doRecordInvocationForUrls(int theRequestLengthChars, HttpResponseBean theHttpResponse, InvocationResponseResultsBean theInvocationResponseResultsBean, Date theInvocationTime) {
		if (theHttpResponse == null) {
			return;
		}

		PersServiceVersionUrl success = theHttpResponse.getSuccessfulUrl();
		if (success != null) {

			PersInvocationUrlStatsPk statsPk = new PersInvocationUrlStatsPk(MINUTE, theInvocationTime, success.getPid());
			PersInvocationUrlStats stats = getStatsForPk(statsPk);
			long responseTime = theHttpResponse.getResponseTime();
			int responseBytes = theHttpResponse.getBody().length();
			switch (theInvocationResponseResultsBean.getResponseType()) {
			case FAULT:
				stats.addFaultInvocation(responseTime, theRequestLengthChars, responseBytes);
				break;
			case SUCCESS:
				stats.addSuccessInvocation(responseTime, theRequestLengthChars, responseBytes);
				break;
			default:
				break;
			}
		}

		for (Entry<PersServiceVersionUrl, Failure> next : theHttpResponse.getFailedUrls().entrySet()) {
			PersServiceVersionUrl nextUrl = next.getKey();
			Failure nextFailure = next.getValue();

			PersInvocationUrlStatsPk statsPk = new PersInvocationUrlStatsPk(MINUTE, theInvocationTime, nextUrl.getPid());
			PersInvocationUrlStats stats = getStatsForPk(statsPk);
			long responseTime = nextFailure.getInvocationMillis();
			int responseBytes = nextFailure.getBody() != null ? nextFailure.getBody().length() : 0;
			stats.addFailInvocation(responseTime, theRequestLengthChars, responseBytes);
		}

	}

	private <P extends BasePersInvocationStatsPk<P, O>, O extends BasePersInvocationStats<P, O>> void doRecordInvocationMethod(int theRequestLengthChars, HttpResponseBean theHttpResponse,
			InvocationResponseResultsBean theInvocationResponseResultsBean, P theStatsPk, Long theThrottleFullIfAny) {
		Validate.notNull(theInvocationResponseResultsBean.getResponseType(), "responseType");

		O stats = getStatsForPk(theStatsPk);

		if (theThrottleFullIfAny != null && theThrottleFullIfAny > 0) {
			stats.addThrottleAccept(theThrottleFullIfAny);
		}

		long responseTime;
		long responseBytes;
		if (theHttpResponse != null) {
			if (theHttpResponse.getBody() == null) {
				throw new NullPointerException("HTTP Response is null");
			}
			responseTime = theHttpResponse.getResponseTime();
			responseBytes = theHttpResponse.getBody().length();
		} else {
			responseTime = 0;
			responseBytes = 0;
		}
		switch (theInvocationResponseResultsBean.getResponseType()) {
		case FAIL:
			stats.addFailInvocation(responseTime, theRequestLengthChars, responseBytes);
			break;
		case FAULT:
			stats.addFaultInvocation(responseTime, theRequestLengthChars, responseBytes);
			break;
		case SUCCESS:
			stats.addSuccessInvocation(responseTime, theRequestLengthChars, responseBytes);
			break;
		case SECURITY_FAIL:
			stats.addServerSecurityFailInvocation();
			break;
		case THROTTLE_REJ:
			stats.addThrottleReject();
			break;
		}
	}

	private void doRecordUrlStatus(boolean theWasSuccess, boolean theWasFault, PersServiceVersionUrlStatus theUrlStatusBean, String theMessage, String theContentType, int theResponseCode) {
		if (theUrlStatusBean.getUrl() == null) {
			throw new IllegalArgumentException("Status has no URL associated with it");
		}

		synchronized (theUrlStatusBean) {
			Date now = new Date();

			if (theWasSuccess) {

				if (theUrlStatusBean.getStatus() != StatusEnum.ACTIVE) {
					Long urlPid = theUrlStatusBean.getUrl().getPid();
					StatusEnum urlStatus = theUrlStatusBean.getStatus();
					String urlUrl = theUrlStatusBean.getUrl().getUrl();
					ourLog.info("URL[{}] is now ACTIVE, was {} - {}", new Object[] { urlPid, urlStatus, urlUrl });
				}
				theUrlStatusBean.setStatus(StatusEnum.ACTIVE);

				if (theWasFault) {
					theUrlStatusBean.setLastFault(now);
					theUrlStatusBean.setLastFaultMessage(theMessage);
					theUrlStatusBean.setLastFaultContentType(theContentType);
					theUrlStatusBean.setLastFaultStatusCode(theResponseCode);
				} else {
					theUrlStatusBean.setLastSuccess(now);
					theUrlStatusBean.setLastSuccessMessage(theMessage);
					theUrlStatusBean.setLastSuccessContentType(theContentType);
					theUrlStatusBean.setLastSuccessStatusCode(theResponseCode);
				}

			} else {

				theUrlStatusBean.setStatus(StatusEnum.DOWN);
				theUrlStatusBean.setLastFail(now);
				theUrlStatusBean.setLastFailMessage(theMessage);
				theUrlStatusBean.setLastFailContentType(theContentType);
				theUrlStatusBean.setLastFailStatusCode(theResponseCode);

				Date nextReset = theUrlStatusBean.getNextCircuitBreakerReset();
				if (nextReset != null) {
					ourLog.info("URL[{}] is DOWN, Next circuit breaker reset attempt is {} - {}", new Object[] { theUrlStatusBean.getUrl().getPid(), myTimeFormat.format(nextReset),
							theUrlStatusBean.getUrl().getUrl() });
				} else {
					ourLog.info("URL[{}] is DOWN - {}", new Object[] { theUrlStatusBean.getUrl().getPid(), theUrlStatusBean.getUrl().getUrl() });
				}

			}

		}
	}

	private void doUpdateUserStatus(PersServiceVersionMethod theMethod, InvocationResponseResultsBean theInvocationResponseResultsBean, PersUser theUser, Date theTransactionTime) {
		PersUserStatus status = getUserStatusForUser(theUser);

		PersUserMethodStatus methodStatus = status.getOrCreateUserMethodStatus(theMethod);

		switch (theInvocationResponseResultsBean.getResponseType()) {
		case SUCCESS:
			status.setLastAccessIfNewer(theTransactionTime);
			methodStatus.setLastSuccessfulInvocationIfNewer(theTransactionTime);
			break;
		case FAULT:
			status.setLastAccessIfNewer(theTransactionTime);
			methodStatus.setLastFaultInvocationIfNewer(theTransactionTime);
			break;
		case SECURITY_FAIL:
			status.setLastSecurityFailIfNewer(theTransactionTime);
			methodStatus.setLastSecurityFailInvocationIfNewer(theTransactionTime);
			break;
		case FAIL:
			status.setLastAccessIfNewer(theTransactionTime);
			methodStatus.setLastFailInvocationIfNewer(theTransactionTime);
			break;
		case THROTTLE_REJ:
			methodStatus.setLastThrottleRejectIfNewer(theTransactionTime);
			break;
		}

	}

	private Date getNow() {
		if (myNowForUnitTests != null) {
			return myNowForUnitTests;
		}
		return new Date();
	}

	private <P extends BasePersStatsPk<P, O>, O extends BasePersStats<P, O>> O getStatsForPk(P statsPk) {
		O tryNew = statsPk.newObjectInstance();
		@SuppressWarnings("unchecked")
		O stats = (O) myUnflushedInvocationStats.putIfAbsent(statsPk, tryNew);
		if (stats == null) {
			stats = tryNew;
		}

		if (ourLog.isTraceEnabled()) {
			ourLog.trace("Now have the following {} stats: {}", myUnflushedInvocationStats.size(), new ArrayList<BasePersStatsPk<?, ?>>(myUnflushedInvocationStats.keySet()));
		}

		return stats;
	}

	private PersServiceVersionStatus getStatusForPk(PersServiceVersionStatus theServiceVersionStatus, Long thePid) {
		Validate.notNull(theServiceVersionStatus, "Status");
		Validate.notNull(thePid, "PID");

		PersServiceVersionStatus status = myUnflushedServiceVersionStatus.putIfAbsent(thePid, theServiceVersionStatus);
		if (status == null) {
			status = theServiceVersionStatus;
		}

		return status;
	}

	private PersServiceVersionUrlStatus getUrlStatus(PersServiceVersionUrl theSuccessfulUrl) {
		PersServiceVersionUrlStatus savedStatus = theSuccessfulUrl.getStatus();
		assert savedStatus != null;

		PersServiceVersionUrlStatus existing = myUrlStatus.putIfAbsent(savedStatus.getPid(), savedStatus);
		if (existing == null) {
			return savedStatus;
		} else {
			return existing;
		}
	}

	private PersUserStatus getUserStatusForUser(PersUser theUser) {
		PersUserStatus tryNew = theUser.getStatus();
		PersUserStatus status = myUnflushedUserStatus.putIfAbsent(theUser, tryNew);
		if (status == null) {
			status = tryNew;
		}

		return status;
	}

	private void shuffleUrlPoolBasedOnStickySessionPolicy(String theSesionId, UrlPoolBean theUrlPool, Map<String, List<String>> theHeaders, BasePersServiceVersion theServiceVersion)
			throws ProcessingException {
		if (StringUtils.isBlank(theSesionId)) {
			return;
		}

		PersStickySessionUrlBinding binding = myStickySessionUrlBindings.get(theSesionId);
		if (binding == null) {
			PersStickySessionUrlBindingPk bindingPk = new PersStickySessionUrlBindingPk(theSesionId, theServiceVersion);
			binding = myDao.getOrCreateStickySessionUrlBinding(bindingPk, theUrlPool.getPreferredUrl());
			if (binding.isNewlyCreated()) {
				myBroadcastSender.notifyNewStickySession(binding);
			}
			myStickySessionUrlBindings.put(bindingPk, binding);
		}

		binding.setLastAccessed(new Date());

		if (binding.getUrl().equals(theUrlPool.getPreferredUrl())) {
			return;
		}

		if (!theUrlPool.getAlternateUrls().contains(binding.getUrl())) {
			ourLog.warn("Can't apply sticky session '{}' because URL {} is not in the target pool", theSesionId, binding.getUrl().getPid());
			return;
		}

		theUrlPool.getAlternateUrls().remove(binding.getUrl());
		theUrlPool.setPreferredUrl(binding.getUrl());

	}

	private void shuffleUrlPoolBasedOnStickySessionPolicy(UrlPoolBean theUrlPool, Map<String, List<String>> theHeaders, BasePersServiceVersion theSvcVer) throws ProcessingException {
		synchronized (myStickySessionUrlBindings) {

			PersHttpClientConfig clientConfig = theSvcVer.getHttpClientConfig();
			if (StringUtils.isNotBlank(clientConfig.getStickySessionHeaderForSessionId())) {
				List<String> sessionKeyValues = theHeaders.get(clientConfig.getStickySessionHeaderForSessionId());
				if (sessionKeyValues.size() > 0) {
					shuffleUrlPoolBasedOnStickySessionPolicy(sessionKeyValues.get(0), theUrlPool, theHeaders, theSvcVer);
				}
			} else if (StringUtils.isNotBlank(clientConfig.getStickySessionCookieForSessionId())) {
				List<String> cookieHeaders = theHeaders.get("Cookie");
				if (cookieHeaders != null) {
					for (String nextCookieHeader : cookieHeaders) {
						List<HttpCookie> cookies = HttpCookie.parse(nextCookieHeader);
						for (HttpCookie nextCookie : cookies) {
							if (nextCookie.getName().equals(clientConfig.getStickySessionCookieForSessionId())) {
								shuffleUrlPoolBasedOnStickySessionPolicy(nextCookie.getValue(), theUrlPool, theHeaders, theSvcVer);
								break;
							}
						}
					}
				}
			}

		}
	}

	void setConfigSvc(IConfigService theConfigSvc) {
		myConfigSvc = theConfigSvc;
	}

	/**
	 * FOR UNIT TESTS ONLY
	 */
	void setDao(IDao thePersistence) {
		myDao = thePersistence;
	}

	void setNowForUnitTests(Date theNow) {
		myNowForUnitTests = theNow;
	}

	@EJB
	private IServiceRegistry myServiceRegistry;

	@Override
	public void updatedStickySessionBinding(DtoStickySessionUrlBinding theBinding) {
		BasePersServiceVersion svcVer = myServiceRegistry.getServiceVersionByPid(theBinding.getServiceVersionPid());
		if (svcVer == null) {
			ourLog.warn("Unknown service version from update: {}", theBinding.getServiceVersionPid());
			return;
		}

		PersServiceVersionUrl url = svcVer.getUrlWithPid(theBinding.getUrlPid());
		if (url == null) {
			ourLog.warn("Unknown URL from update: {}", theBinding.getServiceVersionPid());
			return;
		}

		synchronized (myStickySessionUrlBindings) {
			PersStickySessionUrlBindingPk pk = new PersStickySessionUrlBindingPk(theBinding.getSessionId(), svcVer);
			PersStickySessionUrlBinding existing = myStickySessionUrlBindings.get(pk);
			if (existing != null) {
				existing.setUrl(url);
			} else {
				myStickySessionUrlBindings.put(pk, new PersStickySessionUrlBinding(pk, url));
			}
		}
	}

	@VisibleForTesting
	 void setBroadcastSender(IBroadcastSender theBroadcastSender) {
		myBroadcastSender=theBroadcastSender;
	}

}
