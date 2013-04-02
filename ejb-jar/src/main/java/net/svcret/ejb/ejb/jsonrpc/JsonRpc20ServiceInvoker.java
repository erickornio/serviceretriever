package net.svcret.ejb.ejb.jsonrpc;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import net.svcret.ejb.model.entity.jsonrpc.PersServiceVersionJsonRpc20;

import org.apache.commons.io.IOUtils;

import ca.uhn.sail.proxy.api.HttpResponseBean;
import ca.uhn.sail.proxy.api.IResponseValidator;
import ca.uhn.sail.proxy.api.IServiceInvoker;
import ca.uhn.sail.proxy.api.InvocationResponseResultsBean;
import ca.uhn.sail.proxy.api.InvocationResultsBean;
import ca.uhn.sail.proxy.api.RequestType;
import ca.uhn.sail.proxy.api.ResponseTypeEnum;
import ca.uhn.sail.proxy.ex.InternalErrorException;
import ca.uhn.sail.proxy.ex.ProcessingException;
import ca.uhn.sail.proxy.ex.UnknownRequestException;
import ca.uhn.sail.proxy.model.entity.PersServiceVersionMethod;
import ca.uhn.sail.proxy.util.Validate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class JsonRpc20ServiceInvoker implements IServiceInvoker<PersServiceVersionJsonRpc20> {

	static final String TOKEN_ID = "id";
	static final String TOKEN_PARAMS = "params";
	static final String TOKEN_METHOD = "method";

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(JsonRpc20ServiceInvoker.class);

	static final String TOKEN_JSONRPC = "jsonrpc";
	static final String TOKEN_RESULT = "result";
	static final String TOKEN_ERROR = "error";


	public static void consumeEqually(JsonReader theJsonReader, JsonWriter theJsonWriter) throws IOException, ProcessingException {
		int objectDepth = 0;
		int arrayDepth = 0;

		JsonToken next = theJsonReader.peek();
		switch (next) {
			case NULL:
				theJsonReader.nextNull();
				theJsonWriter.nullValue();
				return;
			case BEGIN_OBJECT:
				theJsonReader.beginObject();
				theJsonWriter.beginObject();
				objectDepth++;
				break;
			case BEGIN_ARRAY:
				theJsonReader.beginArray();
				theJsonWriter.beginArray();
				arrayDepth++;
				break;
			case BOOLEAN:
				theJsonWriter.value(theJsonReader.nextBoolean());
				return;
			case NUMBER:
				theJsonWriter.value(theJsonReader.nextLong());
				return;
			case STRING:
				theJsonWriter.value(theJsonReader.nextString());
				break;
			case NAME:
			case END_ARRAY:
			case END_DOCUMENT:
			case END_OBJECT:
			default:
				throw new ProcessingException("Found token of " + next + " in an unexpected place");
		}

		do {
			next = theJsonReader.peek();
			switch (next) {
				case NULL:
					theJsonReader.nextNull();
					theJsonWriter.nullValue();
					return;
				case BEGIN_ARRAY:
					theJsonReader.beginArray();
					theJsonWriter.beginArray();
					arrayDepth++;
					break;
				case BEGIN_OBJECT:
					theJsonReader.beginObject();
					theJsonWriter.beginObject();
					objectDepth++;
					break;
				case BOOLEAN:
					theJsonWriter.value(theJsonReader.nextBoolean());
					break;
				case END_ARRAY:
					theJsonReader.endArray();
					theJsonWriter.endArray();
					arrayDepth--;
					break;
				case END_DOCUMENT:
					break;
				case END_OBJECT:
					theJsonReader.endObject();
					theJsonWriter.endObject();
					objectDepth--;
					break;
				case NAME:
					theJsonWriter.name(theJsonReader.nextName());
					break;
				case NUMBER:
					theJsonWriter.value(theJsonReader.nextLong());
					break;
				case STRING:
					theJsonWriter.value(theJsonReader.nextString());
					break;
			}
		} while (objectDepth > 0 || arrayDepth > 0);

	}


	public static void consumeEqually(JsonReader theJsonReader) throws IOException, ProcessingException {
		int objectDepth = 0;
		int arrayDepth = 0;

		JsonToken next = theJsonReader.peek();
		switch (next) {
			case BEGIN_OBJECT:
				theJsonReader.beginObject();
				objectDepth++;
				break;
			case BEGIN_ARRAY:
				arrayDepth++;
				break;
			case NULL:
				theJsonReader.nextNull();
				return;
			case BOOLEAN:
				theJsonReader.nextBoolean();
				return;
			case END_ARRAY:
			case END_DOCUMENT:
			case END_OBJECT:
			case NAME:
				throw new IllegalStateException("Not expected here");
			case NUMBER:
				theJsonReader.nextLong();
				return;
			case STRING:
				theJsonReader.nextString();
				return;
		}

		do {
			next = theJsonReader.peek();
			switch (next) {
				case NULL:
					theJsonReader.nextNull();
					return;
				case BEGIN_ARRAY:
					theJsonReader.beginArray();
					arrayDepth++;
					break;
				case BEGIN_OBJECT:
					theJsonReader.beginObject();
					objectDepth++;
					break;
				case BOOLEAN:
					theJsonReader.nextBoolean();
					break;
				case END_ARRAY:
					theJsonReader.endArray();
					arrayDepth--;
					break;
				case END_DOCUMENT:
					break;
				case END_OBJECT:
					theJsonReader.endObject();
					objectDepth--;
					break;
				case NAME:
					theJsonReader.nextName();
					break;
				case NUMBER:
					theJsonReader.nextLong();
					break;
				case STRING:
					theJsonReader.nextString();
					break;
			}
		} while (objectDepth > 0 || arrayDepth > 0);

	}


	@Override
	public InvocationResultsBean processInvocation(PersServiceVersionJsonRpc20 theServiceDefinition, RequestType theRequestType, String thePath, String theQuery, Reader theReader) throws InternalErrorException, UnknownRequestException, IOException, ProcessingException {
		Validate.throwIllegalArgumentExceptionIfNull("Reader", theReader);
		if (theRequestType != RequestType.POST) {
			throw new UnknownRequestException("This service requires all requests to be of type HTTP POST");
		}

		StringWriter stringWriter = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(stringWriter);

		jsonWriter.setLenient(true);
		jsonWriter.setSerializeNulls(true);
		jsonWriter.setIndent("  ");

		JsonReader jsonReader = new JsonReader(theReader);
		jsonReader.setLenient(true);

		String method = null;

		jsonReader.beginObject();
		jsonWriter.beginObject();

		while (jsonReader.hasNext()) {

			String nextName = jsonReader.nextName();
			jsonWriter.name(nextName);

			if (TOKEN_JSONRPC.equals(nextName)) {

				String rpcVal = jsonReader.nextString();
				jsonWriter.value(rpcVal);
				ourLog.debug("JsonRpc version in request: {}", rpcVal);

			} else if (TOKEN_METHOD.equals(nextName)) {

				method = jsonReader.nextString();
				jsonWriter.value(method);
				ourLog.debug("JsonRpc method name: {}", method);

			} else if (TOKEN_PARAMS.equals(nextName)) {

				consumeEqually(jsonReader, jsonWriter);

			} else if (TOKEN_ID.equals(nextName)) {

				String requestId = jsonReader.nextString();
				jsonWriter.value(requestId);
				ourLog.debug("JsonRpc request ID: {}", requestId);

			}

		}

		jsonReader.endObject();
		jsonWriter.endObject();

		jsonReader.close();
		jsonWriter.close();

		InvocationResultsBean retVal = new InvocationResultsBean();

		String requestBody = stringWriter.toString();
		String contentType = "application/json";
		Map<String, String> headers = new HashMap<String, String>();
		PersServiceVersionMethod methodDef = theServiceDefinition.getMethod(method);
		retVal.setResultMethod(methodDef, requestBody, contentType, headers);

		return retVal;
	}


	@Override
	public InvocationResponseResultsBean processInvocationResponse(HttpResponseBean theResponse) throws ProcessingException {
		InvocationResponseResultsBean retVal = new InvocationResponseResultsBean();

		String body = theResponse.getBody();
		StringReader reader = new StringReader(body);
		
		JsonReader jsonReader = new JsonReader(reader);
		try {
			jsonReader.beginObject();

			while (jsonReader.hasNext()) {

				String nextName = jsonReader.nextName();

				if (JsonRpc20ServiceInvoker.TOKEN_JSONRPC.equals(nextName)) {

					String rpcVal = jsonReader.nextString();
					ourLog.debug("JSON-RPC version in response: {}", rpcVal);

				} else if (JsonRpc20ServiceInvoker.TOKEN_RESULT.equals(nextName)) {

					JsonRpc20ServiceInvoker.consumeEqually(jsonReader);

				} else if (JsonRpc20ServiceInvoker.TOKEN_ERROR.equals(nextName)) {

					ourLog.debug("Response is fault");
					
					Gson gson = new GsonBuilder().create();
					JsonErrorType error = gson.fromJson(jsonReader, JsonErrorType.class);
					assert error != null;
					
					retVal.setResponseType(ResponseTypeEnum.FAULT);
					retVal.setResponseFaultCode(Integer.toString(error.getCode()));
					retVal.setResponseFaultDescription(error.getMessage());
					
				} else if (JsonRpc20ServiceInvoker.TOKEN_ID.equals(nextName)) {

					String requestId = jsonReader.nextString();
					ourLog.debug("JsonRpc request ID: {}", requestId);

				}

			}

		} catch (IOException e) {
			throw new ProcessingException(e);
		} finally {
			IOUtils.closeQuietly(jsonReader);
		}

		retVal.setResponseBody(body);
		retVal.setResponseContentType("application/json");
		retVal.setResponseHeaders(new HashMap<String, String>());
		if (retVal.getResponseType() == null) {
			retVal.setResponseType(ResponseTypeEnum.SUCCESS);
		}
		
		return retVal;
	}


	@Override
	public IResponseValidator provideInvocationResponseValidator() {
		return new JsonRpc20ResponseValidator();
	}

}