/*******************************************************************************
 * Copyright (c) 2006, 2016 Cloudsmith Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 * 	Cloudsmith Inc - initial API and implementation
 * 	IBM Corporation - ongoing development
 *  Sonatype Inc - ongoing development
 *  Ericsson AB. - Bug 407940 - [transport] Initial connection happens in current thread
 *  Red Hat Inc. - Bug 460967
 *  Rapicorp Inc - Bug 467286 - Set the ECF user agent property
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.transport.ecf;

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.events.*;
import org.eclipse.ecf.filetransfer.identity.*;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.*;
import org.eclipse.equinox.internal.p2.repository.Messages;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.FrameworkUtil;

/**
 * FileReader is an ECF FileTransferJob implementation.
 */
public final class FileReader extends FileTransferJob implements IFileTransferListener {
	/**
	 * Class used to suppress warnings about a job being blocked by another job.
	 * Since we are running a job that will always be blocked by another job that
	 * is actually performing the transfer, these messages are unnecessary and ugly.
	 */
	static class SuppressBlockedMonitor extends SubProgressMonitor {
		public SuppressBlockedMonitor(IProgressMonitor monitor, int ticks) {
			super(monitor, ticks);
		}

		public void setBlocked(IStatus reason) {
			//do nothing
		}

		public void clearBlocked() {
			//do nothing
		}
	}

	static Map<String, Map<String, String>> options;

	static private String getProperty(String key, String defaultValue) {
		String value = Activator.getContext().getProperty(key);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	static {
		Map<String, String> extraRequestHeaders = new HashMap<String, String>(1);
		String userAgent = null;
		String javaSpec = getProperty("java.runtime.version", "unknownJava"); //$NON-NLS-1$//$NON-NLS-2$
		String javaVendor = getProperty("java.vendor", "unknownJavaVendor");//$NON-NLS-1$//$NON-NLS-2$
		String osName = getProperty("org.osgi.framework.os.name", "unknownOS"); //$NON-NLS-1$ //$NON-NLS-2$
		String osgiArch = getProperty("org.osgi.framework.processor", "unknownArch");//$NON-NLS-1$//$NON-NLS-2$
		String language = getProperty("osgi.nl", "unknownLanguage");//$NON-NLS-1$//$NON-NLS-2$
		String osVersion = getProperty("org.osgi.framework.os.version", "unknownOSVersion"); //$NON-NLS-1$ //$NON-NLS-2$
		String p2Version = FrameworkUtil.getBundle(FileReader.class).getVersion().toString();
		userAgent = "p2/" + p2Version + " (Java " + javaSpec + ' ' + javaVendor + "; " + osName + ' ' + osVersion + ' ' + osgiArch + "; " + language + ") "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		String userAgentProvided = getProperty("p2.userAgent", null); //$NON-NLS-1$
		if (userAgentProvided == null) {
			String productId = getProperty("eclipse.product", "unknownProduct"); //$NON-NLS-1$ //$NON-NLS-2$
			String appId = getProperty("eclipse.application", "unknownApp"); //$NON-NLS-1$ //$NON-NLS-2$
			String buildId = getProperty("eclipse.buildId", "unknownBuildId"); //$NON-NLS-1$ //$NON-NLS-2$
			userAgent += productId + '/' + buildId + " (" + appId + ')'; //$NON-NLS-1$
		} else {
			userAgent += userAgentProvided;
		}
		extraRequestHeaders.put("User-Agent", userAgent); //$NON-NLS-1$
		options = new HashMap<String, Map<String, String>>(1);
		options.put(org.eclipse.ecf.filetransfer.IRetrieveFileTransferOptions.REQUEST_HEADERS, extraRequestHeaders);
	}

	private static IFileReaderProbe testProbe;
	private boolean closeStreamWhenFinished = false;
	private Exception exception;
	private FileInfo fileInfo;
	private long lastProgressCount;
	private long lastStatsCount;
	protected IProgressMonitor theMonitor;
	private OutputStream theOutputStream;
	private ProgressStatistics statistics;
	private final int connectionRetryCount;
	private final long connectionRetryDelay;
	private final IConnectContext connectContext;
	private URI requestUri;
	protected IFileTransferConnectStartEvent connectEvent;
	private Job cancelJob;
	private boolean monitorStarted;
	private IProvisioningAgent agent;
	private boolean isPause = false;
	private boolean hasPaused = false;
	private IFileTransferPausable pasuable = null;

	/**
	 * Create a new FileReader that will retry failed connection attempts and sleep some amount of time between each
	 * attempt.
	 */
	public FileReader(IProvisioningAgent aAgent, IConnectContext aConnectContext) {
		super(Messages.FileTransport_reader); // job label

		// Hide this job.
		setSystem(true);
		setUser(false);
		connectionRetryCount = RepositoryPreferences.getConnectionRetryCount();
		connectionRetryDelay = RepositoryPreferences.getConnectionMsRetryDelay();
		connectContext = aConnectContext;
		this.agent = aAgent;
	}

	public FileInfo getLastFileInfo() {
		return fileInfo;
	}

	/**
	 * A job to handle cancelation when trying to establish a socket connection.
	 * At this point we don't have a transfer job running yet, so we need a separate
	 * job to monitor for cancelation.
	 */
	protected class CancelHandler extends Job {
		private boolean done = false;

		protected CancelHandler() {
			super(Messages.FileTransport_cancelCheck);
			setSystem(true);
		}

		public IStatus run(IProgressMonitor jobMonitor) {
			while (!done && !jobMonitor.isCanceled()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				}
				if (theMonitor != null && theMonitor.isCanceled())
					if (connectEvent != null)
						connectEvent.cancel();
			}
			return Status.OK_STATUS;
		}

		protected void canceling() {
			//wake up from sleep in run method
			Thread t = getThread();
			if (t != null)
				t.interrupt();
		}

	}

