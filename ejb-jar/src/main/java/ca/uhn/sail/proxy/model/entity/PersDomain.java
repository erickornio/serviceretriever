package ca.uhn.sail.proxy.model.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import com.google.common.base.Objects;

@Table(name = "PX_DOMAIN")
@Entity
public class PersDomain {

	@Column(name = "DOMAIN_ID", length = 100, nullable = false)
	private String myDomainId;

	@Column(name = "DOMAIN_NAME", length = 200, nullable = true)
	private String myDomainName;

	private HashMap<String, PersService> myIdToServices;

	@Version()
	@Column(name = "OPTLOCK")
	protected int myOptLock;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PID")
	private Long myPid;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "DOMAIN_PID", referencedColumnName = "PID")
	private Collection<PersService> myServices;

	public PersDomain() {
		super();
	}

	public PersDomain(long thePid, String theDomainId) {
		myPid = thePid;
		myDomainId = theDomainId;
	}


	/**
	 * @return the domainId
	 */
	public String getDomainId() {
		return myDomainId;
	}

	/**
	 * @return the domainName
	 */
	public String getDomainName() {
		return myDomainName;
	}

	/**
	 * @return the versionNum
	 */
	public int getOptLock() {
		return myOptLock;
	}

	/**
	 * @return the id
	 */
	public Long getPid() {
		return myPid;
	}

	/**
	 * @return the services
	 */
	public Collection<PersService> getServices() {
		if (myServices == null) {
			myServices = new ArrayList<PersService>();
		}
		return myServices;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object theObj) {
		return theObj instanceof PersDomain && Objects.equal(myPid, ((PersDomain) theObj).myPid);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(myPid);
	}

	public void loadAllAssociations() {
		myIdToServices = new HashMap<String, PersService>();
		for (PersService next : myServices) {
			myIdToServices.put(next.getServiceId(), next);
			next.loadAllAssociations();
		}
	}

	/**
	 * @param theDomainId
	 *            the domainId to set
	 */
	public void setDomainId(String theDomainId) {
		myDomainId = theDomainId;
	}

	/**
	 * @param theDomainName
	 *            the domainName to set
	 */
	public void setDomainName(String theDomainName) {
		myDomainName = theDomainName;
	}

	/**
	 * @param theOptLock
	 *            the versionNum to set
	 */
	public void setOptLock(int theOptLock) {
		myOptLock = theOptLock;
	}

	/**
	 * @param theId
	 *            the id to set
	 */
	public void setPid(Long thePid) {
		myPid = thePid;
	}

}
