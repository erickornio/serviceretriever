package net.svcret.admin.client.rpc;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.svcret.admin.shared.AddServiceVersionResponse;
import net.svcret.admin.shared.ServiceFailureException;
import net.svcret.admin.shared.model.BaseDtoAuthenticationHost;
import net.svcret.admin.shared.model.BaseDtoMonitorRule;
import net.svcret.admin.shared.model.BaseDtoServiceVersion;
import net.svcret.admin.shared.model.DtoLibraryMessage;
import net.svcret.admin.shared.model.DtoMonitorRuleActiveCheck;
import net.svcret.admin.shared.model.DtoMonitorRuleActiveCheckOutcome;
import net.svcret.admin.shared.model.DtoNodeStatusAndStatisticsList;
import net.svcret.admin.shared.model.DtoServiceVersionSoap11;
import net.svcret.admin.shared.model.DtoAuthenticationHostList;
import net.svcret.admin.shared.model.DtoConfig;
import net.svcret.admin.shared.model.DtoDomain;
import net.svcret.admin.shared.model.DtoDomainList;
import net.svcret.admin.shared.model.DtoHttpClientConfig;
import net.svcret.admin.shared.model.GMonitorRuleFiring;
import net.svcret.admin.shared.model.GMonitorRuleList;
import net.svcret.admin.shared.model.GPartialUserList;
import net.svcret.admin.shared.model.GRecentMessage;
import net.svcret.admin.shared.model.GRecentMessageLists;
import net.svcret.admin.shared.model.GService;
import net.svcret.admin.shared.model.GServiceVersionDetailedStats;
import net.svcret.admin.shared.model.GServiceVersionSingleFireResponse;
import net.svcret.admin.shared.model.GServiceVersionUrl;
import net.svcret.admin.shared.model.GUser;
import net.svcret.admin.shared.model.HierarchyEnum;
import net.svcret.admin.shared.model.ModelUpdateRequest;
import net.svcret.admin.shared.model.ModelUpdateResponse;
import net.svcret.admin.shared.model.PartialUserListRequest;
import net.svcret.admin.shared.model.ServiceProtocolEnum;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("modelupdate")
public interface ModelUpdateService extends RemoteService {

	DtoDomain addDomain(DtoDomain theDomain) throws ServiceFailureException;

	GService addService(long theDomainPid, GService theService) throws ServiceFailureException;

	AddServiceVersionResponse addServiceVersion(Long theExistingDomainPid, String theCreateDomainId, Long theExistingServicePid, String theCreateServiceId, BaseDtoServiceVersion theVersion) throws ServiceFailureException;

	BaseDtoServiceVersion cloneServiceVersion(long thePidToClone) throws ServiceFailureException;

	BaseDtoServiceVersion createNewServiceVersion(ServiceProtocolEnum theProtocol, Long theDomainPid, Long theServicePid, Long theUncommittedId);

	DtoMonitorRuleActiveCheck executeMonitorRuleActiveCheck(DtoMonitorRuleActiveCheck theCheck) throws ServiceFailureException;

	Map<Long, GMonitorRuleFiring> getLatestFailingMonitorRuleFiringForRulePids() throws ServiceFailureException;

	DtoConfig loadConfig() throws ServiceFailureException;

	DtoLibraryMessage loadLibraryMessage(long theMessagePid) throws ServiceFailureException;

	Collection<DtoLibraryMessage> loadLibraryMessages() throws ServiceFailureException;

	Collection<DtoLibraryMessage> loadLibraryMessages(HierarchyEnum theType, long thePid) throws ServiceFailureException;

	ModelUpdateResponse loadModelUpdate(ModelUpdateRequest theRequest) throws ServiceFailureException;

	BaseDtoMonitorRule loadMonitorRule(long theRulePid);

	DtoMonitorRuleActiveCheckOutcome loadMonitorRuleActiveCheckOutcomeDetails(long thePid) throws ServiceFailureException;

