package net.svcret.core.invoker.jsonrpc;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.svcret.admin.shared.enm.ResponseTypeEnum;
import net.svcret.core.api.ICredentialGrabber;
import net.svcret.core.api.RequestType;
import net.svcret.core.api.SrBeanIncomingRequest;
import net.svcret.core.api.SrBeanIncomingResponse;
import net.svcret.core.api.SrBeanProcessedRequest;
import net.svcret.core.api.SrBeanProcessedResponse;
import net.svcret.core.api.SrBeanProcessedRequest.ResultTypeEnum;
import net.svcret.core.ejb.DefaultAnswer;
import net.svcret.core.ex.InvocationRequestFailedException;
import net.svcret.core.invoker.jsonrpc.JsonRpc20ServiceInvoker;
import net.svcret.core.model.entity.PersBaseClientAuth;
import net.svcret.core.model.entity.PersBaseServerAuth;
import net.svcret.core.model.entity.PersMethod;
import net.svcret.core.model.entity.jsonrpc.NamedParameterJsonRpcClientAuth;
import net.svcret.core.model.entity.jsonrpc.NamedParameterJsonRpcServerAuth;
import net.svcret.core.model.entity.jsonrpc.PersServiceVersionJsonRpc20;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("static-method")
public class JsonRpc20ServiceInvokerTest {

	@Test
	public void testObscureDocument() throws Exception{
		String request = "{\n" + "  \"jsonrpc\": \"2.0\",\n" + "  \"method\": \"getCanonicalMappings\",\n" + "  \"params\": {\n" + "    \"request\": {\n"
				+ "      \"idAuthority\": \"2.16.840.1.113883.3.59.3:736\",\n" + "      \"idExt\": \"87170\"\n" + "    },\n" + "    \"clientId\": \"mockuser\",\n"
				+ "    \"clientPass\": \"mockpass\"\n" + "  }\n" + "}";

		JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();
		svc.setPrettyPrintModeForUnitTest(true);
		Set<String> obscure = new HashSet<>();
		String obscured = svc.obscureMessageForLogs(null, request, obscure);
		ourLog.info("Not obscured: {}", obscured);

		obscure.clear();
		obscure.add("idExt");
		obscured = svc.obscureMessageForLogs(null, request, obscure);
		ourLog.info("Not obscured: {}", obscured);
		assertTrue(obscured.contains("\"idExt\": \"**REDACTED**\""));

		obscure.clear();
		obscure.add("request");
		obscured = svc.obscureMessageForLogs(null, request, obscure);
		ourLog.info("Not obscured: {}", obscured);
		assertTrue(obscured.contains("\"request\": \"**REDACTED**\""));
		assertFalse(obscured.contains("\"idExt\""));

		//@formatter:off
		request = "{\n" + 
				"    \"jsonrpc\": \"2.0\",\n" + 
				"    \"method\": \"getNODRPatientByMrn\",\n" + 
				"    \"params\": {\n" + 
				"        \"auth\": \"UHN\",\n" + 
				"        \"mrn\": \"\",\n" + 
				"        \"auditSourceId\": \"FORM_VIEWER\",\n" + 
				"        \"user\": {\n" + 
				"            \"lastName\": \"King\",\n" + 
				"            \"lastLoginDate\": \"Aug 27, 2013 03:51:44 PM\",\n" + 
				"            \"location\": \"UHN\",\n" + 
				"            \"firstName\": \"Gered\",\n" + 
				"            \"attributes\": {},\n" + 
				"            \"distinguishedName\": \"CN=King\\\\, Gered,OU=AdvancedUsers,OU=UHNPeople,DC=uhn,DC=ca\",\n" + 
				"            \"email\": \"Gered.King@uhn.ca\",\n" + 
				"            \"uid\": \"t35103uhn\",\n" + 
				"            \"fullName\": \"King, Gered\",\n" + 
				"            \"clientIp\": \"10.7.7.167\",\n" + 
				"            \"description\": \"Developer - Medical Informatics, SIMS\"\n" + 
				"        },\n" + 
				"        \"clientId\": \"clipdevsvc\",\n" + 
				"        \"clientPass\": \"Hospital20\"\n" + 
				"    }\n" + 
				"}";
		//@formatter:on

		obscure.clear();
		obscure.add("password");
		obscure.add("clientPass");
		obscured = svc.obscureMessageForLogs(null, request, obscure);
		ourLog.info("Obscured: {}", obscured);
		assertTrue(obscured.contains("\"clientPass\": \"**REDACTED**\""));
		assertTrue(obscured.contains("\"clientId\""));

	}

