package net.svcret.core.model.entity;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import net.svcret.admin.shared.model.BaseDtoServiceCatalogItem;

import org.apache.commons.lang3.StringUtils;

@MappedSuperclass
public abstract class BasePersServiceCatalogItem extends BasePersKeepsRecentTransactions {

	private static final long serialVersionUID = 1L;

	@Column(name = "DISP_PUBLIC_REG", nullable = true)
	private Boolean myDisplayInPublicRegistry;

//	@ManyToOne(cascade = {}, optional = true)
//	@JoinColumn(name = "MOST_REC_MONITORRULE_FIR", nullable = true)
//	private PersMonitorRuleFiring myMostRecentMonitorRuleFiring;

	@Column(name = "OBSCURE_REQ_ELEMS_IN_LOG", length = 2000, nullable = true)
	private String myObscureRequestElementsInLog;

	private transient volatile TreeSet<String> myObscureRequestElementsInLogCache;

	@Column(name = "OBSCURE_RESP_ELEMS_IN_LOG", length = 2000, nullable = true)
	private String myObscureResponseElementsInLog;

	private transient volatile TreeSet<String> myObscureResponseElementsInLogCache;

	public abstract boolean canInheritObscureElements();

	public abstract Set<String> determineInheritedObscureRequestElements();

	public abstract Set<String> determineInheritedObscureResponseElements();

	public abstract Set<String> determineObscureRequestElements();

	public abstract Set<String> determineObscureResponseElements();


	public abstract Collection<? extends BasePersServiceVersion> getAllServiceVersions();

	public Boolean getDisplayInPublicRegistry() {
		return myDisplayInPublicRegistry;
	}

//	public PersMonitorRuleFiring getMostRecentMonitorRuleFiring() {
//		return myMostRecentMonitorRuleFiring;
//	}

	public Set<String> getObscureRequestElementsInLog() {
		TreeSet<String> retVal = myObscureRequestElementsInLogCache;
		if (retVal == null) {
			retVal = new TreeSet<String>();
			if (myObscureRequestElementsInLog != null) {
				for (String next : myObscureRequestElementsInLog.split("\\n")) {
					if (StringUtils.isNotBlank(next)) {
						retVal.add(next.trim());
					}
				}
			}
			myObscureRequestElementsInLogCache = retVal;
		}
		return retVal;
	}

	public Set<String> getObscureResponseElementsInLog() {
		TreeSet<String> retVal = myObscureResponseElementsInLogCache;
		if (retVal == null) {
			retVal = new TreeSet<String>();
			if (myObscureResponseElementsInLog != null) {
				for (String next : myObscureResponseElementsInLog.split("\\n")) {
					if (StringUtils.isNotBlank(next)) {
						retVal.add(next.trim());
					}
				}
			}
			myObscureResponseElementsInLogCache = retVal;
		}
		return retVal;
	}

	public void merge(BasePersObject theObj) {
		super.merge(theObj);

		BasePersServiceCatalogItem item = (BasePersServiceCatalogItem) theObj;
		setObscureRequestElementsInLog(item.getObscureRequestElementsInLog());
		setObscureResponseElementsInLog(item.getObscureResponseElementsInLog());
		setDisplayInPublicRegistry(item.getDisplayInPublicRegistry());
	}

	public void populateServiceCatalogItemFromDto(BaseDtoServiceCatalogItem theDto) {
		setObscureRequestElementsInLog(theDto.getObscureRequestElementsInLogCache());
		setObscureResponseElementsInLog(theDto.getObscureResponseElementsInLogCache());
		setDisplayInPublicRegistry(theDto.getDisplayInPublicRegistry());
	}

	public void populateServiceCatalogItemToDto(BaseDtoServiceCatalogItem theDto) {
		theDto.setObscureRequestElementsInLogCache(getObscureRequestElementsInLog());
		theDto.setObscureResponseElementsInLogCache(getObscureResponseElementsInLog());
		theDto.setDisplayInPublicRegistry(getDisplayInPublicRegistry());
	}

	public void setDisplayInPublicRegistry(Boolean theDisplayInPublicRegistry) {
		myDisplayInPublicRegistry = theDisplayInPublicRegistry;
	}

//	/**
//	 * @param theMostRecentMonitorRuleFiring
//	 *            the mostRecentMonitorRuleFiring to set
//	 */
//	public void setMostRecentMonitorRuleFiring(PersMonitorRuleFiring theMostRecentMonitorRuleFiring) {
//		myMostRecentMonitorRuleFiring = theMostRecentMonitorRuleFiring;
//	}

	public void setObscureRequestElementsInLog(Set<String> theObscureRequestElementsInLog) {
		StringBuilder b = new StringBuilder();
		if (theObscureRequestElementsInLog != null) {
			for (String next : theObscureRequestElementsInLog) {
				if (next.contains("\n")) {
					throw new IllegalArgumentException("Element can not contain newline");
				}
				if (b.length() > 0) {
					b.append('\n');
				}
				b.append(next);
			}
		}
		myObscureRequestElementsInLog = b.toString();
		myObscureRequestElementsInLogCache = null;
	}

	public void setObscureResponseElementsInLog(Set<String> theObscureResponseElementsInLog) {
		StringBuilder b = new StringBuilder();
		if (theObscureResponseElementsInLog != null) {
			for (String next : theObscureResponseElementsInLog) {
				if (next.contains("\n")) {
					throw new IllegalArgumentException("Element can not contain newline");
				}
				if (b.length() > 0) {
					b.append('\n');
				}
				b.append(next);
			}
		}
		myObscureResponseElementsInLog = b.toString();
		myObscureRequestElementsInLogCache = null;
	}

	public static void validateId(String theId) {
		if (StringUtils.isBlank(theId)) {
			throw new IllegalArgumentException("ID must not be blank");
		}

		if (theId.contains(" ")) {
			throw new IllegalArgumentException("ID must not contain spaces: " + theId);
		}

		if (theId.contains("__")) {
			throw new IllegalArgumentException("ID must not contain the character saequence: '__': " + theId);
		}
	}

}
