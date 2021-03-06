package net.svcret.core.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import net.svcret.admin.api.ProcessingException;
import net.svcret.admin.api.UnexpectedFailureException;
import net.svcret.admin.shared.enm.AuthorizationOutcomeEnum;
import net.svcret.admin.shared.enm.ResponseTypeEnum;
import net.svcret.core.api.IConfigService;
import net.svcret.core.api.SrBeanIncomingRequest;
import net.svcret.core.api.SrBeanIncomingResponse;
import net.svcret.core.api.SrBeanProcessedRequest;
import net.svcret.core.api.SrBeanProcessedResponse;
import net.svcret.core.model.entity.BasePersServiceVersion;
import net.svcret.core.model.entity.PersConfig;
import net.svcret.core.model.entity.PersMethod;
import net.svcret.core.model.entity.PersUser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;

@Service
public class FilesystemAuditLoggerBean implements IFilesystemAuditLogger {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FilesystemAuditLoggerBean.class);
	private static SimpleDateFormat ourLogDateFormat = new SimpleDateFormat("yyyyMMdd-HH':00:00'");
	private static final Pattern PARAM_VALUE_WHITESPACE = Pattern.compile("\\r|\\n", Pattern.MULTILINE);
	private File myAuditPath;
	@Autowired
	private IConfigService myConfigSvc;
	private volatile int myFailIfQueueExceedsSize = 10000;
	private ReentrantLock myFlushLockAuditRecord = new ReentrantLock();
	private SimpleDateFormat myItemDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS Z");
	private AtomicLong myLastAuditRecordFlush = new AtomicLong(System.currentTimeMillis());
	private Map<String, MyBufferedFileWriter> myLogFileWriters = new HashMap<String, MyBufferedFileWriter>();
	private volatile int myTriggerQueueFlushAtMillisSinceLastFlush = 60 * 1000;
	private volatile int myTriggerQueueFlushAtQueueSize = 100;

	private ConcurrentLinkedQueue<UnflushedAuditRecord> myUnflushedAuditRecord = new ConcurrentLinkedQueue<UnflushedAuditRecord>();

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public void flushAuditEventsIfNeeded() {
		try {
			flushIfNeccesary();
		} catch (Exception e) {
			ourLog.error("Failed to flush transactions", e);
		}
	}

	public void forceFlush() throws ProcessingException, UnexpectedFailureException {
		flushWithinLockedContext();
	}

	@PostConstruct
	public void initialize() throws Exception {
		ourLog.info("Initializing filesystem audit logger service");

		if (myAuditPath == null) {
			String path = new File("audit").getAbsolutePath();
			ourLog.info("Going to use the following path for filesystem auditing: {}", path);

			myAuditPath = new File(path);
			if (!myAuditPath.exists()) {
				ourLog.info("Path does not exist, going to create it: {}", myAuditPath.getAbsolutePath());
				if (!myAuditPath.mkdirs()) {
					throw new ProcessingException("Failed to create path (do we have permission?): " + myAuditPath.getAbsoluteFile());
				}
			}
		} else {
			if (!myAuditPath.exists()) {
				if (!myAuditPath.mkdirs()) {
					throw new Exception("Failed to create path: "+myAuditPath);
				}
			}
		}

		if (!myAuditPath.isDirectory()) {
			throw new ProcessingException("Path exists but is not a directory: " + myAuditPath.getAbsoluteFile());
		}

	}

	public void setAuditPath(File theAuditPath) {
		myAuditPath = theAuditPath;
	}

	@Override
	public void recordServiceTransaction(SrBeanIncomingRequest theRequest, BasePersServiceVersion theSvcVer, PersMethod theMethod, PersUser theUser, String theRequestBody, SrBeanProcessedResponse theInvocationResponse, SrBeanIncomingResponse theHttpResponse,
			AuthorizationOutcomeEnum theAuthorizationOutcome, SrBeanProcessedRequest theInvocationResults) throws ProcessingException, UnexpectedFailureException {

		validateQueueSize();

		UnflushedAuditRecord auditLog = new UnflushedAuditRecord(theRequest.getRequestTime(), theRequest, theSvcVer, theMethod, theUser, theRequestBody, theInvocationResults, theInvocationResponse, theHttpResponse, theAuthorizationOutcome, AuditLogTypeEnum.SVCVER);
		myUnflushedAuditRecord.add(auditLog);

		flushIfNeccesary();
	}

	@Override
	public void recordUserTransaction(SrBeanIncomingRequest theRequest, BasePersServiceVersion theSvcVer, PersMethod theMethod, PersUser theUser, String theRequestBody, SrBeanProcessedResponse theInvocationResponse, SrBeanIncomingResponse theHttpResponse,
			AuthorizationOutcomeEnum theAuthorizationOutcome, SrBeanProcessedRequest theInvocationResults) throws ProcessingException, UnexpectedFailureException {

		validateQueueSize();

		UnflushedAuditRecord auditLog = new UnflushedAuditRecord(theRequest.getRequestTime(), theRequest, theSvcVer, theMethod, theUser, theRequestBody, theInvocationResults, theInvocationResponse, theHttpResponse, theAuthorizationOutcome, AuditLogTypeEnum.USER);
		myUnflushedAuditRecord.add(auditLog);

		flushIfNeccesary();
	}

	@VisibleForTesting
	public void setConfigServiceForUnitTests(IConfigService theCfgSvc) {
		myConfigSvc = theCfgSvc;

	}

	private void addItem(Writer writer, String key, String value) throws IOException {
		writer.append(key);
		writer.append(": ");
		writer.append(formatParamValue(value));
		writer.append("\n");
	}

	private void closeAndRemoveWriter(UnflushedAuditRecord theNext) {
		String fileName = theNext.createLogFile();
		MyBufferedFileWriter existing = myLogFileWriters.remove(fileName);
		if (existing != null) {
			try {
				existing.close();
			} catch (IOException e) {
				ourLog.debug("Failed to close writer", e);
			}
		}
	}

	private void flushIfNeccesary() throws ProcessingException, UnexpectedFailureException {
		if (myUnflushedAuditRecord.size() < myTriggerQueueFlushAtQueueSize) {
			if (myLastAuditRecordFlush.get() + myTriggerQueueFlushAtMillisSinceLastFlush > System.currentTimeMillis()) {
				return;
			}
		}

		if (!myFlushLockAuditRecord.tryLock()) {
			return;
		}

		try {
			flushWithinLockedContext();
		} finally {
			myFlushLockAuditRecord.unlock();
		}
	}

	private void flushWithinLockedContext() throws ProcessingException, UnexpectedFailureException {
		ourLog.info("About to begin flushing approximately {} records from audit log queue", myUnflushedAuditRecord.size());
		long start = System.currentTimeMillis();

		Map<String, String> lookups = new HashMap<String, String>();
		PersConfig config = myConfigSvc.getConfig();

		int count = 0;
		while (true) {

			UnflushedAuditRecord next = myUnflushedAuditRecord.peek();
			if (next == null) {
				break;
			}

			try {
				flushWithinLockedContext(lookups, config, next);
			} catch (IOException e) {
				ourLog.info("Failed while writing audit log, going to retry once: {}", e.toString());
				closeAndRemoveWriter(next);
				try {
					flushWithinLockedContext(lookups, config, next);
				} catch (IOException e1) {
					closeAndRemoveWriter(next);
					ourLog.error("Failed to write audit log", e);
					throw new ProcessingException("Failed to write to audit log", e);
				}
			}

			myUnflushedAuditRecord.poll();
			count++;
		}

		for (Iterator<Entry<String, MyBufferedFileWriter>> iter = myLogFileWriters.entrySet().iterator(); iter.hasNext();) {
			Entry<String, MyBufferedFileWriter> next = iter.next();
			try {
				next.getValue().flush();
			} catch (IOException e) {
				ourLog.error("Failed to flush writer", e);
				IOUtils.closeQuietly(next.getValue());
				iter.remove();
			}
		}

		long delay = System.currentTimeMillis() - start;
		ourLog.info("Finished flushing {} audit records in {}ms", count, delay);
	}

	private void flushWithinLockedContext(Map<String, String> lookups, PersConfig config, UnflushedAuditRecord next) throws IOException {
		String fileName = next.createLogFile();
		if (!myLogFileWriters.containsKey(fileName)) {
			MyBufferedFileWriter writer = new MyBufferedFileWriter(new File(myAuditPath, fileName));
			myLogFileWriters.put(fileName, writer);
		}

		if (!lookups.containsKey(next.myRequestHostIp)) {
			try {
				InetAddress addr = InetAddress.getByName(next.myRequestHostIp);
				String host = addr.getHostName();
				lookups.put(next.myRequestHostIp, host);
			} catch (Throwable e) {
				ourLog.debug("Failed to lookup hostname", e);
				lookups.put(next.myRequestHostIp, null);
			}
		}

		MyBufferedFileWriter writer = myLogFileWriters.get(fileName);

		if (config.getAuditLogDisableIfDiskCapacityBelowMb() != null) {
			long freeSpaceMb = (writer.getBytesFree() / FileUtils.ONE_MB);
			if (freeSpaceMb < config.getAuditLogDisableIfDiskCapacityBelowMb()) {
				ourLog.debug("Not saving audit record because only {} Mb free (threshold is {})", freeSpaceMb, config.getAuditLogDisableIfDiskCapacityBelowMb());
				// TODO: add a line at the bottom of the file indicating that
				// saving was suspended
			}
		} else {

			addItem(writer, "Date", myItemDateFormat.format(next.myRequestTime));
			addItem(writer, "Latency", Long.toString(next.myTransactionMillis));
			if (next.myThrottleTimeIfAny != null) {
				addItem(writer, "ThrottleLatency", Long.toString(next.myThrottleTimeIfAny));
			}
			addItem(writer, "ResponseType", next.myResponseType.name());
			if (StringUtils.isNotBlank(next.myFailureDescription)) {
				addItem(writer, "FailureDescription", next.myFailureDescription);
			}
			addItem(writer, "RequestorIp", next.myRequestHostIp + " (" + lookups.get(next.myRequestHostIp) + ")");
			addItem(writer, "DomainId", next.myDomainId);
			addItem(writer, "ServiceId", next.myServiceId);
			addItem(writer, "ServiceVersionId", next.myServiceVersionId);
			addItem(writer, "ServiceVersionPid", Long.toString(next.myServiceVersionPid));
			if (StringUtils.isNotBlank(next.myMethodName)) {
				addItem(writer, "MethodName", next.myMethodName);
			}
			if (next.mySuccessImplementationUrl != null) {
				addItem(writer, "HandledByUrl", "[" + next.mySuccessImplementationUrlId + "] " + next.mySuccessImplementationUrl);
			}
			if (next.myUserPid == null) {
				addItem(writer, "User", "none");
			} else {
				addItem(writer, "User", "[" + next.myUserPid + "] " + next.myUsername);
			}
			if (next.myAuthorizationOutcome != null) {
				addItem(writer, "AuthorizationOutcome", next.myAuthorizationOutcome.name());
			}
			for (Entry<String, String> nextPair : next.getPropertyCaptures().entrySet()) {
				addItem(writer, "PropertyCapture", "[" + nextPair.getKey() + "] " + nextPair.getValue());
			}
			writer.append("Request:\n");
			String formatedRequest = formatMessage(next.myRequestBody);
			writer.append(formatedRequest);

			if (next.myResponseBody != null) {
				writer.append("\nResponse:\n");
				String formattedResponse = formatMessage(next.myResponseBody);
				writer.append(formattedResponse);
			}

			writer.append("\n\n");
			writer.flush();
		}
	}

	private CharSequence formatParamValue(String theValue) {
		return PARAM_VALUE_WHITESPACE.matcher(theValue).replaceAll(" ");
	}

	private void validateQueueSize() throws ProcessingException {
		if (myUnflushedAuditRecord.size() > myFailIfQueueExceedsSize) {
			throw new ProcessingException("Audit log queue has exceeded maximum threshold of " + myFailIfQueueExceedsSize);
		}
	}

	/**
	 * Cleans up line separators and indents each line of the string by two
	 * spaces
	 */
	@SuppressWarnings("fallthrough")
	static String formatMessage(String theInput) {
		if (theInput == null) {
			return null;
		}

		StringBuilder output = new StringBuilder(theInput.length() + 100);
		output.append("  ");

		for (int i = 0; i < theInput.length(); i++) {

			char nextChar = theInput.charAt(i);

			switch (nextChar) {
			case '\r':
				char followingChar = (i + 1) < theInput.length() ? theInput.charAt(i + 1) : ' ';
				if (followingChar == '\n') {
					continue;
				}
				// fall through

			case '\n':
				output.append("\n  ");
				continue;

			default:
				// nothing
			}

			output.append(nextChar);
		}

		// Trim trailing newlines
		while (output.length() > 2) {
			char lastChar = output.charAt(output.length() - 1);
			if (Character.isWhitespace(lastChar)) {
				output.setLength(output.length() - 1);
			} else {
				break;
			}
		}

		return output.toString();
	}

	public enum AuditLogTypeEnum {
		/*
		 * Don't put underscores in these names!
		 */
		SVCVER {
			@Override
			public String createLogFileName(UnflushedAuditRecord theRecord) {
				return "svcver_" + theRecord.getServiceVersionPid() + "_" + theRecord.getServiceId() + "_" + theRecord.getServiceVersionId() + ".log_" + theRecord.formatEventDateForLogFilename();
			}
		},

		USER {
			@Override
			public String createLogFileName(UnflushedAuditRecord theRecord) {
				return "user_" + theRecord.getUserPid() + "_" + theRecord.getUsername() + ".log_" + theRecord.formatEventDateForLogFilename();
			}
		};

		public abstract String createLogFileName(UnflushedAuditRecord theRecord);

	}

	private class MyBufferedFileWriter extends BufferedWriter {

		private File myFile;

		public MyBufferedFileWriter(File theFile) throws IOException {
			super(new FileWriter(theFile, true));
			myFile = theFile;
		}

		public long getBytesFree() {
			return myFile.getFreeSpace();
		}

	}

	private class UnflushedAuditRecord {

		private AuditLogTypeEnum myAuditRecordType;
		private AuthorizationOutcomeEnum myAuthorizationOutcome;
		private String myDomainId;
		private String myFailureDescription;
		private Map<String, List<String>> myHeaders;
		private String mySuccessImplementationUrl;
		private String mySuccessImplementationUrlId;
		private String myMethodName;
		private String myRequestBody;
		private String myRequestHostIp;
		private Date myRequestTime;
		private String myResponseBody;
		private Map<String, List<String>> myResponseHeaders;
		private ResponseTypeEnum myResponseType;
		private String myServiceId;
		private String myServiceVersionId;
		private Long myServiceVersionPid;
		private Long myTransactionMillis;
		private String myUsername;
		private Long myUserPid;
		private Map<String, String> myPropertyCaptures;
		private Long myThrottleTimeIfAny;

		public UnflushedAuditRecord(Date theRequestTime, SrBeanIncomingRequest theRequest, BasePersServiceVersion theSvcVer, PersMethod theMethod, PersUser theUser, String theRequestBody, SrBeanProcessedRequest theInvocationResults, SrBeanProcessedResponse theInvocationResponse,
				SrBeanIncomingResponse theHttpResponse, AuthorizationOutcomeEnum theAuthorizationOutcome, AuditLogTypeEnum theType) {

			if (theType == AuditLogTypeEnum.USER && theUser == null) {
				throw new IllegalArgumentException("No user provided for USER record");
			}

			myAuditRecordType = theType;
			myRequestTime = theRequestTime;
			myHeaders = theRequest.getRequestHeaders();
			myRequestBody = theRequestBody;
			if (theHttpResponse != null && theHttpResponse.getSuccessfulUrl() != null) {
				mySuccessImplementationUrlId = theHttpResponse.getSuccessfulUrl().getUrlId();
				mySuccessImplementationUrl = theHttpResponse.getSuccessfulUrl().getUrl();
			}
			myRequestHostIp = theRequest.getRequestHostIp();
			myResponseHeaders = theInvocationResponse.getResponseHeaders();
			myResponseBody = theInvocationResponse.getObscuredResponseBody();
			myResponseType = theInvocationResponse.getResponseType();
			myFailureDescription = theInvocationResponse.getResponseFailureDescription();
			myAuthorizationOutcome = theAuthorizationOutcome;
			myMethodName = theMethod != null ? theMethod.getName() : null;
			if (theUser != null) {
				myUsername = theUser.getUsername();
				myUserPid = theUser.getPid();
			} else {
				myUsername = null;
			}
			myServiceVersionId = theSvcVer.getVersionId();
			myServiceId = theSvcVer.getService().getServiceId();
			myDomainId = theSvcVer.getService().getDomain().getDomainId();
			myServiceVersionPid = theSvcVer.getPid();
			myTransactionMillis = theHttpResponse != null ? theHttpResponse.getResponseTime() : 0;
			if (theInvocationResults == null || theInvocationResults.getPropertyCaptures() == null) {
				myPropertyCaptures = Collections.emptyMap();
			} else {
				myPropertyCaptures = theInvocationResults.getPropertyCaptures();
			}
			myThrottleTimeIfAny = theInvocationResults != null ? theInvocationResults.getThrottleTimeIfAny() : null;

			assert myAuditRecordType != null;
			assert myRequestTime != null;
			assert myHeaders != null;
			assert myRequestHostIp != null;
			assert StringUtils.isNotBlank(myDomainId);
			assert StringUtils.isNotBlank(myServiceId);
			assert StringUtils.isNotBlank(myServiceVersionId);
			assert myServiceVersionPid != null;
			assert myResponseType != null;
			assert myTransactionMillis != null;
			assert myResponseBody == null || myResponseHeaders != null;
		}

		public String getServiceId() {
			return myServiceId;
		}

		public String createLogFile() {
			return myAuditRecordType.createLogFileName(this);
		}

		public String formatEventDateForLogFilename() {
			return ourLogDateFormat.format(myRequestTime);
		}

		public Map<String, String> getPropertyCaptures() {
			return myPropertyCaptures;
		}

		public String getServiceVersionId() {
			return myServiceVersionId;
		}

		public Long getServiceVersionPid() {
			return myServiceVersionPid;
		}

		public String getUsername() {
			return myUsername;
		}

		public Long getUserPid() {
			return myUserPid;
		}

	}

}
