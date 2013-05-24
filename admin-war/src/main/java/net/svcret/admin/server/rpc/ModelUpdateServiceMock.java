package net.svcret.admin.server.rpc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.svcret.admin.client.rpc.ModelUpdateService;
import net.svcret.admin.shared.ServiceFailureException;
import net.svcret.admin.shared.model.AddServiceVersionResponse;
import net.svcret.admin.shared.model.BaseGAuthHost;
import net.svcret.admin.shared.model.BaseGDashboardObject;
import net.svcret.admin.shared.model.BaseGDashboardObjectWithUrls;
import net.svcret.admin.shared.model.BaseGServiceVersion;
import net.svcret.admin.shared.model.GAuthenticationHostList;
import net.svcret.admin.shared.model.GConfig;
import net.svcret.admin.shared.model.GDomain;
import net.svcret.admin.shared.model.GDomainList;
import net.svcret.admin.shared.model.GHttpClientConfig;
import net.svcret.admin.shared.model.GHttpClientConfigList;
import net.svcret.admin.shared.model.GLocalDatabaseAuthHost;
import net.svcret.admin.shared.model.GPartialUserList;
import net.svcret.admin.shared.model.GRecentMessage;
import net.svcret.admin.shared.model.GRecentMessageLists;
import net.svcret.admin.shared.model.GService;
import net.svcret.admin.shared.model.GServiceList;
import net.svcret.admin.shared.model.GServiceMethod;
import net.svcret.admin.shared.model.GServiceVersionDetailedStats;
import net.svcret.admin.shared.model.GServiceVersionUrl;
import net.svcret.admin.shared.model.GSoap11ServiceVersion;
import net.svcret.admin.shared.model.GUrlStatus;
import net.svcret.admin.shared.model.GUser;
import net.svcret.admin.shared.model.GUserDomainPermission;
import net.svcret.admin.shared.model.GUserList;
import net.svcret.admin.shared.model.ModelUpdateRequest;
import net.svcret.admin.shared.model.ModelUpdateResponse;
import net.svcret.admin.shared.model.PartialUserListRequest;
import net.svcret.admin.shared.model.ServerSecuredEnum;
import net.svcret.admin.shared.model.ServiceProtocolEnum;
import net.svcret.admin.shared.model.StatusEnum;
import net.svcret.admin.shared.model.UrlSelectionPolicy;
import net.svcret.admin.shared.model.UserGlobalPermissionEnum;
import net.svcret.admin.shared.util.StringUtil;
import net.svcret.ejb.model.entity.BasePersAuthenticationHost;

import org.apache.commons.lang3.time.DateUtils;

public class ModelUpdateServiceMock implements ModelUpdateService {

	private static long ourNextPid = 1000000L;
	private GDomainList myDomainList;
	private GHttpClientConfigList myClientConfigList;
	private GAuthenticationHostList myAuthHostList;
	private GUserList myUserList;
	private GConfig myConfig;