	@Test
	public void testNamedParemeterCredentialGrabber() throws Exception {

		String request = "{\n" + "  \"jsonrpc\": \"2.0\",\n" + "  \"method\": \"getCanonicalMappings\",\n" + "  \"params\": {\n" + "    \"request\": {\n"
				+ "      \"idAuthority\": \"2.16.840.1.113883.3.59.3:736\",\n" + "      \"idExt\": \"87170\"\n" + "    },\n" + "    \"clientId\": \"mockuser\",\n"
				+ "    \"clientPass\": \"mockpass\"\n" + "  }\n" + "}";
		StringReader reader = new StringReader(request);

		JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();

		PersServiceVersionJsonRpc20 def = mock(PersServiceVersionJsonRpc20.class);
		ArrayList<PersBaseServerAuth<?, ?>> serverAuths = new ArrayList<>();
		serverAuths.add(new NamedParameterJsonRpcServerAuth("clientId", "clientPass"));
		when(def.getServerAuths()).thenReturn(serverAuths);
		PersMethod method = mock(PersMethod.class);

		when(def.getMethod("getCanonicalMappings")).thenReturn(method);

		DefaultAnswer.setRunTime();
		SrBeanIncomingRequest req = new SrBeanIncomingRequest();
		req.setPath("/");
		req.setQuery("");
		req.addHeader("Content-Type", "application/json");
		req.setRequestType(RequestType.POST);
		req.setInputReader(reader);
		SrBeanProcessedRequest resp = svc.processInvocation(req,def);

		ICredentialGrabber grabber = resp.getCredentialsInRequest(serverAuths.get(0));
		assertEquals("mockpass", grabber.getPassword());
		assertEquals("mockuser", grabber.getUsername());

	}

