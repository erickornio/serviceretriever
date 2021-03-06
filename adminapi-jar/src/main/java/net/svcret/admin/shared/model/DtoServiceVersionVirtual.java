package net.svcret.admin.shared.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.svcret.admin.shared.util.XmlConstants;

@XmlRootElement(namespace = XmlConstants.DTO_NAMESPACE, name = "ServiceVersionVirtual")
@XmlAccessorType(XmlAccessType.FIELD)
public class DtoServiceVersionVirtual extends BaseDtoServiceVersion {

	private static final long serialVersionUID = 1L;

	@XmlElement(name = "TargetServiceVersionPid")
	private long myTargetServiceVersionPid;

	public DtoServiceVersionVirtual() {
	}
	public DtoServiceVersionVirtual(long theTargetPid) {
		myTargetServiceVersionPid=theTargetPid;
	}

	@Override
	public ServiceProtocolEnum getProtocol() {
		return ServiceProtocolEnum.VIRTUAL;
	}

	public long getTargetServiceVersionPid() {
		return myTargetServiceVersionPid;
	}

	public void setTargetServiceVersionPid(long theTargetServiceVersionPid) {
		myTargetServiceVersionPid = theTargetServiceVersionPid;
	}

}