	List<GMonitorRuleFiring> loadMonitorRuleFirings(Long theDomainPid, Long theServicePid, Long theServiceVersionPid, int theStart);

	GMonitorRuleList loadMonitorRuleList() throws ServiceFailureException;

	DtoNodeStatusAndStatisticsList loadNodeListAndStatistics();

	GRecentMessage loadRecentMessageForServiceVersion(long thePid) throws ServiceFailureException;

	GRecentMessage loadRecentMessageForUser(long thePid) throws ServiceFailureException;

	GRecentMessageLists loadRecentTransactionListForServiceVersion(long theServiceVersionPid);

	GRecentMessageLists loadRecentTransactionListForuser(long thePid);

	GServiceVersionDetailedStats loadServiceVersionDetailedStats(long theVersionPid) throws ServiceFailureException;

	BaseDtoServiceVersion loadServiceVersionIntoSession(long theServiceVersionPid) throws ServiceFailureException;

	UserAndAuthHost loadUser(long theUserPid, boolean theLoadStats) throws ServiceFailureException;

	GPartialUserList loadUsers(PartialUserListRequest theRequest) throws ServiceFailureException;

	DtoServiceVersionSoap11 loadWsdl(DtoServiceVersionSoap11 theService, DtoHttpClientConfig theClientConfig, String theWsdlUrl) throws ServiceFailureException;

	DtoAuthenticationHostList removeAuthenticationHost(long thePid) throws ServiceFailureException;

	DtoDomainList removeDomain(long thePid) throws ServiceFailureException;

	DtoDomainList removeService(long theDomainPid, long theServicePid) throws ServiceFailureException;

	DtoDomainList removeServiceVersion(long thePid) throws ServiceFailureException;

	void removeUser(long thePid) throws ServiceFailureException;

	void reportClientError(String theMessage, Throwable theException);

	GServiceVersionUrl resetCircuitBreakerForServiceVersionUrl(long theUrlPid) throws ServiceFailureException;

	DtoAuthenticationHostList saveAuthenticationHost(BaseDtoAuthenticationHost theAuthHost) throws ServiceFailureException;

	void saveConfig(DtoConfig theConfig) throws ServiceFailureException;

	DtoDomainList saveDomain(DtoDomain theDomain) throws ServiceFailureException;

	void saveLibraryMessage(DtoLibraryMessage theMessage) throws ServiceFailureException;

	GMonitorRuleList saveMonitorRule(BaseDtoMonitorRule theRule) throws ServiceFailureException;

	DtoDomainList saveService(GService theService) throws ServiceFailureException;

	void saveServiceVersionToSession(BaseDtoServiceVersion theServiceVersion);

	GUser saveUser(GUser theUser) throws ServiceFailureException;

	GServiceVersionSingleFireResponse testServiceVersionWithSingleMessage(String theMessageText, String theContentType, long theServiceVersionPid) throws ServiceFailureException;

	public static class UserAndAuthHost implements Serializable {
		private static final long serialVersionUID = 1L;

		private BaseDtoAuthenticationHost myAuthHost;
		private GUser myUser;

		/**
		 * Constructor
		 */
		public UserAndAuthHost() {
			super();
		}

		/**
		 * Constructor
		 */
		public UserAndAuthHost(GUser theUser, BaseDtoAuthenticationHost theAuthHost) {
			super();
			myUser = theUser;
			myAuthHost = theAuthHost;
		}

		/**
		 * @return the authHost
		 */
		public BaseDtoAuthenticationHost getAuthHost() {
			return myAuthHost;
		}

		/**
		 * @return the user
		 */
		public GUser getUser() {
			return myUser;
		}

		/**
		 * @param theAuthHost
		 *            the authHost to set
		 */
		public void setAuthHost(BaseDtoAuthenticationHost theAuthHost) {
			myAuthHost = theAuthHost;
		}

		/**
		 * @param theUser
		 *            the user to set
		 */
		public void setUser(GUser theUser) {
			myUser = theUser;
		}
	}

}
