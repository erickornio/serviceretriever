package ca.uhn.sail.proxy.ejb;

import java.sql.DriverManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.ejb.EntityManagerImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class BaseJpaTest {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(BaseJpaTest.class);
	protected EntityManager myEntityManager;
	protected static EntityManagerFactory ourEntityManagerFactory;

	@BeforeClass
	public static void beforeClass() throws Exception {

		if (ourEntityManagerFactory == null) {
			try {
				ourLog.info("Starting in-memory HSQL database for unit tests");
				Class.forName("org.hsqldb.jdbcDriver");
				DriverManager.getConnection("jdbc:hsqldb:mem:unit-testing-jpa", "sa", "");
			} catch (Exception ex) {
				ex.printStackTrace();
				Assert.fail("Exception during HSQL database startup.");
			}
			try {
				ourLog.info("Building JPA EntityManager for unit tests");
				ourEntityManagerFactory = Persistence.createEntityManagerFactory("ServiceProxy_UNITTEST");
			} catch (Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
		}

	}
	
	
	protected void newEntityManager() {
		if (myEntityManager != null) {
			myEntityManager.getTransaction().commit();
		}
		myEntityManager = (EntityManagerImpl) ourEntityManagerFactory.createEntityManager();
		myEntityManager.getTransaction().begin();
	}

	@Before
	public void before() {
		myEntityManager = null;
	}


}
