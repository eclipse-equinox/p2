/*******************************************************************************
 * Copyright (c) 2006-2009, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.io.*;
import java.net.URI;
import java.util.Date;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.events.*;
import org.eclipse.ecf.filetransfer.identity.*;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.osgi.util.NLS;

/**
 * @author Thomas Hallgren
 * @author henrik.lindberg@cloudsmith.com - adaption to 1.4 and to this p2 package
 */
public class FileReader extends FileTransferJob implements IFileTransferListener {
	private static IFileReaderProbe testProbe;
	private boolean closeStreamWhenFinished = false;
	private Exception exception;
	private FileInfo fileInfo;
	private long lastProgressCount;
	private long lastStatsCount;
	private IProgressMonitor theMonitor;
	private OutputStream theOutputStream;
	private ProgressStatistics statistics;
	private final int connectionRetryCount;
	private final long connectionRetryDelay;
	private final IConnectContext connectContext;
	private URI uri;

	/**
	 * Create a new FileReader that will retry failed connection attempts and sleep some amount of time between each
	 * attempt.
	 */
	public FileReader(IConnectContext aConnectContext) {
		super(Messages.FileTransport_reader); // job label

		// Hide this job.
		setSystem(true);
		setUser(false);
		connectionRetryCount = RepositoryPreferences.getConnectionRetryCount();
		connectionRetryDelay = RepositoryPreferences.getConnectionMsRetryDelay();
		connectContext = aConnectContext;
	}

	public FileInfo getLastFileInfo() {
		return fileInfo;
	}

	public synchronized void handleTransferEvent(IFileTransferEvent event) {
		if (event instanceof IIncomingFileTransferReceiveStartEvent) {
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

			if (theMonitor != null) {
				long fileLength = source.getFileLength();
				statistics = new ProgressStatistics(uri, source.getRemoteFileName(), fileLength);
				theMonitor.beginTask(null, 1000);
				theMonitor.subTask(statistics.report());
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
				statistics.increase(count);
				fileInfo.setAverageSpeed(statistics.getAverageSpeed());
				if (statistics.shouldReport()) {
					count = br - lastProgressCount;
					lastProgressCount = br;
					theMonitor.subTask(statistics.report());
					theMonitor.worked((int) (1000 * count / statistics.getTotal()));
				}
			}
			onData(source);
		} else if (event instanceof IIncomingFileTransferReceiveDoneEvent) {
			if (closeStreamWhenFinished)
				hardClose(theOutputStream);

			if (exception == null)
				exception = ((IIncomingFileTransferReceiveDoneEvent) event).getException();
			onDone(((IIncomingFileTransferReceiveDoneEvent) event).getSource());
		}
	}

	public InputStream read(URI url) throws CoreException, FileNotFoundException, AuthenticationFailedException {
		final PipedInputStream input = new PipedInputStream();
		PipedOutputStream output;
		try {
			output = new PipedOutputStream(input);
		} catch (IOException e) {
			throw RepositoryStatusHelper.wrap(e);
		}
		RepositoryTracing.debug("Downloading {0}", url); //$NON-NLS-1$

		final IProgressMonitor cancellationMonitor = new NullProgressMonitor();
		sendRetrieveRequest(url, output, null, true, cancellationMonitor);

		return new InputStream() {
			public int available() throws IOException {
				checkException();
				return input.available();
			}

			public void close() throws IOException {
				cancellationMonitor.setCanceled(true);
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
					e = new IOException(t.getMessage());
					e.initCause(t);
				}
				throw e;
			}
		};
	}

	//	/** Only request info
	//	 * @deprecated REMOVE THIS METHOD - SHOULD USE BROWSE INSTEAD TO ONLY GET HEAD - ALSO REMOVE PARAMTER ONLYHEAD
	//	 * @param uri
	//	 * @return FileInfo
	//	 * @throws CoreException
	//	 * @throws FileNotFoundException
	//	 * @throws AuthenticationFailedException
	//	 */
	//	public FileInfo readInfo(URI uri) throws CoreException, FileNotFoundException, AuthenticationFailedException {
	//		sendRetrieveRequest(uri, null, false, null);
	//		return getLastFileInfo();
	//	}
	public void readInto(URI uri, OutputStream anOutputStream, IProgressMonitor monitor) //
			throws CoreException, FileNotFoundException, AuthenticationFailedException {
		readInto(uri, anOutputStream, -1, monitor);
	}

	public void readInto(URI uri, OutputStream anOutputStream, long startPos, IProgressMonitor monitor) //
			throws CoreException, FileNotFoundException, AuthenticationFailedException {
		try {
			sendRetrieveRequest(uri, anOutputStream, (startPos != -1 ? new DownloadRange(startPos) : null), false, monitor);

			join();
		} catch (InterruptedException e) {
			monitor.setCanceled(true);
			throw new OperationCanceledException();
			//		} catch (Throwable t) {
			//			t.printStackTrace();
		} finally {
			if (monitor != null) {
				if (statistics == null)
					//
					// Monitor was never started. See to that it's balanced
					//
					monitor.beginTask(null, 1);
				else
					statistics = null;
				monitor.done();
			}
		}
	}

	protected void sendRetrieveRequest(URI uri, OutputStream outputStream, DownloadRange range, boolean closeStreamOnFinish, //
			IProgressMonitor monitor) throws CoreException, FileNotFoundException, AuthenticationFailedException {

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
		this.theOutputStream = outputStream;
		this.uri = uri;

		for (int retryCount = 0;;) {
			if (monitor != null && monitor.isCanceled())
				throw new OperationCanceledException();

			try {
				IFileID fileID = FileIDFactory.getDefault().createFileID(adapter.getRetrieveNamespace(), uri.toString());
				if (range != null)
					adapter.sendRetrieveRequest(fileID, range, this, null);
				else
					adapter.sendRetrieveRequest(fileID, this, null);
			} catch (IncomingFileTransferException e) {
				exception = e;
			} catch (FileCreateException e) {
				exception = e;
			} catch (Throwable t) {
				if (exception != null)
					exception.printStackTrace();
			}

			// note that 'exception' could have been captured in a callback
			if (exception != null) {
				// if this is a authentication failure - it is not meaningful to continue
				RepositoryStatusHelper.checkPermissionDenied(exception);

				Throwable t = RepositoryStatusHelper.unwind(exception);
				if (t instanceof CoreException)
					throw RepositoryStatusHelper.unwindCoreException((CoreException) t);

				if (t instanceof FileNotFoundException)
					//
					// Connection succeeded but the target doesn't exist
					//
					throw (FileNotFoundException) t;

				if (t instanceof IOException && retryCount < connectionRetryCount) {
					// TODO: Retry only certain exceptions or filter out
					// some exceptions not worth retrying
					//
					++retryCount;
					exception = null;
					try {
						LogHelper.log(new Status(IStatus.WARNING, Activator.ID, NLS.bind(Messages.connection_to_0_failed_on_1_retry_attempt_2, new String[] {uri.toString(), t.getMessage(), String.valueOf(retryCount)}), t));

						Thread.sleep(connectionRetryDelay);
						continue;
					} catch (InterruptedException e) {
						/* ignore */
					}
				}
				throw RepositoryStatusHelper.wrap(exception);
			}
			break;
		}
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

	public static void setTestProbe(IFileReaderProbe probe) {
		testProbe = probe;
	}

	public interface IFileReaderProbe {
		public void onStart(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor);

		public void onData(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor);

		public void onDone(FileReader reader, IIncomingFileTransfer source, IProgressMonitor monitor);
	}
}