	public synchronized void handleTransferEvent(IFileTransferEvent event) {
		if (event instanceof IFileTransferConnectStartEvent) {
			// keep the connect event to be able to cancel the transfer
			connectEvent = (IFileTransferConnectStartEvent) event;
			connectEvent.connectUsingJob(((IFileTransferConnectStartEvent) event).prepareConnectJob(null));
			cancelJob = new CancelHandler();
			//schedule with a delay to avoid the overhead of an extra job on a fast connection
			cancelJob.schedule(500);
		} else if (event instanceof IIncomingFileTransferReceiveStartEvent) {
			//we no longer need the cancel handler because we are about to fork the transfer job
			if (cancelJob != null)
				cancelJob.cancel();
			IIncomingFileTransfer source = ((IIncomingFileTransferEvent) event).getSource();
			try {
				FileInfo fi = new FileInfo();
				Date lastModified = source.getRemoteLastModified();
				if (lastModified != null)
					fi.setLastModified(lastModified.getTime());
				fi.setName(source.getRemoteFileName());
				fi.setSize(source.getFileLength());
				fileInfo = fi;

				((IIncomingFileTransferReceiveStartEvent) event).receive(theOutputStream, this);
			} catch (IOException e) {
				exception = e;
				return;
			}
			long fileLength = source.getFileLength();
			ProgressStatistics stats = new ProgressStatistics(agent, requestUri, source.getRemoteFileName(), fileLength);
			setStatistics(stats);

			if (theMonitor != null) {
				theMonitor.beginTask(null, 1000);
				monitorStarted = true;
				theMonitor.subTask(stats.report());
				lastStatsCount = 0;
				lastProgressCount = 0;
			}
			onStart(source);
		} else if (event instanceof IIncomingFileTransferReceiveDataEvent) {
			IIncomingFileTransfer source = ((IIncomingFileTransferEvent) event).getSource();
			if (theMonitor != null) {
				if (theMonitor.isCanceled()) {
					source.cancel();
					return;
				}

				long br = source.getBytesReceived();
				long count = br - lastStatsCount;
				lastStatsCount = br;
				ProgressStatistics stats = getStatistics();
				if (stats != null) {
					stats.increase(count);
					fileInfo.setAverageSpeed(stats.getAverageSpeed());
					if (stats.shouldReport()) {
						count = br - lastProgressCount;
						lastProgressCount = br;
						theMonitor.subTask(stats.report());
						theMonitor.worked((int) (1000 * count / stats.getTotal()));
					}
				}
			}
			pauseIfPossible(source);
			onData(source);
		} else if (event instanceof IIncomingFileTransferReceiveDoneEvent) {
			// stop paused Reader if resuming failed
			this.hasPaused = false;
			if (closeStreamWhenFinished)
				hardClose(theOutputStream);

			if (exception == null)
				exception = ((IIncomingFileTransferReceiveDoneEvent) event).getException();
			onDone(((IIncomingFileTransferReceiveDoneEvent) event).getSource());
		} else if (event instanceof IIncomingFileTransferReceivePausedEvent) {
			this.hasPaused = true;
		} else if (event instanceof IIncomingFileTransferReceiveResumedEvent) {
			//we no longer need the cancel handler because we are about to resume the transfer job
			if (cancelJob != null)
				cancelJob.cancel();
			try {
				((IIncomingFileTransferReceiveResumedEvent) event).receive(theOutputStream, this);
			} catch (IOException e) {
				exception = e;
			} finally {
				this.hasPaused = false;
			}
		}
	}

