package net.svcret.ejb.model.entity;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Authentication host: This is a module which is used to take credentials of
 * clients who are calling our services and validate that they are correct
 */
@Table(name = "PX_AUTH_HOST")
@Entity()
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "AUTH_TYPE", length = 20, discriminatorType = DiscriminatorType.STRING)
@NamedQueries(value= {@NamedQuery(name=Queries.AUTHHOST_FINDALL, query=Queries.AUTHHOST_FINDALL_Q)})
public abstract class BasePersAuthenticationHost extends BasePersObject {

	public static final String MODULE_DESC_ADMIN_AUTH = "Authenticates administrators of ServiceRetriever itself";
	public static final String MODULE_ID_ADMIN_AUTH = "ADMIN_AUTH";

	@Column(name="AUTOCREATE_AUTHD_USERS", nullable=false)
	private boolean myAutoCreateAuthorizedUsers;

	@Column(name="CACHE_SUCCESS_MILLIS", nullable=true)
	private Integer myCacheSuccessfulCredentialsForMillis;

	@Column(name = "MODULE_ID", length = 100, nullable = false, unique = true)
	private String myModuleId;

	@Column(name = "MODULE_NAME", length = 200, nullable = false)
	private String myModuleName;
	
	@Version()
	@Column(name = "OPTLOCK")
	private int myOptLock;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PID")
	private Long myPid;

	public BasePersAuthenticationHost() {
		super();
	}

	public BasePersAuthenticationHost(String theModuleId) {
		myModuleId = theModuleId;
	}

	/**
	 * @return the cacheSuccessfulCredentialsForMillis
	 */
	public Integer getCacheSuccessfulCredentialsForMillis() {
		return myCacheSuccessfulCredentialsForMillis;
	}

	/**
	 * @return the moduleId
	 */
	public String getModuleId() {
		return myModuleId;
	}

	/**
	 * @return the moduleName
	 */
	public String getModuleName() {
		return myModuleName;
	}

	/**
	 * @return the optLock
	 */
	public int getOptLock() {
		return myOptLock;
	}

	/**
	 * @return the pid
	 */
	public Long getPid() {
		return myPid;
	}
	
	public abstract AuthorizationHostTypeEnum getType();

	/**
	 * @param theCacheSuccessfulCredentialsForMillis the cacheSuccessfulCredentialsForMillis to set
	 */
	public void setCacheSuccessfulCredentialsForMillis(Integer theCacheSuccessfulCredentialsForMillis) {
		myCacheSuccessfulCredentialsForMillis = theCacheSuccessfulCredentialsForMillis;
	}

	/**
	 * @param theModuleId
	 *            the moduleId to set
	 */
	public void setModuleId(String theModuleId) {
		myModuleId = theModuleId;
	}

	/**
	 * @param theModuleName
	 *            the moduleName to set
	 */
	public void setModuleName(String theModuleName) {
		myModuleName = theModuleName;
	}

	/**
	 * @param theOptLock
	 *            the optLock to set
	 */
	public void setOptLock(int theOptLock) {
		myOptLock = theOptLock;
	}

	/**
	 * @param thePid
	 *            the pid to set
	 */
	public void setPid(Long thePid) {
		myPid = thePid;
	}

}