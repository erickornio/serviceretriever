package net.svcret.ejb.model.entity.virtual;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import net.svcret.admin.shared.model.ServiceProtocolEnum;
import net.svcret.ejb.model.entity.BasePersServiceVersion;

@Entity
@DiscriminatorValue("VIRTUAL")
public class PersServiceVersionVirtual extends BasePersServiceVersion {

	private static final long serialVersionUID = 1L;

	@ManyToOne(cascade= {})
	@JoinColumn(name="VIRTUAL_TARGET_PID", nullable=true)
	@NotNull
	private BasePersServiceVersion myTarget;
	
	@Override
	public ServiceProtocolEnum getProtocol() {
		return ServiceProtocolEnum.JSONRPC20;
	}

	public BasePersServiceVersion getTarget() {
		return myTarget;
	}

	public void setTarget(BasePersServiceVersion theTarget) {
		myTarget = theTarget;
	}

}