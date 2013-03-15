package ca.uhn.sail.proxy.model.entity;

import java.util.Date;

import javax.persistence.Embeddable;

@Embeddable
public class PersInvocationStatsPk extends BasePersInvocationStatsPk {

	private static final long serialVersionUID = 1L;

	public PersInvocationStatsPk() {
		super();
	}

	public PersInvocationStatsPk(InvocationStatsIntervalEnum theInterval, Date theStartTime, PersServiceVersionMethod theMethod) {
		super(theInterval, theStartTime, theMethod);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PersInvocationStats newObjectInstance() {
		return new PersInvocationStats(this);
	}
	
	@Override
	protected boolean doEquals(BasePersMethodStatsPk theObj2) {
		PersInvocationStatsPk theObj = (PersInvocationStatsPk) theObj2;
		return getInterval().equals(theObj.getInterval()) // -
				&& getMethod().equals(theObj.getMethod()) // -
				&& getStartTime().equals(theObj.getStartTime()); // -
	}


}