	private synchronized void pauseIfPossible(IIncomingFileTransfer source) {
		if (isPaused() && !hasPaused) {
			pasuable = source.getAdapter(IFileTransferPausable.class);
			if (pasuable != null)
				pasuable.pause();
		}
	}

	public InputStream read(URI url, final IProgressMonitor monitor) throws CoreException, FileNotFoundException, AuthenticationFailedException, JREHttpClientRequiredException {
		final PipedInputStream input = new PipedInputStream();
		PipedOutputStream output;
		try {
			output = new PipedOutputStream(input);
		} catch (IOException e) {
			throw RepositoryStatusHelper.wrap(e);
		}
		RepositoryTracing.debug("Downloading {0}", url); //$NON-NLS-1$

		sendRetrieveRequest(url, output, null, true, monitor);

		return new InputStream() {
			public int available() throws IOException {
				checkException();
				return input.available();
			}

			public void close() throws IOException {
				hardClose(input);
				checkException();
			}

			public void mark(int readlimit) {
				input.mark(readlimit);
			}

			public boolean markSupported() {
				return input.markSupported();
			}

			public int read() throws IOException {
				checkException();
				return input.read();
			}

			public int read(byte b[]) throws IOException {
				checkException();
				return input.read(b);
			}

			public int read(byte b[], int off, int len) throws IOException {
				checkException();
				return input.read(b, off, len);
			}

			public void reset() throws IOException {
				checkException();
				input.reset();
			}

			public long skip(long n) throws IOException {
				checkException();
				return input.skip(n);
			}

			private void checkException() throws IOException {
				if (getException() == null)
					return;

				IOException e;
				Throwable t = RepositoryStatusHelper.unwind(getException());
				if (t instanceof IOException)
					e = (IOException) t;
				else {
					if (t instanceof UserCancelledException) {
						Throwable cause = t;
						t = new OperationCanceledException(t.getMessage());
						t.initCause(cause);
					}
					e = new IOException(t.getMessage());
					e.initCause(t);
				}
				throw e;
			}
		};
	}

	public void readInto(URI uri, OutputStream anOutputStream, IProgressMonitor monitor) //
			throws CoreException, FileNotFoundException, AuthenticationFailedException, JREHttpClientRequiredException {
		readInto(uri, anOutputStream, -1, monitor);
	}

	public boolean belongsTo(Object family) {
		return family == this;
	}

