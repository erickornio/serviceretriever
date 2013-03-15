package ca.uhn.sail.proxy.ejb;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.uhn.sail.proxy.api.IServicePersistence;
import ca.uhn.sail.proxy.ex.ProcessingException;
import ca.uhn.sail.proxy.model.entity.BasePersAuthenticationHost;
import ca.uhn.sail.proxy.model.entity.BasePersInvocationStats;
import ca.uhn.sail.proxy.model.entity.BasePersMethodStats;
import ca.uhn.sail.proxy.model.entity.BasePersServiceVersion;
import ca.uhn.sail.proxy.model.entity.InvocationStatsIntervalEnum;
import ca.uhn.sail.proxy.model.entity.PersAuthenticationHostLdap;
import ca.uhn.sail.proxy.model.entity.PersBaseClientAuth;
import ca.uhn.sail.proxy.model.entity.PersBaseServerAuth;
import ca.uhn.sail.proxy.model.entity.PersDomain;
import ca.uhn.sail.proxy.model.entity.PersEnvironment;
import ca.uhn.sail.proxy.model.entity.PersHttpClientConfig;
import ca.uhn.sail.proxy.model.entity.PersInvocationStats;
import ca.uhn.sail.proxy.model.entity.PersInvocationStatsPk;
import ca.uhn.sail.proxy.model.entity.PersInvocationUserStats;
import ca.uhn.sail.proxy.model.entity.PersInvocationUserStatsPk;
import ca.uhn.sail.proxy.model.entity.PersServiceVersionMethod;
import ca.uhn.sail.proxy.model.entity.PersService;
import ca.uhn.sail.proxy.model.entity.PersServiceUser;
import ca.uhn.sail.proxy.model.entity.PersServiceUserPermission;
import ca.uhn.sail.proxy.model.entity.PersServiceVersionStatus;
import ca.uhn.sail.proxy.model.entity.PersServiceVersionUrl;
import ca.uhn.sail.proxy.model.entity.soap.PersServiceVersionSoap11;
import ca.uhn.sail.proxy.model.entity.soap.PersWsSecUsernameTokenClientAuth;
import ca.uhn.sail.proxy.model.entity.soap.PersWsSecUsernameTokenServerAuth;

