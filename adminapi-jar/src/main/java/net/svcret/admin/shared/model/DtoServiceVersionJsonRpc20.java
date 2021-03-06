package net.svcret.admin.shared.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import net.svcret.admin.shared.util.XmlConstants;


@XmlRootElement(namespace=XmlConstants.DTO_NAMESPACE, name="ServiceVersionJsonRpc20")
@XmlAccessorType(XmlAccessType.FIELD)
public class DtoServiceVersionJsonRpc20 extends BaseDtoServiceVersion {

	private static final long serialVersionUID = 1L;

	public DtoServiceVersionJsonRpc20() {
	}
	
	public DtoServiceVersionJsonRpc20(String theId, String theName, long theHttpClientPid) {
		setId(theId);
		setName(theName);
		setHttpClientConfigPid(theHttpClientPid);
	}

	@Override
	public ServiceProtocolEnum getProtocol() {
		return ServiceProtocolEnum.JSONRPC20;
	}

}
