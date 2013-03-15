package ca.uhn.sail.proxy.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UrlPoolBean {

	private List<String> myAlternateUrls;
	private long myConnectTimeoutMillis;
	private String myPreferredUrl;
	private long myReadTimeoutMillis;

	/**
	 * @return the alternateUrls
	 */
	public List<String> getAlternateUrls() {
		if (myAlternateUrls == null) {
			return Collections.emptyList();
		}
		return myAlternateUrls;
	}

	/**
	 * @return the connectTimeoutMillis
	 */
	public long getConnectTimeoutMillis() {
		return myConnectTimeoutMillis;
	}

	/**
	 * @return the preferredUrl
	 */
	public String getPreferredUrl() {
		return myPreferredUrl;
	}

	/**
	 * @return the readTimeoutMillis
	 */
	public long getReadTimeoutMillis() {
		return myReadTimeoutMillis;
	}

	/**
	 * @param theAlternateUrls
	 *            the alternateUrls to set
	 */
	public void setAlternateUrls(List<String> theAlternateUrls) {
		myAlternateUrls = theAlternateUrls;
	}

	/**
	 * @param theConnectTimeoutMillis the connectTimeoutMillis to set
	 */
	public void setConnectTimeoutMillis(long theConnectTimeoutMillis) {
		myConnectTimeoutMillis = theConnectTimeoutMillis;
	}


	/**
	 * @param thePreferredUrl
	 *            the preferredUrl to set
	 */
	public void setPreferredUrl(String thePreferredUrl) {
		myPreferredUrl = thePreferredUrl;
	}

	/**
	 * @param theReadTimeoutMillis the readTimeoutMillis to set
	 */
	public void setReadTimeoutMillis(long theReadTimeoutMillis) {
		myReadTimeoutMillis = theReadTimeoutMillis;
	}

	public Collection<String> getAllUrls() {
		ArrayList<String> retVal = new ArrayList<String>();
		retVal.add(getPreferredUrl());
		retVal.addAll(getAlternateUrls());
		return retVal;
	}


}