	public ModelUpdateServiceMock() {
		myConfig = new GConfig();
		myConfig.getProxyUrlBases().add("http://base/proxy");

		myDomainList = new GDomainList();

		GDomain dom = new GDomain();
		dom.setPid(ourNextPid++);
		dom.setId("domain1");
		dom.setName("Domain 1");
		myDomainList.add(dom);

		GService svc = new GService();
		svc.setId("svc1a");
		svc.setName("Service 1-A");
		svc.setPid(10L);
		dom.getServiceList().add(svc);

		GSoap11ServiceVersion ver = new GSoap11ServiceVersion();
		ver.setActive(true);
		ver.setWsdlLocation("http://foo");
		ver.setId("Version 1-A-1");
		ver.setPid(100L);
		ver.setName("Version 1-A-1");
		ver.setProxyPath("/some/service");
		ver.setLastAccess(new Date());
		svc.getVersionList().add(ver);

		GServiceMethod met = new GServiceMethod();
		met.setPid(1000L);
		met.setId("Method 1");
		met.setName("Method 1");
		ver.getMethodList().add(met);

		met = new GServiceMethod();
		met.setPid(1001L);
		met.setId("Method 2");
		met.setName("Method 2");
		ver.getMethodList().add(met);

		svc = new GService();
		svc.setId("svc1b");
		svc.setName("Service 1-B");
		svc.setPid(11L);
		dom.getServiceList().add(svc);

		dom = new GDomain();
		dom.setPid(2L);
		dom.setId("domain2");
		dom.setName("Domain 2");
		myDomainList.add(dom);

		myClientConfigList = new GHttpClientConfigList();
		GHttpClientConfig defCfg = new GHttpClientConfig();
		defCfg.setPid(ourNextPid++);
		defCfg.setId("DEFAULT");
		defCfg.setName("Default (Can't be mopdified)");
		defCfg.setUrlSelectionPolicy(UrlSelectionPolicy.PREFER_LOCAL);
		defCfg.setCircuitBreakerTimeBetweenResetAttempts(60000);
		defCfg.setReadTimeoutMillis(1000);
		defCfg.setConnectTimeoutMillis(2000);
		defCfg.setCircuitBreakerEnabled(true);
		defCfg.setFailureRetriesBeforeAborting(1);
		myClientConfigList.add(defCfg);

		myAuthHostList = new GAuthenticationHostList();
		GLocalDatabaseAuthHost hostList = new GLocalDatabaseAuthHost();
		hostList.setPid(ourNextPid++);
		hostList.setModuleId(BasePersAuthenticationHost.MODULE_ID_ADMIN_AUTH);
		hostList.setModuleName(BasePersAuthenticationHost.MODULE_DESC_ADMIN_AUTH);
		hostList.setSupportsPasswordChange(true);
		myAuthHostList.add(hostList);

		myUserList = new GUserList();
		GUser user = new GUser();
		user.setPid(ourNextPid++);
		user.setUsername("admin");
		user.setAuthHostPid(hostList.getPid());
		user.addGlobalPermission(UserGlobalPermissionEnum.SUPERUSER);
		user.setStatsLastAccess(new Date());
		user.setStatsSecurityFailTransactions(random60mins());
		user.setStatsSuccessTransactions(random60mins());
		user.setStatsSecurityFailTransactionsAvgPerMin(2.0);
		user.setStatsSuccessTransactionsAvgPerMin(0.01);
		user.setAllowableSourceIps(new ArrayList<String>());
		myUserList.add(user);

		user = new GUser();
		user.setPid(ourNextPid++);
		user.setUsername("testuser");
		GUserDomainPermission perm = new GUserDomainPermission();
		perm.setPid(ourNextPid++);
		perm.setAllowAllServices(true);
		user.addDomainPermission(perm);
		user.setAuthHostPid(hostList.getPid());
		user.addGlobalPermission(UserGlobalPermissionEnum.SUPERUSER);
		user.setStatsSecurityFailTransactions(random60mins());
		user.setStatsSuccessTransactions(random60mins());
		user.setStatsSecurityFailTransactionsAvgPerMin(2.0);
		user.setStatsSuccessTransactionsAvgPerMin(0.01);
		user.setAllowableSourceIps(new ArrayList<String>());
		user.getAllowableSourceIps().add("127.0.0.1");
		user.getAllowableSourceIps().add("192.168.1.1");
		myUserList.add(user);

	}