	@Test
	public void testNamedParemeterCredentialGrabberServerSecurityAndNamedParameterClientSecurity() throws Exception{

		//@formatter:off
		String request = 
				"{\n" + 
				"  \"jsonrpc\": \"2.0\",\n" + 
				"  \"method\": \"getCanonicalMappings\",\n" +
				"  \"params\": {\n" +
				"    \"request\": {\n" +
				"      \"idAuthority\": \"2.16.840.1.113883.3.59.3:736\",\n" + 
				"      \"idExt\": \"87170\"\n" + 
				"    },\n" + 
				"    \"clientId\": \"mockuser\",\n" +
				"    \"clientPass\": \"mockpass\"\n" + 
				"  }\n" + 
				"}";
		//@formatter:on

		StringReader reader = new StringReader(request);

		JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();

		PersServiceVersionJsonRpc20 def = mock(PersServiceVersionJsonRpc20.class);
		ArrayList<PersBaseServerAuth<?, ?>> serverAuths = new ArrayList<>();
		serverAuths.add(new NamedParameterJsonRpcServerAuth("clientId", "clientPass"));
		when(def.getServerAuths()).thenReturn(serverAuths);

		ArrayList<PersBaseClientAuth<?>> clientAuths = new ArrayList<>();
		clientAuths.add(new NamedParameterJsonRpcClientAuth("newUsername", "clientId", "newPassword", "clientPass"));
		when(def.getClientAuths()).thenReturn(clientAuths);

		PersMethod method = mock(PersMethod.class);

		when(def.getMethod("getCanonicalMappings")).thenReturn(method);

		DefaultAnswer.setRunTime();
		SrBeanIncomingRequest req = new SrBeanIncomingRequest();
		req.setPath("/");
		req.setQuery("");
		req.addHeader("Content-Type", "application/json");
		req.setRequestType(RequestType.POST);
		req.setInputReader(reader);
		SrBeanProcessedRequest resp = svc.processInvocation(req,def);

		ICredentialGrabber grabber = resp.getCredentialsInRequest(serverAuths.get(0));
		assertEquals("mockpass", grabber.getPassword());
		assertEquals("mockuser", grabber.getUsername());

		String newRequest = resp.getMethodRequestBody();
		ourLog.info("New request: {}", newRequest);
		assertTrue(newRequest.contains("\"clientId\": \"newUsername\""));
		assertTrue(newRequest.contains("\"clientPass\": \"newPassword\""));

		/*
		 * Now with a request containing no credentials in it to begin with (so they need to be added)
		 */

		//@formatter:off
		request = 
				"{\n" + 
				"  \"jsonrpc\": \"2.0\",\n" + 
				"  \"method\": \"getCanonicalMappings\",\n" +
				"  \"params\": {\n" +
				"    \"request\": {\n" +
				"      \"idAuthority\": \"2.16.840.1.113883.3.59.3:736\",\n" + 
				"      \"idExt\": \"87170\"\n" + 
				"    }\n" + 
				"  }\n" + 
				"}";
		//@formatter:on

		reader = new StringReader(request);

		svc = new JsonRpc20ServiceInvoker();

		def = mock(PersServiceVersionJsonRpc20.class);
		serverAuths = new ArrayList<>();
		serverAuths.add(new NamedParameterJsonRpcServerAuth("clientId", "clientPass"));
		when(def.getServerAuths()).thenReturn(serverAuths);

		clientAuths = new ArrayList<>();
		clientAuths.add(new NamedParameterJsonRpcClientAuth("newUsername", "clientId", "newPassword", "clientPass"));
		when(def.getClientAuths()).thenReturn(clientAuths);

		method = mock(PersMethod.class);

		when(def.getMethod("getCanonicalMappings")).thenReturn(method);

		DefaultAnswer.setRunTime();
		req = new SrBeanIncomingRequest();
		req.setPath("/");
		req.setQuery("");
		req.addHeader("Content-Type", "application/json");
		req.setRequestType(RequestType.POST);
		req.setInputReader(reader);
		resp = svc.processInvocation(req,def);

		grabber = resp.getCredentialsInRequest(serverAuths.get(0));
		assertEquals(null, grabber.getPassword());
		assertEquals(null, grabber.getUsername());

		newRequest = resp.getMethodRequestBody();
		ourLog.info("New request: {}", newRequest);
		assertTrue(newRequest.contains("\"clientId\": \"newUsername\""));
		assertTrue(newRequest.contains("\"clientPass\": \"newPassword\""));

	}

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(JsonRpc20ServiceInvokerTest.class);

	/**
	 * A failing message
	 */
	@Test
	public void testNullParameterValues() throws Exception{

		String request = "{\"jsonrpc\":\"2.0\",\"method\":\"getActsByVisit\",\"params\":{\"auth\":\"UHN\",\"convertFromAppTerminology\":false,\"clientId\":\"clipdevsvc\",\"auditSourceId\":\"FORM_VIEWER\",\"visitId\":\"26200\n"
				+ "0218\",\"actToTerminologyLevel\":\"NONE\",\"convertObsToAppTerm\":false,\"statusCodes\":null,\"returnLoadedConcept\":false,\"loaded\":false,\"user\":{\"uid\":\"userId\",\"clientIp\":\"127.0.0.\n"
				+ "1\",\"attributes\":{}},\"clientPass\":\"Hospital20\",\"procCodes\":[\"1881\",\"1756\",\"1757\",\"1758\",\"5119\"]}}";

		ourLog.info("Request:\n{}", request);

		StringReader reader = new StringReader(request);

		JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();
		PersServiceVersionJsonRpc20 def = mock(PersServiceVersionJsonRpc20.class);
		PersMethod method = mock(PersMethod.class);

		when(def.getMethod("getActsByVisit")).thenReturn(method);
		when(def.getServerAuths()).thenReturn(new ArrayList<PersBaseServerAuth<?, ?>>());

		DefaultAnswer.setRunTime();
		SrBeanIncomingRequest req = new SrBeanIncomingRequest();
		req.setPath("/");
		req.setQuery("");
		req.addHeader("Content-Type", "application/json");
		req.setRequestType(RequestType.POST);
		req.setInputReader(reader);
		SrBeanProcessedRequest resp = svc.processInvocation(req,def);

		Assert.assertEquals("application/json", resp.getMethodContentType());
		Assert.assertEquals(ResultTypeEnum.METHOD, resp.getResultType());
		Assert.assertSame(method, resp.getMethodDefinition());

		svc.obscureMessageForLogs(def, request, createObscureRequest()); // just to try

	}

