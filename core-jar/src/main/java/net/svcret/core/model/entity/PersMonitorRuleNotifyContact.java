package net.svcret.core.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="PX_MONITOR_RULE_CONTACT")
public class PersMonitorRuleNotifyContact extends BasePersObject {

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "PID")
	private Long myPid;

	@ManyToOne(cascade= {}, optional=false)
	@JoinColumn(name="RULE_PID", nullable=false)
	private BasePersMonitorRule myRule;

	@Column(name = "EMAIL", length=200)
	private String myEmail;

	public PersMonitorRuleNotifyContact() {
	}
	
	public PersMonitorRuleNotifyContact(String theNext) {
		myEmail = theNext;
	}

	public BasePersMonitorRule getRule() {
		return myRule;
	}

	public void setRule(BasePersMonitorRule theRule) {
		myRule = theRule;
	}

	public String getEmail() {
		return myEmail;
	}

	public void setEmail(String theEmail) {
		myEmail = theEmail;
	}

	@Override
	public Long getPid() {
		return myPid;
	}

	public void loadAllAssociations() {
		// nothing for now
	}

	
}