	@Override
	public ModelUpdateResponse loadModelUpdate(ModelUpdateRequest theRequest) {
		ModelUpdateResponse retVal = new ModelUpdateResponse();

		retVal.setDomainList(new GDomainList());
		retVal.getDomainList().mergeResults(myDomainList);

		for (GDomain nextDomain : retVal.getDomainList()) {
			Set<Long> domainsToLoadStats = theRequest.getDomainsToLoadStats();
			long nextDomainPid = nextDomain.getPid();
			if (domainsToLoadStats.contains(nextDomainPid)) {
				populateRandom(nextDomain);
			}
			for (GService nextService : nextDomain.getServiceList()) {
				if (theRequest.getServicesToLoadStats().contains(nextService.getPid())) {
					populateRandom(nextService);
				}
				for (BaseGServiceVersion nextVersion : nextService.getVersionList()) {
					if (theRequest.getVersionsToLoadStats().contains(nextVersion.getPid())) {
						populateRandom(nextVersion);
						for (GServiceMethod nextMethod : nextVersion.getMethodList()) {
							if (theRequest.getVersionMethodsToLoadStats().contains(nextMethod.getPid())) {
								populateRandom(nextMethod);
							}
						}
					}
				}
			}
		}

		if (theRequest.isLoadHttpClientConfigs()) {
			retVal.setHttpClientConfigList(getHttpClientConfigList());
		}

		if (theRequest.isLoadAuthHosts()) {
			retVal.setAuthenticationHostList(getAuthHostList());
		}

		return retVal;
	}

	private GAuthenticationHostList getAuthHostList() {
		GAuthenticationHostList retVal = new GAuthenticationHostList();
		retVal.mergeResults(myAuthHostList);
		return retVal;
	}

	private GHttpClientConfigList getHttpClientConfigList() {
		GHttpClientConfigList clientConfigList = new GHttpClientConfigList();
		clientConfigList.mergeResults(myClientConfigList);
		return clientConfigList;
	}

	private void populateRandom(BaseGDashboardObjectWithUrls<?> obj) {
		obj.setStatsInitialized(true);
		obj.setStatus(randomStatus());
		obj.setTransactions60mins(random60mins());
		obj.setLatency60mins(random60mins());
		obj.setUrlsActive(randomUrlNumber());
		obj.setUrlsDown(randomUrlNumber());
		obj.setUrlsUnknown(randomUrlNumber());
		obj.setLastSuccessfulInvocation(randomRecentDate());
		obj.setLastServerSecurityFailure(randomRecentDate());
		obj.setServerSecured(ServerSecuredEnum.FULLY);
	}

	private Date randomRecentDate() {
		return new Date(System.currentTimeMillis() - (long) (DateUtils.MILLIS_PER_DAY * Math.random()));
	}

	private void populateRandom(BaseGDashboardObject<?> obj) {
		obj.setStatsInitialized(true);
		obj.setStatus(randomStatus());
		obj.setTransactions60mins(random60mins());
		obj.setLatency60mins(random60mins());
	}

	private int randomUrlNumber() {
		return (int) (5.0 * Math.random());
	}

	private StatusEnum randomStatus() {
		double rnd = 3.0 * Math.random();
		if (rnd < 1) {
			return StatusEnum.ACTIVE;
		}
		if (rnd < 2) {
			return StatusEnum.DOWN;
		}
		return StatusEnum.UNKNOWN;
	}

	private int[] random60mins() {
		int[] retVal = new int[60];
		for (int i = 0; i < 60; i++) {
			retVal[i] = (int) (Math.random() * 100.0);
		}
		return retVal;
	}

	@Override
	public GDomain addDomain(GDomain theDomain) throws ServiceFailureException {
		theDomain.setPid(ourNextPid++);
		myDomainList.add(theDomain);
		return theDomain;
	}

	@Override
	public GDomain saveDomain(GDomain theDomain) {
		GDomain retVal = myDomainList.getDomainByPid(theDomain.getPid());
		retVal.setId(theDomain.getId());
		retVal.setName(theDomain.getName());
		return retVal;
	}