	/**
	 * A failing message
	 */
	@Test
	public void testPartialMessage() throws Exception{

		String request = "{\"jsonrpc\":\"2.0\",\"method\":\"getActsByVisit\",\"params\":{\"auth\":\"UHN\",\"convertFromAppTerminology\":false,\"clientId\":\"clipdevsvc\",\"auditSourceId\":\"FORM_VIEWER\",\"visitId\":\"26200\n"
				+ "0218\",\"actToTerminologyLevel\":\"NONE\",\"convertObsToAppTerm\":false,\"statusCodes\":null,\"returnLoadedConcept\":false,\"loaded\":false,\"user\":{\"uid\":\"userId\",\"clientIp\":\"127.0.0.\n"
				+ "1\",\"attributes\":{}},\"clientPass\":\"Hospital20\",\"procCodes\":[\"1881\",\"1756\",\"1757\",\"1758\",\"5119\"]}}";

		ourLog.info("Request:\n{}", request);

		for (int i = 0; i < request.length(); i++) {

			String substring = request.substring(0, request.length() - i);
			StringReader reader = new StringReader(substring);

			JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();
			PersServiceVersionJsonRpc20 def = mock(PersServiceVersionJsonRpc20.class, new DefaultAnswer());
			PersMethod method = mock(PersMethod.class, new DefaultAnswer());

			when(def.getMethod("getActsByVisit")).thenReturn(method);
			when(def.getServerAuths()).thenReturn(new ArrayList<PersBaseServerAuth<?, ?>>());

			try {
				SrBeanIncomingRequest req = new SrBeanIncomingRequest();
				req.setPath("/");
				req.setQuery("");
				req.addHeader("Content-Type", "application/json");
				req.setRequestType(RequestType.POST);
				req.setInputReader(reader);
				svc.processInvocation(req,def);
				
				svc.obscureMessageForLogs(def, substring, createObscureRequest());
			} catch (InvocationRequestFailedException e) {
				// this is ok
			}

		}

	}

	private static Set<String> createObscureRequest(String... theStrings) {
		HashSet<String> retVal = new HashSet<>();
		retVal.add("AAAA");
		for (String string : theStrings) {
			retVal.add(string);
		}
		return retVal;
	}

