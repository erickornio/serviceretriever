package net.svcret.ejb.model.entity;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import net.svcret.ejb.util.Validate;

@Entity
@Table(name = "PX_STATE")
public class PersState {

	@Id
	@Column(name = "KEY", length = 200)
	private String myKey;

	@Version()
	@Column(name = "OPTLOCK")
	private Timestamp myOptLock;

	@Column(name = "VERSION", nullable = false)
	private long myVersion;

	public PersState() {
		// nothing
	}

	public PersState(String theKey) {
		Validate.throwIllegalArgumentExceptionIfBlank("Key", theKey);
		myKey = theKey;
	}

	@Override
	public boolean equals(Object theObj) {
		if (!(theObj instanceof PersState)) {
			return false;
		}

		return myKey.equals(((PersState) theObj).myKey);
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return myKey;
	}

	/**
	 * @return the optLock
	 */
	public Timestamp getOptLock() {
		return myOptLock;
	}

	/**
	 * @return the version
	 */
	public long getVersion() {
		return myVersion;
	}

	@Override
	public int hashCode() {
		return myKey.hashCode();
	}

	/**
	 * @param theKey
	 *            the key to set
	 */
	public void setKey(String theKey) {
		Validate.throwIllegalArgumentExceptionIfBlank("Key", theKey);
		myKey = theKey;
	}

	/**
	 * @param theVersion
	 *            the version to set
	 */
	public void setVersion(long theVersion) {
		myVersion = theVersion;
	}

	public void incrementVersion() {
		myVersion++;
	}

}