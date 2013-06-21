package net.svcret.ejb.api;

import java.io.Reader;
import java.util.List;
import java.util.Map;

public class HttpRequestBean {

	private Reader myInputReader;
	private String myPath;
	private String myQuery;
	private Map<String, List<String>> myRequestHeaders;
	private String myRequestHostIp;
	private RequestType myRequestType;

	/**
	 * @return the inputReader
	 */
	public Reader getInputReader() {
		return myInputReader;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return myPath;
	}

	/**
	 * @return the query
	 */
	public String getQuery() {
		return myQuery;
	}

	/**
	 * @return the requestHeaders
	 */
	public Map<String, List<String>> getRequestHeaders() {
		return myRequestHeaders;
	}

	/**
	 * @return the requestHostIp
	 */
	public String getRequestHostIp() {
		return myRequestHostIp;
	}

	/**
	 * @return the requestType
	 */
	public RequestType getRequestType() {
		return myRequestType;
	}

	/**
	 * @param theInputReader
	 *            the inputReader to set
	 */
	public void setInputReader(Reader theInputReader) {
		myInputReader = theInputReader;
	}

	/**
	 * @param thePath
	 *            the path to set
	 */
	public void setPath(String thePath) {
		myPath = thePath;
	}

	/**
	 * @param theQuery
	 *            the query to set
	 */
	public void setQuery(String theQuery) {
		myQuery = theQuery;
	}

	public void setRequestHeaders(Map<String, List<String>> theRequestHeaders) {
		myRequestHeaders = theRequestHeaders;
	}

	/**
	 * @param theRequestHostIp
	 *            the requestHostIp to set
	 */
	public void setRequestHostIp(String theRequestHostIp) {
		myRequestHostIp = theRequestHostIp;
	}

	/**
	 * @param theRequestType
	 *            the requestType to set
	 */
	public void setRequestType(RequestType theRequestType) {
		myRequestType = theRequestType;
	}

}