public class ServicePersistenceBeanTest extends BaseJpaTest {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ServicePersistenceBeanTest.class);
	private IServicePersistence mySvc;

	@Test
	public void testStatus() throws ProcessingException {
		Date now = new Date();

		newEntityManager();

		PersDomain domain = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		PersService service = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName");
		PersServiceVersionSoap11 ver = mySvc.getOrCreateServiceVersionWithId(service, "VersionId0");
		PersServiceVersionMethod method = ver.getOrCreateAndAddMethodWithName("MethodName");

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getOrCreateServiceVersionWithId(service, "VersionId0");
		method = ver.getOrCreateAndAddMethodWithName("MethodName");

		PersServiceVersionStatus status = ver.getStatus();
		assertNotNull(status.getPid());

		newEntityManager();

		status = mySvc.getStatusForServiceVersionWithPid(ver.getPid());
		assertNotNull(status);

		PersInvocationStats stats = new PersInvocationStats(InvocationStatsIntervalEnum.MINUTE, now, method);
		stats.addSuccessInvocation(100, 0, 0);
		stats.addSuccessInvocation(200, 0, 0);

		mySvc.saveInvocationStats(sing(stats));

		newEntityManager();

		status = mySvc.getStatusForServiceVersionWithPid(ver.getPid());
		assertNotNull(status);
		PersInvocationStatsPk pk = new PersInvocationStatsPk(InvocationStatsIntervalEnum.MINUTE, now, method);
		BasePersInvocationStats loadedStats = mySvc.getOrCreateInvocationStats(pk);

		assertEquals(2, loadedStats.getSuccessInvocationCount());
		assertEquals(150, loadedStats.getSuccessInvocationAvgTime());

		newEntityManager();

		stats = new PersInvocationStats(InvocationStatsIntervalEnum.MINUTE, now, method);
		stats.addSuccessInvocation(200, 0, 0);

		mySvc.saveInvocationStats(sing(stats));

		newEntityManager();

		status = mySvc.getStatusForServiceVersionWithPid(ver.getPid());
		assertNotNull(status);
		pk = new PersInvocationStatsPk(InvocationStatsIntervalEnum.MINUTE, now, method);
		loadedStats = mySvc.getOrCreateInvocationStats(pk);

		assertEquals(3, loadedStats.getSuccessInvocationCount());
		assertEquals(166, loadedStats.getSuccessInvocationAvgTime());

	}

	@Test
	public void testUserStatus() throws ProcessingException {
		Date now = new Date();

		newEntityManager();

		PersDomain domain = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		PersService service = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName");
		PersServiceVersionSoap11 ver = mySvc.getOrCreateServiceVersionWithId(service, "VersionId0");
		PersServiceVersionMethod method = ver.getOrCreateAndAddMethodWithName("MethodName");
		PersServiceUser user = mySvc.getOrCreateServiceUser("Username");

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getOrCreateServiceVersionWithId(service, "VersionId0");
		method = ver.getOrCreateAndAddMethodWithName("MethodName");

		PersServiceVersionStatus status = ver.getStatus();
		assertNotNull(status.getPid());

		newEntityManager();

		status = mySvc.getStatusForServiceVersionWithPid(ver.getPid());
		assertNotNull(status);

		PersInvocationUserStats stats = new PersInvocationUserStats(InvocationStatsIntervalEnum.MINUTE, now, method, user);
		stats.addSuccessInvocation(100, 0, 0);
		stats.addSuccessInvocation(200, 0, 0);

		mySvc.saveInvocationStats(sing(stats));

		newEntityManager();

		status = mySvc.getStatusForServiceVersionWithPid(ver.getPid());
		assertNotNull(status);
		PersInvocationUserStatsPk pk = PersServiceVersionStatus.createEntryPk(InvocationStatsIntervalEnum.MINUTE, now, method, user);
		BasePersInvocationStats loadedStats = mySvc.getOrCreateInvocationUserStats(pk);

		assertEquals(2, loadedStats.getSuccessInvocationCount());
		assertEquals(150, loadedStats.getSuccessInvocationAvgTime());

		newEntityManager();

		stats = new PersInvocationUserStats(InvocationStatsIntervalEnum.MINUTE, now, method, user);
		stats.addSuccessInvocation(200, 0, 0);

		mySvc.saveInvocationStats(sing(stats));

		newEntityManager();

		status = mySvc.getStatusForServiceVersionWithPid(ver.getPid());
		assertNotNull(status);
		pk = PersServiceVersionStatus.createEntryPk(InvocationStatsIntervalEnum.MINUTE, now, method, user);
		loadedStats = mySvc.getOrCreateInvocationUserStats(pk);

		assertEquals(3, loadedStats.getSuccessInvocationCount());
		assertEquals(166, loadedStats.getSuccessInvocationAvgTime());

	}

	private Collection<BasePersMethodStats> sing(BasePersInvocationStats theStats) {
		ArrayList<BasePersMethodStats> retVal = new ArrayList<BasePersMethodStats>();
		retVal.add(theStats);
		return retVal;
	}

	@Before
	public void before2() {
		mySvc = new ServicePersistenceBean();
	}

	@Override
	protected void newEntityManager() {
		super.newEntityManager();
		((ServicePersistenceBean) mySvc).setEntityManager(myEntityManager);
	}

	@Test
	public void testGetOrCreateAuthenticationHostLdap() throws ProcessingException {
		newEntityManager();

		PersAuthenticationHostLdap ldap0 = mySvc.getOrCreateAuthenticationHostLdap("module0");

		newEntityManager();

		PersAuthenticationHostLdap ldap0b = mySvc.getOrCreateAuthenticationHostLdap("module0");
		PersAuthenticationHostLdap ldap1 = mySvc.getOrCreateAuthenticationHostLdap("module1");

		assertEquals(ldap0.getPid(), ldap0b.getPid());
		assertNotEquals(ldap1.getPid(), ldap0.getPid());

		newEntityManager();

		BasePersAuthenticationHost ldap0c = mySvc.getAuthenticationHost("module0");
		assertEquals(ldap0, ldap0c);

		BasePersAuthenticationHost ldap1b = mySvc.getAuthenticationHost("module1");
		assertEquals(ldap1, ldap1b);
		assertNotEquals(ldap1, ldap0c);

	}

	@Test
	public void testGetOrCreateDomain() throws ProcessingException {
		newEntityManager();

		PersDomain domain = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		assertTrue(domain.getPid() > 0);

		newEntityManager();

		Collection<PersDomain> allDomains = mySvc.getAllDomains();
		assertEquals(1, allDomains.size());
		assertTrue(domain.equals(allDomains.iterator().next()));

		newEntityManager();

		PersDomain domain2 = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		assertEquals(domain.getPid(), domain2.getPid());

	}

	@After
	public void after() throws SQLException {
		try {
			myEntityManager.getTransaction().rollback();
		} catch (Exception e) {
			// ignore
		}
		myEntityManager.clear();

		Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:unit-testing-jpa", "sa", "");
		connection.setAutoCommit(true);
		Statement statement = connection.createStatement();
		boolean result = statement.execute("TRUNCATE SCHEMA public AND COMMIT");
		ourLog.info("Truncation result {}", result);
		statement.close();

		myEntityManager.clear();

	}

	@Test
	public void testGetOrCreateService() throws ProcessingException {
		newEntityManager();

		PersDomain domain = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		PersService service = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName");
		assertTrue(service.getPid() > 0);
		assertEquals("ServiceName", service.getServiceName());

		newEntityManager();

		Collection<PersService> allServices = mySvc.getAllServices();
		assertEquals(1, allServices.size());
		assertEquals(service, allServices.iterator().next());

		newEntityManager();

		PersService service2 = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName2");
		assertEquals(service.getPid(), service2.getPid());
		// assertNotEquals(domain.getPid(), service2.getPid());
		assertEquals("ServiceName2", service2.getServiceName());

	}

	@Test
	public void testGetOrCreateServiceVersionResource() throws ProcessingException {
		newEntityManager();

		PersDomain domain = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		PersService service = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName");
		PersServiceVersionSoap11 ver = mySvc.getOrCreateServiceVersionWithId(service, "VersionId0");

		ver.addResource("http://foo", "foo contents");
		ver.addResource("http://bar", "bar contents");
		ver.addResource("http://baz", "baz contents");

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();
		assertEquals(3, ver.getUriToResource().size());
		assertEquals("foo contents", ver.getUriToResource().get("http://foo").getResourceText());
		assertEquals("bar contents", ver.getUriToResource().get("http://bar").getResourceText());
		assertEquals("baz contents", ver.getUriToResource().get("http://baz").getResourceText());

		ver.getUriToResource().remove("http://foo");
		mySvc.saveServiceVersion(ver);

		ver = mySvc.getAllServiceVersions().iterator().next();
		assertEquals(2, ver.getUriToResource().size());
		assertNull(ver.getUriToResource().get("http://foo"));
		assertEquals("bar contents", ver.getUriToResource().get("http://bar").getResourceText());
		assertEquals("baz contents", ver.getUriToResource().get("http://baz").getResourceText());

	}

	@Test
	public void testGetOrCreateServiceUser() throws ProcessingException {
		newEntityManager();

		PersEnvironment e0 = mySvc.getOrCreateEnvironment("E0");
		PersEnvironment e1 = mySvc.getOrCreateEnvironment("E1");
		PersDomain d0 = mySvc.getOrCreateDomainWithId("D0");
		PersService d0s0 = mySvc.getOrCreateServiceWithId(d0, "D0S0", "D0S0");
		PersService d0s1 = mySvc.getOrCreateServiceWithId(d0, "D0S1", "D0S1");
		BasePersServiceVersion d0s0v0 = mySvc.getOrCreateServiceVersionWithId(d0s0, "D0S0V0");
		BasePersServiceVersion d0s0v1 = mySvc.getOrCreateServiceVersionWithId(d0s0, "D0S0V1");
		BasePersServiceVersion d0s1v0 = mySvc.getOrCreateServiceVersionWithId(d0s0, "D0S1V0");
		BasePersServiceVersion d0s1v1 = mySvc.getOrCreateServiceVersionWithId(d0s0, "D0S1V1");

		PersServiceUser user = mySvc.getOrCreateServiceUser("Username");

		PersServiceUserPermission perm0 = user.addPermission(d0);
		perm0.setAllEnvironments(true);
		perm0.setAllServices(true);
		mySvc.saveServiceUser(user);

		newEntityManager();

		/*
		 * Environments
		 */

		user = mySvc.getOrCreateServiceUser("Username");
		perm0 = user.getPermissions().iterator().next();
		assertTrue(perm0.isAllEnvironments());
		assertTrue(perm0.isAllServices());
		perm0.addEnvironment(e0);
		perm0.addEnvironment(e1);
		mySvc.saveServiceUser(user);

		newEntityManager();

		user = mySvc.getOrCreateServiceUser("Username");
		perm0 = user.getPermissions().iterator().next();
		assertEquals(2, perm0.getEnvironments().size());
		assertEquals(e0, perm0.getEnvironments().get(0));
		assertEquals(e1, perm0.getEnvironments().get(1));
		perm0.removeEnvironment(e0);
		mySvc.saveServiceUser(user);

		newEntityManager();

		user = mySvc.getOrCreateServiceUser("Username");
		perm0 = user.getPermissions().iterator().next();
		assertEquals(1, perm0.getEnvironments().size());
		assertEquals(e1, perm0.getEnvironments().get(0));

	}

	@Test
	public void testGetOrCreateServiceVersionUrl() throws ProcessingException {
		newEntityManager();

		PersDomain domain = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		PersService service = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName");
		PersServiceVersionSoap11 ver = mySvc.getOrCreateServiceVersionWithId(service, "VersionId0");

		PersServiceVersionUrl url0 = new PersServiceVersionUrl();
		url0.setUrlId("url0");
		url0.setUrl("http://url0");
		ver.getUrls().add(url0);

		PersServiceVersionUrl url1 = new PersServiceVersionUrl();
		url1.setUrlId("url1");
		url1.setUrl("http://url1");
		ver.getUrls().add(url1);

		PersServiceVersionUrl url2 = new PersServiceVersionUrl();
		url2.setUrlId("url2");
		url2.setUrl("http://url2");
		ver.getUrls().add(url2);

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();
		assertEquals(3, ver.getUrls().size());
		assertNotNull(ver.getUrls().get(0).getStatus());

		ver.retainOnlyUrlsWithIds("url0", "url2");
		ver.getUrlWithId("url0").setUrl("http://url0b");

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();
		assertEquals(2, ver.getUrls().size());
		Iterator<PersServiceVersionUrl> iter = ver.getUrls().iterator();
		assertEquals("http://url0b", iter.next().getUrl());
		assertEquals("http://url2", iter.next().getUrl());

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();

		PersServiceVersionUrl url1b = new PersServiceVersionUrl();
		url1b.setUrlId("url1b");
		url1b.setUrl("http://url1b");
		ver.getUrls().add(1, url1b);

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();
		assertEquals(3, ver.getUrls().size());
		iter = ver.getUrls().iterator();
		assertEquals("http://url0b", iter.next().getUrl());
		assertEquals("http://url1b", iter.next().getUrl());
		assertEquals("http://url2", iter.next().getUrl());

	}

	@Test
	public void testGetOrCreateServiceVersionClientAuth() throws ProcessingException {
		newEntityManager();

		PersDomain domain = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		PersService service = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName");
		PersServiceVersionSoap11 ver = mySvc.getOrCreateServiceVersionWithId(service, "VersionId0");
		PersAuthenticationHostLdap ldap = mySvc.getOrCreateAuthenticationHostLdap("Ldap0");

		PersWsSecUsernameTokenClientAuth ca0 = new PersWsSecUsernameTokenClientAuth();
		ca0.setUsername("un0");
		ver.addClientAuth(ca0);

		PersWsSecUsernameTokenClientAuth ca1 = new PersWsSecUsernameTokenClientAuth();
		ca1.setUsername("un1");
		ver.addClientAuth(ca1);

		PersWsSecUsernameTokenClientAuth ca2 = new PersWsSecUsernameTokenClientAuth();
		ca2.setUsername("un2");
		ver.addClientAuth(ca2);

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();
		assertEquals(3, ver.getClientAuths().size());

		ver.removeClientAuth(ver.getClientAuths().get(1));

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();
		assertEquals(2, ver.getClientAuths().size());
		Iterator<PersBaseClientAuth<?>> iter = ver.getClientAuths().iterator();
		assertEquals("un0", iter.next().getUsername());
		assertEquals("un2", iter.next().getUsername());

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();

		PersWsSecUsernameTokenClientAuth url1b = new PersWsSecUsernameTokenClientAuth();
		url1b.setUsername("un1b");
		ver.addClientAuth(1, url1b);

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();
		assertEquals(3, ver.getClientAuths().size());

		iter = ver.getClientAuths().iterator();
		assertEquals("un0", iter.next().getUsername());
		assertEquals("un1b", iter.next().getUsername());
		assertEquals("un2", iter.next().getUsername());

	}

	@Test
	public void testGetOrCreateServiceVersionServerAuth() throws ProcessingException {
		newEntityManager();

		PersDomain domain = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		PersService service = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName");
		PersServiceVersionSoap11 ver = mySvc.getOrCreateServiceVersionWithId(service, "VersionId0");
		PersAuthenticationHostLdap ldap = mySvc.getOrCreateAuthenticationHostLdap("Ldap0");

		PersWsSecUsernameTokenServerAuth auth = new PersWsSecUsernameTokenServerAuth();
		auth.setAuthenticationHost(ldap);

		ver.addServerAuth(auth);

		mySvc.saveServiceVersion(ver);

		newEntityManager();

		ver = mySvc.getAllServiceVersions().iterator().next();
		assertEquals(1, ver.getServerAuths().size());

		PersBaseServerAuth<?,?> serverAuth = ver.getServerAuths().get(0);
		assertEquals(PersWsSecUsernameTokenServerAuth.class, serverAuth.getClass());
		assertEquals(ldap, serverAuth.getAuthenticationHost());
	}

	@Test
	public void testGetOrCreateServiceVersion() throws ProcessingException {
		newEntityManager();

		PersDomain domain = mySvc.getOrCreateDomainWithId("DOMAIN_ID");
		PersService service = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName");
		PersServiceVersionSoap11 version0 = mySvc.getOrCreateServiceVersionWithId(service, "VersionId0");
		PersServiceVersionSoap11 version1 = mySvc.getOrCreateServiceVersionWithId(service, "VersionId1");

		assertTrue(version0.getPid() > 0);

		newEntityManager();

		Collection<PersServiceVersionSoap11> allVersions = mySvc.getAllServiceVersions();
		assertEquals(2, allVersions.size());
		assertTrue(version0.equals(allVersions.iterator().next()));
		assertEquals(2, service.getVersions().size());

		newEntityManager();

		// Update the service version properties
		version0.setWsdlUrl("http://foo");
		mySvc.saveServiceVersion(version0);

		version1.setWsdlUrl("http://bar");
		mySvc.saveServiceVersion(version1);

		newEntityManager();

		allVersions = mySvc.getAllServiceVersions();
		assertEquals(2, allVersions.size());
		assertEquals("http://foo", allVersions.iterator().next().getWsdlUrl());

		// Remove a service version
		mySvc.removeServiceVersion(version0.getPid());

		newEntityManager();

		allVersions = mySvc.getAllServiceVersions();
		assertEquals(1, allVersions.size());
		assertEquals("http://bar", allVersions.iterator().next().getWsdlUrl());

		service = mySvc.getOrCreateServiceWithId(domain, "SERVICE_ID", "ServiceName");
		assertEquals(1, service.getVersions().size());
		assertEquals("http://bar", service.getVersions().iterator().next().getWsdlUrl());

		assertEquals(PersHttpClientConfig.DEFAULT_ID, service.getVersions().iterator().next().getHttpClientConfig().getId());
		
	}

}
