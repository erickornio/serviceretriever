package net.svcret.ejb.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Table(name = "PX_USER_PERM_SVCVER_METHOD", uniqueConstraints = { // -
@UniqueConstraint(columnNames = { "SVCVER_METHOD_PID", "USER_PERM_SVCVER_PID" }) } // -
)
@Entity
public class PersUserServiceVersionMethodPermission extends BasePersObject {

	@Version()
	@Column(name = "OPTLOCK")
	private int myOptLock;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PID")
	private Long myPid;

	@ManyToOne(cascade = {}, fetch = FetchType.LAZY)
	@JoinColumn(name = "SVCVER_METHOD_PID", referencedColumnName = "PID", nullable = false)
	private PersServiceVersionMethod myServiceVersionMethod;

	@ManyToOne(cascade = {}, fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_PERM_SVCVER_PID", referencedColumnName = "PID", nullable = false)
	private PersUserServiceVersionPermission myServiceVersionPermission;

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

	/**
	 * @return the serviceDomain
	 */
	public PersServiceVersionMethod getServiceVersionMethod() {
		return myServiceVersionMethod;
	}

	/**
	 * @return the serviceUser
	 */
	public PersUserServiceVersionPermission getServiceVersionPermission() {
		return myServiceVersionPermission;
	}

	/**
	 * @param theServiceVersionMethod
	 *            the serviceDomain to set
	 */
	public void setServiceVersionMethod(PersServiceVersionMethod theServiceVersionMethod) {
		myServiceVersionMethod = theServiceVersionMethod;
	}

	/**
	 * @param theServiceVersionPermission the domainPermission to set
	 */
	public void setServiceVersionPermission(PersUserServiceVersionPermission theServiceVersionPermission) {
		myServiceVersionPermission = theServiceVersionPermission;
	}

	
}