	@Override
	public GService addService(long theDomainPid, String theId, String theName, boolean theActive) {

		GDomain dom = myDomainList.getDomainByPid(theDomainPid);

		GService svc = new GService();
		svc.setPid(ourNextPid++);
		svc.setId(theId);
		svc.setName(theName);
		svc.setStatus(StatusEnum.ACTIVE);
		svc.setTransactions60mins(random60mins());
		svc.setLatency60mins(random60mins());
		svc.setActive(theActive);

		dom.getServiceList().add(svc);

		return svc;
	}

	@Override
	public GSoap11ServiceVersion loadWsdl(GSoap11ServiceVersion theService, String theWsdlUrl) throws ServiceFailureException {
		if (StringUtil.isBlank(theWsdlUrl)) {
			throw new ServiceFailureException("Failed to load URL: \"" + theWsdlUrl + '"');
		}

		GSoap11ServiceVersion retVal = new GSoap11ServiceVersion();
		retVal.setWsdlLocation(theWsdlUrl);

		retVal.setActive(true);
		retVal.setUncommittedSessionId(theService.getUncommittedSessionId());

		GServiceMethod method = new GServiceMethod();
		method.setId("method1");
		method.setName("method1");
		retVal.getMethodList().add(method);

		method = new GServiceMethod();
		method.setId("method2");
		method.setName("method2");
		retVal.getMethodList().add(method);

		GServiceVersionUrl url = new GServiceVersionUrl();
		url.setId("url1");
		url.setUrl("http://something/aaaa.html");
		retVal.getUrlList().add(url);

		return retVal;
	}

	@Override
	public void saveServiceVersionToSession(BaseGServiceVersion theServiceVersion) {
		throw new UnsupportedOperationException();
	}

