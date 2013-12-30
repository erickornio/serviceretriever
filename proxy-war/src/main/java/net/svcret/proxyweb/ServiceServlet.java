package net.svcret.proxyweb;

import static net.svcret.ejb.util.HttpUtil.sendFailure;
import static net.svcret.ejb.util.HttpUtil.sendSecurityFailure;
import static net.svcret.ejb.util.HttpUtil.sendSuccessfulResponse;
import static net.svcret.ejb.util.HttpUtil.sendThrottleQueueFullFailure;
import static net.svcret.ejb.util.HttpUtil.sendUnknownLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.svcret.ejb.api.SrBeanIncomingRequest;
import net.svcret.ejb.api.IServiceOrchestrator;
import net.svcret.ejb.api.SrBeanOutgoingResponse;
import net.svcret.ejb.api.RequestType;
import net.svcret.ejb.ex.InvocationFailedDueToInternalErrorException;
import net.svcret.ejb.ex.InvocationRequestOrResponseFailedException;
import net.svcret.ejb.ex.ProcessingException;
import net.svcret.ejb.ex.SecurityFailureException;
import net.svcret.ejb.ex.UnknownRequestException;
import net.svcret.ejb.throttle.ThrottleException;
import net.svcret.ejb.throttle.ThrottleQueueFullException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterators;

@WebServlet(asyncSupported = true, loadOnStartup = 1, urlPatterns = { "/" })
public class ServiceServlet extends HttpServlet {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ServiceServlet.class);

	private static final long serialVersionUID = 1L;

	@EJB
	private IServiceOrchestrator myOrch;

	@Override
	protected void doGet(HttpServletRequest theReq, HttpServletResponse theResp) throws ServletException, IOException {
		handle(theReq, theResp, RequestType.GET);
	}

	@Override
	protected void doPost(HttpServletRequest theReq, HttpServletResponse theResp) throws ServletException, IOException {
		handle(theReq, theResp, RequestType.POST);
	}

	private void handle(HttpServletRequest theReq, HttpServletResponse theResp, RequestType get) throws IOException, ServletException {
		try {
			doHandle(theReq, theResp, get);
		} catch (IOException e) {
			throw e;
		} catch (Throwable e) {
			ourLog.error("Failed to process service request", e);
			throw new ServletException(e);
		}
	}

	private void doHandle(HttpServletRequest theReq, HttpServletResponse theResp, RequestType requestAction) throws IOException {
		long start = System.currentTimeMillis();

		String path = theReq.getRequestURI().substring(theReq.getContextPath().length());
		try {
			path = new URLCodec().decode(path);
		} catch (DecoderException e1) {
			ourLog.error("Failed to decode path", e1);
			sendFailure(theResp, "Failed to decode path: " + e1.getMessage());
		}

		String contextPath = theReq.getContextPath();
		String query = "?" + theReq.getQueryString();
		String requestURL = theReq.getRequestURL().toString();
		String requestHostIp = theReq.getRemoteAddr();
		String protocol = theReq.getProtocol();
		String requestURI = theReq.getRequestURI();
		String base = extractBase(contextPath, requestURL);

		ourLog.debug("New {} request at path[{}] and base[{}] and context path[{}]", new Object[] { requestAction.name(), path, base, contextPath });

		SrBeanOutgoingResponse response;
		try {
			SrBeanIncomingRequest request = new SrBeanIncomingRequest();
			request.setRequestType(requestAction);
			request.setRequestHostIp(StringUtils.defaultString(requestHostIp, "UNKNOWN"));
			request.setPath(path);
			request.setRequestFullUri(requestURI);
			request.setQuery(query);
			request.setBase(base);
			request.setProtocol(protocol);
			request.setContextPath(contextPath);
			request.setInputReader(theReq.getReader());
			request.setRequestTime(new Date(start));

			Map<String, List<String>> requestHeaders = new HashMap<String, List<String>>();
			for (Iterator<String> nameIter = Iterators.forEnumeration(theReq.getHeaderNames()); nameIter.hasNext();) {
				String nextName = nameIter.next();
				ArrayList<String> values = Collections.list(theReq.getHeaders(nextName));
				requestHeaders.put(nextName, values);
			}
			request.setRequestHeaders(requestHeaders);

			response = myOrch.handleServiceRequest(request);
		} catch (UnknownRequestException e) {
			ourLog.info("Unknown request location: {} - Message {}", path, e.getMessage());
			sendUnknownLocation(theResp, e);
			return;
		} catch (ProcessingException e) {
			ourLog.info("Processing Failure", e);
			sendFailure(theResp, e.toString());
			return;
		} catch (InvocationRequestOrResponseFailedException e) {
			ourLog.info("Processing Failure", e.getMessage());
			sendFailure(theResp, e.toString());
			return;
		} catch (InvocationFailedDueToInternalErrorException e) {
			ourLog.info("Processing Failure", e.getMessage());
			sendFailure(theResp, e.toString());
			return;
		} catch (SecurityFailureException e) {
			ourLog.info("Security Failure accessing URL: {}", theReq.getRequestURL());
			sendSecurityFailure(theResp);
			return;
		} catch (ThrottleException e) {

			AsyncContext asyncContext = theReq.startAsync();

			try {
				myOrch.enqueueThrottledRequest(e, asyncContext);
			} catch (ThrottleQueueFullException e1) {
				ourLog.info("Request was throttled and queue was full for URL: {}", theReq.getRequestURL());
				sendThrottleQueueFullFailure(theResp);
			}
			return;

		} catch (ThrottleQueueFullException e) {
			ourLog.info("Request was throttled and queue was full for URL: {}", theReq.getRequestURL());
			sendThrottleQueueFullFailure(theResp);
			return;
		} catch (Throwable e) {
			ourLog.info("Processing Failure", e);
			sendFailure(theResp, e.getMessage());
			return;
		}

		sendSuccessfulResponse(theResp, response);

		long delay = System.currentTimeMillis() - start;
		ourLog.info("Handled {} request at path[{}] with {} byte response in {} ms", new Object[] { requestAction.name(), path, response.getResponseBody().length(), delay });
	}

	@Override
	public void init(ServletConfig theConfig) throws ServletException {
		ourLog.info("Starting servlet with path: " + theConfig.getServletContext().getContextPath());
		ourLog.info("Real path: " + theConfig.getServletContext().getRealPath("/"));
	}

	private static String extractBase(String contextPath, String requestURL) {
		int hostIndex = requestURL.indexOf("//") + 2;
		int pathStart = requestURL.indexOf('/', hostIndex);
		return requestURL.substring(0, pathStart) + contextPath;
	}

}