	@Test
	public void testProcessInvocationWithNumbers() throws Exception{

		JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();
		PersServiceVersionJsonRpc20 def = mock(PersServiceVersionJsonRpc20.class);
		PersMethod method = mock(PersMethod.class);

		when(def.getMethod("someMethod")).thenReturn(method);
		when(def.getServerAuths()).thenReturn(new ArrayList<PersBaseServerAuth<?, ?>>());

		DefaultAnswer.setRunTime();

		@SuppressWarnings("unused")
		SrBeanProcessedRequest resp;

		//@formatter:off
		String request = // -
		"{ \"jsonrpc\": \"2.0\",\n" + // -
				"  \"method\": \"someMethod\",\n" + // -
				"  \"params\": 123\n" + // -
				"}"; // -
		//@formatter:on

		SrBeanIncomingRequest req = new SrBeanIncomingRequest();
		req.setPath("/");
		req.setQuery("");
		req.addHeader("Content-Type", "application/json");
		req.setRequestType(RequestType.POST);
		req.setInputReader(new StringReader(request));
		resp = svc.processInvocation(req,def);
		svc.obscureMessageForLogs(null, request, createObscureRequest()); // just to try
		svc.obscureMessageForLogs(null, request, createObscureRequest("params")); // just to try

		//@formatter:off
		request = // -
		"{ \"jsonrpc\": \"2.0\",\n" + // -
				"  \"method\": \"someMethod\",\n" + // -
				"  \"params\": -123.456\n" + // -
				"}"; // -
		//@formatter:on

		req.setInputReader(new StringReader(request));
		resp = svc.processInvocation(req,def);
		svc.obscureMessageForLogs(null, request, createObscureRequest()); // just to try
		svc.obscureMessageForLogs(null, request, createObscureRequest("params")); // just to try

		//@formatter:off
		request = // -
		"{ \"jsonrpc\": \"2.0\",\n" + // -
				"  \"method\": \"someMethod\",\n" + // -
				"  \"params\": [ 123.456, 876.543 ] \n" + // -
				"}"; // -
		//@formatter:on

		req.setInputReader(new StringReader(request));
		resp = svc.processInvocation(req,def);
		svc.obscureMessageForLogs(null, request, createObscureRequest()); // just to try
		svc.obscureMessageForLogs(null, request, createObscureRequest("params")); // just to try

		//@formatter:off
		request = // -
		"{ \"jsonrpc\": \"2.0\",\n" + // -
				"  \"method\": \"someMethod\",\n" + // -
				"  \"params\": [123.456, 876.543, \"222.222\"] \n" + // -
				"}"; // -
		//@formatter:on

		ourLog.info(request);
		req.setInputReader(new StringReader(request));
		resp = svc.processInvocation(req,def);
		svc.obscureMessageForLogs(null, request, createObscureRequest()); // just to try
		svc.obscureMessageForLogs(null, request, createObscureRequest("params")); // just to try

		//@formatter:off
		request = // -
		"{ \"jsonrpc\": \"2.0\",\n" + // -
				"  \"method\": \"someMethod\",\n" + // -
				"  \"params\": { \"hello\" :  876.543 } \n" + // -
				"}"; // -
		//@formatter:on

		req.setInputReader(new StringReader(request));
		resp = svc.processInvocation(req,def);
		svc.obscureMessageForLogs(null, request, createObscureRequest()); // just to try
		svc.obscureMessageForLogs(null, request, createObscureRequest("params")); // just to try

	}

	@Test
	public void testProcessServiceInvocation() throws Exception{

		String request = "{\n" + "  \"jsonrpc\": \"2.0\",\n" + "  \"method\": \"getCanonicalMappings\",\n" + "  \"params\": {\n" + "    \"request\": {\n"
				+ "      \"idAuthority\": \"2.16.840.1.113883.3.59.3:736\",\n" + "      \"idExt\": \"87170\"\n" + "    },\n" + "    \"clientId\": \"mock\",\n" + "    \"clientPass\": \"mock\"\n"
				+ "  }\n" + "}";

		JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();
		PersServiceVersionJsonRpc20 def = mock(PersServiceVersionJsonRpc20.class);
		PersMethod method = mock(PersMethod.class);

		when(def.getMethod("getCanonicalMappings")).thenReturn(method);
		when(def.getServerAuths()).thenReturn(new ArrayList<PersBaseServerAuth<?, ?>>());

		DefaultAnswer.setRunTime();
		SrBeanIncomingRequest req = new SrBeanIncomingRequest();
		req.setPath("/");
		req.setQuery("");
		req.addHeader("Content-Type", "application/json");
		req.setRequestType(RequestType.POST);
		req.setInputReader(new StringReader(request));
		SrBeanProcessedRequest resp = svc.processInvocation(req,def);

		String actualBody = resp.getMethodRequestBody();
		Assert.assertEquals(request, actualBody);
		Assert.assertEquals("application/json", resp.getMethodContentType());
		Assert.assertEquals(ResultTypeEnum.METHOD, resp.getResultType());
		Assert.assertSame(method, resp.getMethodDefinition());

		/*
		 * Try with null method params
		 */

		request = "{\n" + "  \"jsonrpc\": \"2.0\",\n" + "  \"method\": \"getCanonicalMappings\",\n" + "  \"params\": null\n" + "}";

		req.setInputReader(new StringReader(request));
		resp = svc.processInvocation(req,def);

		actualBody = resp.getMethodRequestBody();
		Assert.assertEquals(request, actualBody);

		/*
		 * Params as an array
		 */

		request = "{\n" + "  \"jsonrpc\": \"2.0\",\n" + "  \"method\": \"getCanonicalMappings\",\n" + "  \"params\": [\n" + "    1,\n" + "    2,\n" + "    3,\n" + "    4,\n" + "    5\n" + "  ]\n"
				+ "}";

		req.setInputReader(new StringReader(request));
		resp = svc.processInvocation(req,def);

		actualBody = resp.getMethodRequestBody();
		Assert.assertEquals(request, actualBody);
	}