	@Override
	public GSoap11ServiceVersion createNewServiceVersion(ServiceProtocolEnum theProtocol, Long theDomainPid, Long theServicePid, Long theUncomittedId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void reportClientError(String theMessage, Throwable theException) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AddServiceVersionResponse addServiceVersion(Long theExistingDomainPid, String theCreateDomainId, Long theExistingServicePid, String theCreateServiceId, BaseGServiceVersion theVersion) {
		GDomain dom;
		if (theExistingDomainPid != null) {
			dom = myDomainList.getDomainByPid(theExistingDomainPid);
			if (dom == null) {
				throw new NullPointerException("Unknown dom " + theExistingDomainPid);
			}
		} else {
			dom = new GDomain();
			dom.setPid(ourNextPid++);
			dom.setId(theCreateDomainId);
			dom.setName(theCreateDomainId);
			myDomainList.add(dom);
		}

		GService svc;
		if (theExistingServicePid != null) {
			svc = dom.getServiceList().getServiceByPid(theExistingServicePid);
			if (svc == null) {
				throw new NullPointerException("Unknown service " + theExistingServicePid);
			}
		} else {
			svc = new GService();
			svc.setPid(ourNextPid++);
			svc.setId(theCreateServiceId);
			svc.setName(theCreateServiceId);
			dom.getServiceList().add(svc);
		}

		if (theVersion.getPidOrNull() != null) {
			BaseGServiceVersion ver = myDomainList.getServiceVersionByPid(theVersion.getPid());
			ver.merge(theVersion);
			theVersion = ver;

		} else {
			theVersion.setPid(ourNextPid++);
			svc.getVersionList().add(theVersion);
		}

		AddServiceVersionResponse retVal = null;
		retVal = new AddServiceVersionResponse();
		retVal.setNewDomain(dom);
		retVal.setNewService(svc);
		retVal.setNewServiceVersion(theVersion);
		return retVal;

	}

	@Override
	public GHttpClientConfig saveHttpClientConfig(boolean theCreate, GHttpClientConfig theConfig) {
		if (theCreate) {
			theConfig.setPid(ourNextPid++);
			myClientConfigList.add(theConfig);
			return theConfig;
		} else {
			GHttpClientConfig existing = myClientConfigList.getConfigByPid(theConfig.getPid());
			existing.merge(theConfig);
			return existing;
		}
	}

	@Override
	public GHttpClientConfigList deleteHttpClientConfig(long thePid) throws ServiceFailureException {
		GHttpClientConfig config = myClientConfigList.getConfigByPid(thePid);
		myClientConfigList.remove(config);

		return getHttpClientConfigList();
	}

	@Override
	public GAuthenticationHostList saveAuthenticationHost(BaseGAuthHost theAuthHost) {
		if (theAuthHost.getPid() <= 0) {
			theAuthHost.setPid(ourNextPid++);
			myAuthHostList.add(theAuthHost);
		} else {
			myAuthHostList.getAuthHostByPid(theAuthHost.getPid()).merge(theAuthHost);
		}
		return myAuthHostList;
	}

	@Override
	public GAuthenticationHostList removeAuthenticationHost(long thePid) {
		myAuthHostList.remove(myAuthHostList.getAuthHostByPid(thePid));
		return getAuthHostList();
	}

	@Override
	public GPartialUserList loadUsers(PartialUserListRequest theRequest) {
		GPartialUserList retVal = new GPartialUserList();
		retVal.addAll(myUserList.toCollection());
		return retVal;
	}

	@Override
	public UserAndAuthHost loadUser(long thePid, boolean theLoadStats) {
		GUser user = myUserList.getUserByPid(thePid);
		BaseGAuthHost authHost = myAuthHostList.getAuthHostByPid(user.getAuthHostPid());

		return new UserAndAuthHost(user, authHost);
	}

	@Override
	public void saveUser(GUser theUser) {
		myUserList.getUserByPid(theUser.getPid()).merge(theUser);
	}

	public long getDefaultHttpClientConfigPid() {
		return myClientConfigList.get(0).getPid();
	}

	@Override
	public GDomainList removeDomain(long thePid) {
		myDomainList.remove(myDomainList.getDomainByPid(thePid));
		return myDomainList;
	}

	@Override
	public BaseGServiceVersion loadServiceVersionIntoSession(long theServiceVersionPid) throws ServiceFailureException {
		BaseGServiceVersion ver = myDomainList.getServiceVersionByPid(theServiceVersionPid);
		return ver;
	}

	@Override
	public GDomainList removeService(long theDomainPid, long theServicePid) {
		GServiceList serviceList = myDomainList.getDomainByPid(theDomainPid).getServiceList();
		serviceList.remove(serviceList.getServiceByPid(theServicePid));
		return myDomainList;
	}

	@Override
	public GDomainList saveService(GService theService) {
		for (GDomain nextDomain : myDomainList) {
			for (GService nextService : nextDomain.getServiceList()) {
				if (nextService.getPid() == theService.getPid()) {
					nextService.mergeSimple(theService);
				}
			}
		}
		return myDomainList;
	}

	@Override
	public GConfig loadConfig() {
		return myConfig;
	}

	@Override
	public void saveConfig(GConfig theConfig) {
		myConfig = theConfig;
	}

	@Override
	public List<GUrlStatus> loadServiceVersionUrlStatuses(long theServiceVersionPid) {
		ArrayList<GUrlStatus> retVal = new ArrayList<GUrlStatus>();

		GUrlStatus url = new GUrlStatus();
		url.setUrl("http://foo");
		url.setUrlPid(1);
		url.setLastFailure(new Date());
		url.setLastFailureMessage("This is a fail message");
		url.setLastSuccess(new Date());
		url.setLastSuccessMessage("This is a success message");
		url.setStatus(StatusEnum.ACTIVE);
		retVal.add(url);

		url = new GUrlStatus();
		url.setUrl("http://bar/werwe/werw");
		url.setUrlPid(2);
		url.setLastFailure(new Date());
		url.setLastFailureMessage("This is a fail message");
		url.setLastSuccess(new Date());
		url.setLastSuccessMessage("This is a success message");
		url.setStatus(StatusEnum.ACTIVE);
		retVal.add(url);

		return retVal;
	}

	@Override
	public GRecentMessageLists loadRecentTransactionListForServiceVersion(long theServiceVersionPid) {
		GRecentMessageLists retVal = new GRecentMessageLists();

		ArrayList<GRecentMessage> list = new ArrayList<GRecentMessage>();
//		list.add(createMessage(false));
//		list.add(createMessage(false));
//		list.add(createMessage(false));
//		list.add(createMessage(false));
//		retVal.setSuccessList(list);
//		retVal.setKeepSuccess(10);
		
		list = new ArrayList<GRecentMessage>();
		list.add(createMessage(false));
		list.add(createMessage(false));
		list.add(createMessage(false));
		list.add(createMessage(false));
		retVal.setFailList(list);
		retVal.setKeepFail(10);

		list = new ArrayList<GRecentMessage>();
		list.add(createMessage(false));
		list.add(createMessage(false));
		list.add(createMessage(false));
		list.add(createMessage(false));
		retVal.setSecurityFailList(list);
		retVal.setKeepSecurityFail(10);

		list = new ArrayList<GRecentMessage>();
		list.add(createMessage(false));
		list.add(createMessage(false));
		list.add(createMessage(false));
		list.add(createMessage(false));
		retVal.setFaultList(list);
		retVal.setKeepFault(10);

		return retVal;
	}

	private GRecentMessage createMessage(boolean theIncludeContents) {
		String responseMessage = "Response Message Contents";
		String requestMessage = "Request Message Contents";
		if (!theIncludeContents) {
			responseMessage = null;
			requestMessage = null;
		}
		return new GRecentMessage(ourNextPid++, new Date(), "http://foo", "127.0.0.1", requestMessage, responseMessage);
	}

	@Override
	public GRecentMessage loadRecentMessageForServiceVersion(long thePid) {
		return createMessage(true);
	}

	@Override
	public GRecentMessageLists loadRecentTransactionListForuser(long thePid) {
		return loadRecentTransactionListForServiceVersion(thePid);
	}

	@Override
	public GRecentMessage loadRecentMessageForUser(long thePid) {
		return loadRecentMessageForServiceVersion(thePid);
	}

	@Override
	public GServiceVersionDetailedStats loadServiceVersionDetailedStats(long theVersionPid) {
		for (GDomain nextDomain : myDomainList) {
			for (GService nextService : nextDomain.getServiceList()) {
				for (BaseGServiceVersion nextVersion : nextService.getVersionList()) {
					if (nextVersion.getPid()==theVersionPid) {
						GServiceVersionDetailedStats retVal = new GServiceVersionDetailedStats();
						Map<Long, List<Integer>> fail=new HashMap<Long, List<Integer>>();
						Map<Long, List<Integer>> fault=new HashMap<Long, List<Integer>>();
						Map<Long, List<Integer>> securityFail=new HashMap<Long, List<Integer>>();
						Map<Long, List<Integer>> success=new HashMap<Long, List<Integer>>();
						for (GServiceMethod nextMethod : nextVersion.getMethodList()) {
							success.put(nextMethod.getPid(), random60minsList());
							fail.put(nextMethod.getPid(), random60minsList());
							securityFail.put(nextMethod.getPid(), random60minsList());
							fault.put(nextMethod.getPid(), random60minsList());
						}
						retVal.setMethodPidToSuccessCount(success);
						retVal.setMethodPidToFailCount(fail);
						retVal.setMethodPidToFaultCount(fault);
						retVal.setMethodPidToSecurityFailCount(securityFail);
						return retVal;
					}
				}
			}
		}
		throw new IllegalArgumentException("Can't find "+theVersionPid);
	}

	private List<Integer> random60minsList() {
		List<Integer> retVal=new ArrayList<Integer>();
		for (int next : random60mins()) {
			retVal.add(next);
		}
		return retVal;
	}

}