	public void readInto(URI uri, OutputStream anOutputStream, long startPos, IProgressMonitor monitor) //
			throws CoreException, FileNotFoundException, AuthenticationFailedException, JREHttpClientRequiredException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		try {
			sendRetrieveRequest(uri, anOutputStream, (startPos != -1 ? new DownloadRange(startPos) : null), false, monitor);
			Job.getJobManager().join(this, new SuppressBlockedMonitor(monitor, 0));
			waitPaused(uri, anOutputStream, startPos, monitor);
			if (monitor.isCanceled() && connectEvent != null)
				connectEvent.cancel();
			// check and throw exception if received in callback
			checkException(uri, connectionRetryCount);
		} catch (InterruptedException e) {
			monitor.setCanceled(true);
			throw new OperationCanceledException();
		} finally {
			// kill the cancelJob, if there is one
			if (cancelJob != null) {
				cancelJob.cancel();
				cancelJob = null;
			}
			// If monitor was never started, make sure it is balanced
			if (!monitorStarted)
				monitor.beginTask(null, 1);
			monitorStarted = false;
			monitor.done();
		}
	}

	protected void waitPaused(URI uri, OutputStream anOutputStream, long startPos, IProgressMonitor monitor) throws AuthenticationFailedException, JREHttpClientRequiredException, FileNotFoundException, CoreException, OperationCanceledException, InterruptedException {
		if (hasPaused) {
			while (hasPaused) {
				Thread.sleep(1000);
				if (monitor.isCanceled())
					throw new OperationCanceledException();
			}
			Job.getJobManager().join(this, new SuppressBlockedMonitor(monitor, 0));
			waitPaused(uri, anOutputStream, startPos, monitor);
		}
	}

	protected void sendRetrieveRequest(URI uri, OutputStream outputStream, DownloadRange range, boolean closeStreamOnFinish, //
			IProgressMonitor monitor) throws CoreException, FileNotFoundException, AuthenticationFailedException, JREHttpClientRequiredException {

		IRetrieveFileTransferFactory factory = Activator.getDefault().getRetrieveFileTransferFactory();
		if (factory == null) {
			throw RepositoryStatusHelper.fromMessage(Messages.ecf_configuration_error);
		}

		IRetrieveFileTransferContainerAdapter adapter = factory.newInstance();
		adapter.setConnectContextForAuthentication(connectContext);

		this.exception = null;
		this.closeStreamWhenFinished = closeStreamOnFinish;
		this.fileInfo = null;
		this.statistics = null;
		this.lastProgressCount = 0L;
		this.lastStatsCount = 0L;
		this.theMonitor = monitor;
		this.monitorStarted = false;
		this.theOutputStream = outputStream;
		this.requestUri = uri;

		for (int retryCount = 0;; retryCount++) {
			if (monitor != null && monitor.isCanceled())
				throw new OperationCanceledException();

			try {
				IFileID fileID = FileIDFactory.getDefault().createFileID(adapter.getRetrieveNamespace(), uri.toString());
				adapter.sendRetrieveRequest(fileID, range, this, options);
			} catch (IncomingFileTransferException e) {
				exception = e;
			} catch (FileCreateException e) {
				exception = e;
			} catch (Throwable t) {
				if (exception != null)
					exception.printStackTrace();
			}
			if (checkException(uri, retryCount))
				break;
		}
	}

	public synchronized boolean pause() {
		this.isPause = true;
		return true;
	}

	public boolean isPaused() {
		return this.isPause;
	}

	public synchronized boolean resume() {
		this.isPause = false;
		if (this.pasuable != null) {
			return this.pasuable.resume();
		}
		return false;
	}

	/**
	 * Utility method to check exception condition and determine if retry should be done.
	 * If there was an exception it is translated into one of the specified exceptions and thrown.
	 * 
	 * @param uri the URI being read - used for logging purposes
	 * @param attemptCounter - the current attempt number (start with 0)
	 * @return true if the exception is an IOException and attemptCounter < connectionRetryCount, false otherwise
	 * @throws CoreException
	 * @throws FileNotFoundException
	 * @throws AuthenticationFailedException
	 */
	private boolean checkException(URI uri, int attemptCounter) throws CoreException, FileNotFoundException, AuthenticationFailedException, JREHttpClientRequiredException {
		// note that 'exception' could have been captured in a callback
		if (exception != null) {
			// check if HTTP client needs to be changed
			RepositoryStatusHelper.checkJREHttpClientRequired(exception);

			// if this is an 'authentication failure' - it is not meaningful to continue
			RepositoryStatusHelper.checkPermissionDenied(exception);

			// if this is a 'file not found' - it is not meaningful to continue
			RepositoryStatusHelper.checkFileNotFound(exception, uri);

			Throwable t = RepositoryStatusHelper.unwind(exception);
			if (t instanceof CoreException)
				throw RepositoryStatusHelper.unwindCoreException((CoreException) t);

			// not meaningful to try 'timeout again' - if a server is that busy, we
			// need to wait for quite some time before retrying- it is not likely it is
			// just a temporary network thing.
			if (t instanceof SocketTimeoutException)
				throw RepositoryStatusHelper.wrap(t);

			if (t instanceof IOException && attemptCounter < connectionRetryCount) {
				// TODO: Retry only certain exceptions or filter out
				// some exceptions not worth retrying
				//
				exception = null;
				try {
					LogHelper.log(new Status(IStatus.WARNING, Activator.ID, NLS.bind(Messages.connection_to_0_failed_on_1_retry_attempt_2, new String[] {uri.toString(), t.getMessage(), String.valueOf(attemptCounter)}), t));

					Thread.sleep(connectionRetryDelay);
					return false;
				} catch (InterruptedException e) {
					/* ignore */
				}
			}
			throw RepositoryStatusHelper.wrap(exception);
		}
		return true;
	}

	protected Exception getException() {
		return exception;
	}

	/**
	 * Closes input and output streams
	 * @param aStream
	 */
	public static void hardClose(Object aStream) {
		if (aStream != null) {
			try {
				if (aStream instanceof OutputStream)
					((OutputStream) aStream).close();
				else if (aStream instanceof InputStream)
					((InputStream) aStream).close();
			} catch (IOException e) { /* ignore */
			}
		}
	}

	private static class DownloadRange implements IFileRangeSpecification {

		private long startPosition;

		public DownloadRange(long startPos) {
			startPosition = startPos;
		}

		public long getEndPosition() {
			return -1;
		}

		public long getStartPosition() {
			return startPosition;
		}

	}

	private void onDone(IIncomingFileTransfer source) {
		if (testProbe != null)
			testProbe.onDone(this, source, theMonitor);
	}

	private void onStart(IIncomingFileTransfer source) {
		if (testProbe != null)
			testProbe.onStart(this, source, theMonitor);
	}

	private void onData(IIncomingFileTransfer source) {
		if (testProbe != null)
			testProbe.onData(this, source, theMonitor);
	}

	/**
	 * Sets a testing probe that can intercept events on the file reader for testing purposes.
	 * This method should only ever be called from automated test suites.
	 */
	public static void setTestProbe(IFileReaderProbe probe) {
		testProbe = probe;
	}

	/**
	 * Sets the progress statistics. This method is synchronized because the field
	 * is accessed from both the transfer thread and the thread initiating the transfer
	 * and we need to ensure field values are consistent across threads.
	 * 
	 * @param statistics the statistics to set, or <code>null</code>
	 */
	private synchronized void setStatistics(ProgressStatistics statistics) {
		this.statistics = statistics;
	}

	/**
	 * Returns the progress statistics. This method is synchronized because the field
	 * is accessed from both the transfer thread and the thread initiating the transfer
	 * and we need to ensure field values are consistent across threads.
	 * 
	 * @return the statistics, or <code>null</code>
	 */
	private synchronized ProgressStatistics getStatistics() {
		return statistics;
	}

	/**
	 * An interface to allow automated tests to hook into file reader events
	 * @see #setTestProbe
	 */
	public interface IFileReaderProbe {
		public void onStart(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor);

		public void onData(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor);

		public void onDone(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor);
	}
}