	@Before
	public void before() {
		DefaultAnswer.setDesignTime();
	}

	@After
	public void after() {
		DefaultAnswer.setRunTime();
	}

	@Test
	public void testProcessInvocationResponse() throws Exception{

		JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();
		SrBeanIncomingResponse respBean = new SrBeanIncomingResponse();

		String response = "{\"jsonrpc\": \"2.0\", \"result\": -19, \"id\": 2}";
		respBean.setBody(response);
		SrBeanProcessedResponse resp = svc.processInvocationResponse(null, null, respBean);
		Assert.assertEquals(ResponseTypeEnum.SUCCESS, resp.getResponseType());

		response = "{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32601, \"message\": \"Method not found\"}, \"id\": \"1\"}";
		respBean.setBody(response);
		resp = svc.processInvocationResponse(null, null, respBean);
		Assert.assertEquals(ResponseTypeEnum.FAULT, resp.getResponseType());
		Assert.assertEquals("-32601", resp.getResponseFaultCode());
		Assert.assertEquals("Method not found", resp.getResponseFaultDescription());

	}

	@Test
	public void testProcessInvocationResponseWithNumbers() throws Exception{

		JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();
		SrBeanIncomingResponse respBean = new SrBeanIncomingResponse();

		respBean.setBody("{\"jsonrpc\": \"2.0\", \"result\": -19, \"id\": 2}");
		SrBeanProcessedResponse resp = svc.processInvocationResponse(null, null, respBean);
		Assert.assertEquals(ResponseTypeEnum.SUCCESS, resp.getResponseType());

		respBean.setBody("{\"jsonrpc\": \"2.0\", \"result\": -19.9, \"id\": 2}");
		resp = svc.processInvocationResponse(null, null, respBean);
		Assert.assertEquals(ResponseTypeEnum.SUCCESS, resp.getResponseType());

		respBean.setBody("{\"jsonrpc\": \"2.0\", \"result\": [ -19.1, 22.2 ], \"id\": 2}");
		resp = svc.processInvocationResponse(null, null, respBean);
		Assert.assertEquals(ResponseTypeEnum.SUCCESS, resp.getResponseType());

		respBean.setBody("{\"jsonrpc\": \"2.0\", \"result\": { \"hello\": 22.2 }, \"id\": 2}");
		resp = svc.processInvocationResponse(null, null, respBean);
		Assert.assertEquals(ResponseTypeEnum.SUCCESS, resp.getResponseType());

	}

	@Test
	public void testProcessLargeInvocationResponse() throws Exception{

		JsonRpc20ServiceInvoker svc = new JsonRpc20ServiceInvoker();
		SrBeanIncomingResponse respBean = new SrBeanIncomingResponse();

		String response = IOUtils.toString(JsonRpc20ResponseValidatorTest.class.getResourceAsStream("/badjsonresponse.json"));
		respBean.setBody(response);
		SrBeanProcessedResponse resp = svc.processInvocationResponse(null, null, respBean);
		Assert.assertEquals(ResponseTypeEnum.SUCCESS, resp.getResponseType());

		String actual = resp.getResponseBody();
		assertEquals(response, actual);

	}

}
