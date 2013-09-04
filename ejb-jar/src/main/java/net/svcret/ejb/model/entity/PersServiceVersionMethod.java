package net.svcret.ejb.model.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.commons.lang3.Validate;

import net.svcret.admin.shared.enm.MethodSecurityPolicyEnum;

@Table(name = "PX_SVC_VER_METHOD")
@Entity
public class PersServiceVersionMethod extends BasePersObject {

	private static final long serialVersionUID = 1L;

	@OneToMany(fetch=FetchType.LAZY, cascade= {CascadeType.REMOVE}, orphanRemoval=true, mappedBy="myPk.myMethod")
	private Collection<PersInvocationMethodSvcverStats> myInvocationStats;

	@Column(name = "NAME", length = 200, nullable=false)
	private String myName;

	@Column(name="METHOD_ORDER", nullable=false)
	private int myOrder;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PID")
	private Long myPid;

	@Column(name = "ROOT_ELEMENTS", length = 1000, nullable=true)
	private String myRootElements;

	@Column(name="SEC_POLICY", length=50, nullable=false)
	@Enumerated(EnumType.STRING)
	private MethodSecurityPolicyEnum mySecurityPolicy=MethodSecurityPolicyEnum.getDefault();

	@ManyToOne()
	@JoinColumn(name = "SVC_VERSION_PID", referencedColumnName = "PID", nullable = false)
	private BasePersServiceVersion myServiceVersion;

	@OneToMany(fetch = FetchType.LAZY, cascade = {}, orphanRemoval = true, mappedBy = "myMethod")
	private List<PersServiceVersionRecentMessage> mySvcVerRecentMessages;

	@OneToMany(fetch=FetchType.LAZY, cascade= {CascadeType.REMOVE}, orphanRemoval=true, mappedBy="myServiceVersionMethod")
	private Collection<PersUserServiceVersionMethodPermission> myUserPermissions;

	@OneToMany(fetch = FetchType.LAZY, cascade = {}, orphanRemoval = true, mappedBy = "myMethod")
	private List<PersUserRecentMessage> myUserRecentMessages;

	@Version()
	@Column(name = "OPTLOCK")
	protected int myOptLock;
	
	/**
	 * Constructor
	 */
	public PersServiceVersionMethod() {
		super();
	}

	/**
	 * Constructor
	 */
	public PersServiceVersionMethod(long thePid, BasePersServiceVersion theServiceVersion, String theMethodName) {
		setPid(thePid);
		setServiceVersion(theServiceVersion);
		setName(theMethodName);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return myName;
	}

	/**
	 * @return the optLock
	 */
	public int getOptLock() {
		return myOptLock;
	}

	/**
	 * @return the order
	 */
	public int getOrder() {
		return myOrder;
	}

	/**
	 * @return the id
	 */
	public Long getPid() {
		return myPid;
	}

	public String getRootElements() {
		return myRootElements;
	}
	
	public MethodSecurityPolicyEnum getSecurityPolicy() {
		return mySecurityPolicy;
	}

	/**
	 * @return the serviceVersion
	 */
	public BasePersServiceVersion getServiceVersion() {
		return myServiceVersion;
	}

	public Collection<PersUserServiceVersionMethodPermission> getUserPermissions() {
		if (myUserPermissions==null) {
			myUserPermissions=new ArrayList<PersUserServiceVersionMethodPermission>();
		}
		return myUserPermissions;
	}

	
	
	public void loadAllAssociations() {
		for (PersUserServiceVersionMethodPermission next : getUserPermissions()) {
			next.loadAllAssociations();
		}
	}

	public void merge(PersServiceVersionMethod theObj) {
		setName(theObj.getName());
		setRootElements(theObj.getRootElements());
	}

	/**
	 * @param theName
	 *            the name to set
	 */
	public void setName(String theName) {
		myName = theName;
	}

	/**
	 * @param theOptLock
	 *            the optLock to set
	 */
	public void setOptLock(int theOptLock) {
		myOptLock = theOptLock;
	}

	/**
	 * @param theOrder the order to set
	 */
	public void setOrder(int theOrder) {
		myOrder = theOrder;
	}

	/**
	 * @param theId
	 *            the id to set
	 */
	public void setPid(Long theId) {
		myPid = theId;
	}

	public void setRootElements(String theRootElements) {
		myRootElements = theRootElements;
	}

	public void setSecurityPolicy(MethodSecurityPolicyEnum theSecurityPolicy) {
		Validate.notNull(theSecurityPolicy);
		mySecurityPolicy = theSecurityPolicy;
	}

	/**
	 * @param theServiceVersion
	 *            the serviceVersion to set
	 */
	public void setServiceVersion(BasePersServiceVersion theServiceVersion) {
		if (theServiceVersion != null) {
			if (theServiceVersion.equals(myServiceVersion)) {
				return;
			} else if (myServiceVersion != null) {
				throw new IllegalStateException("Can't move methods to a new version");
			}
		} else {
			throw new NullPointerException("ServiceVersion can not be null");
		}
		
		myServiceVersion = theServiceVersion;
//		theServiceVersion.addMethod(this);
	}

}
