package net.svcret.admin.client.ui.config.sec;

import net.svcret.admin.shared.model.BaseGClientSecurity;
import net.svcret.admin.shared.model.BaseGServerSecurity;

public class ViewAndEditFactory {

	private ViewAndEditFactory() {
		// non instantiable
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends BaseGClientSecurity> IProvidesViewAndEdit<T> provideClientSecurity(T theClientSecurity) {
		switch (theClientSecurity.getType()) {
		case WSSEC_UT:
			return (IProvidesViewAndEdit<T>) new WsSecUsernameTokenClientSecurityViewAndEdit();
		case HTTP_BASICAUTH:
			return (IProvidesViewAndEdit<T>) new HttpBasicAuthClientSecurityViewAndEdit();
		case JSONRPC_NAMPARM:
			return (IProvidesViewAndEdit<T>) new ClientSecurityViewAndEditJsonRpcNamedParameter();
		}
		
		throw new IllegalStateException("Type: " + theClientSecurity.getType());
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends BaseGServerSecurity> IProvidesViewAndEdit<T> provideServerSecurity(T theServerSecurity) {
		switch (theServerSecurity.getType()) {
		case WSSEC_UT:
			return (IProvidesViewAndEdit<T>) new SoapWsSecUsernameTokenServerSecurity();
		case JSONRPC_NAMED_PARAMETER:
			return (IProvidesViewAndEdit<T>) new JsonRpcNamedParameterServerSecurity();
		case HTTP_BASIC_AUTH:
			return (IProvidesViewAndEdit<T>) new HttpBasicServerSecurity();
		}
		
		throw new IllegalStateException("Type: " + theServerSecurity.getType());
	}

}
