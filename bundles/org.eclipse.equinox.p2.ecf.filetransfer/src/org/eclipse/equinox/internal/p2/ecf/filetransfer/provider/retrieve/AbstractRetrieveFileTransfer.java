/****************************************************************************
 * Copyright (c) 2004, 2010 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *    Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 220258
 *    Henrich Kraemer - bug 295030, Update Manager doesn't work with SOCKS proxy  
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.provider.filetransfer.retrieve;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.FileTransferJob;
import org.eclipse.ecf.filetransfer.IFileRangeSpecification;
import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.IFileTransferPausable;
import org.eclipse.ecf.filetransfer.IFileTransferRunnable;
import org.eclipse.ecf.filetransfer.IIncomingFileTransfer;
import org.eclipse.ecf.filetransfer.IRetrieveFileTransferOptions;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.ecf.filetransfer.UserCancelledException;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveDataEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveDoneEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceivePausedEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveResumedEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveStartEvent;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.ecf.internal.provider.filetransfer.Activator;
import org.eclipse.ecf.internal.provider.filetransfer.Messages;
import org.eclipse.ecf.provider.filetransfer.identity.FileTransferNamespace;
import org.eclipse.ecf.provider.filetransfer.util.PollingInputStream;
import org.eclipse.ecf.provider.filetransfer.util.ProxySetupHelper;
import org.eclipse.ecf.provider.filetransfer.util.TimeoutInputStream;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractRetrieveFileTransfer implements IIncomingFileTransfer, IRetrieveFileTransfer, IFileTransferPausable {

	public static final int DEFAULT_BUF_LENGTH = 4096;

	protected static final int POLLING_RETRY_ATTEMPTS = Integer.parseInt(System.getProperty("org.eclipse.ecf.provider.filetransfer.retrieve.retryAttempts", "30")); //$NON-NLS-1$ //$NON-NLS-2$;;

	protected static final int TIMEOUT_INPUTSTREAM_BUFFER_SIZE = 8192;

	protected static final int READ_TIMEOUT = Integer.parseInt(System.getProperty("org.eclipse.ecf.provider.filetransfer.retrieve.readTimeout", "1000")); //$NON-NLS-1$ //$NON-NLS-2$;

	protected static final int CLOSE_TIMEOUT = Integer.parseInt(System.getProperty("org.eclipse.ecf.provider.filetransfer.retrieve.closeTimeout", "1000")); //$NON-NLS-1$ //$NON-NLS-2$;

	private static final String readTimeoutMessage = "Timeout while reading input stream.\n" + //$NON-NLS-1$
			"The following system properties can be used to adjust the readTimeout, retryAttempts, and closeTimeout\n" + //$NON-NLS-1$
			"\torg.eclipse.ecf.provider.filetransfer.retrieve.readTimeout=<default:1000>\n" + //$NON-NLS-1$
			"\torg.eclipse.ecf.provider.filetransfer.retrieve.retryAttempts=<default:30>\n" + //$NON-NLS-1$
			"\torg.eclipse.ecf.provider.filetransfer.retrieve.closeTimeout=<default:1000>\n"; //$NON-NLS-1$

	private static final String closeTimeoutMessage = "Timeout while closing input stream.\n" + //$NON-NLS-1$
			"The following system properties can be used to adjust the readTimeout, retryAttempts, and closeTimeout\n" + //$NON-NLS-1$
			"\torg.eclipse.ecf.provider.filetransfer.retrieve.readTimeout=<default:1000>\n" + //$NON-NLS-1$
			"\torg.eclipse.ecf.provider.filetransfer.retrieve.retryAttempts=<default:30>\n" + //$NON-NLS-1$
			"\torg.eclipse.ecf.provider.filetransfer.retrieve.closeTimeout=<default:1000>\n"; //$NON-NLS-1$

	protected Object jobLock = new Object();
	protected Job job;

	protected URL remoteFileURL;

	protected IFileID remoteFileID;

	protected IFileTransferListener listener;

	protected int buff_length = DEFAULT_BUF_LENGTH;

	protected boolean done = false;

	protected volatile long bytesReceived = 0;

	protected InputStream remoteFileContents;

	protected OutputStream localFileContents;

	protected boolean closeOutputStream = true;

	protected Exception exception;

	protected long fileLength = -1;

	protected long lastModifiedTime = 0L;

	protected Map options = null;

	protected boolean paused = false;

	protected IFileRangeSpecification rangeSpecification = null;

	protected Proxy proxy;

	protected IConnectContext connectContext;

	protected long transferStartTime;

	protected double downloadRateBytesPerSecond = 0L;

	/**
	 * @since 3.1
	 */
	protected Map responseHeaders;

	public AbstractRetrieveFileTransfer() {
		//
	}

	protected InputStream wrapTransferReadInputStream(InputStream inputStream, IProgressMonitor monitor) {
		return new PollingInputStream(inputStream, getRetryAttempts(), monitor, readTimeoutMessage, closeTimeoutMessage);
	}

	private int getRetryAttempts() {
		int result = POLLING_RETRY_ATTEMPTS;
		Map localOptions = getOptions();
		if (localOptions != null) {
			// See if the property is present, if so set
			Object o = localOptions.get("org.eclipse.ecf.provider.filetransfer.retrieve.retryAttempts"); //$NON-NLS-1$
			if (o != null) {
				if (o instanceof Integer) {
					result = ((Integer) o).intValue();
				} else if (o instanceof String) {
					result = Integer.parseInt(((String) o));
				}
			}
		}
		return result;
	}

	private IFileTransferRunnable fileTransferRunnable = new IFileTransferRunnable() {
		public IStatus performFileTransfer(IProgressMonitor monitor) {
			transferStartTime = System.currentTimeMillis();
			final byte[] buf = new byte[buff_length];
			final long totalWork = ((fileLength == -1) ? 100 : fileLength);
			double factor = (totalWork > Integer.MAX_VALUE) ? (((double) Integer.MAX_VALUE) / ((double) totalWork)) : 1.0;
			int work = (totalWork > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) totalWork;
			monitor.beginTask(getRemoteFileURL().toString() + Messages.AbstractRetrieveFileTransfer_Progress_Data, work);
			InputStream readInputStream = null;
			try {
				// We will test for remoteFileContents is null...if it is null then we can't continue.
				// See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=425868
				if (remoteFileContents == null)
					throw new IOException("input stream cannot be null"); //$NON-NLS-1$
				// Create read input stream
				readInputStream = wrapTransferReadInputStream(remoteFileContents, monitor);
				while (!isDone() && !isPaused()) {
					try {
						final int bytes = readInputStream.read(buf);
						handleReceivedData(buf, bytes, factor, monitor);
					} catch (OperationCanceledException e) {
						throw new UserCancelledException(Messages.AbstractRetrieveFileTransfer_Exception_User_Cancelled);
					}
				}
			} catch (final Exception e) {
				if (!isDone()) {
					setDoneException(e);
				}
			} finally {
				try {
					if (readInputStream != null)
						readInputStream.close();
				} catch (final IOException e) {
					Activator a = Activator.getDefault();
					if (a != null)
						a.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, "hardClose", e)); //$NON-NLS-1$
				}
				hardClose();
				monitor.done();
				try {
					if (isPaused())
						fireTransferReceivePausedEvent();
					else
						fireTransferReceiveDoneEvent();
				} catch (Exception e) {
					// simply log
					Activator a = Activator.getDefault();
					if (a != null)
						a.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, Messages.AbstractRetrieveFileTransfer_EXCEPTION_IN_FINALLY, e));
				}
			}
			return getFinalStatus(exception);
		}

	};

	protected URL getRemoteFileURL() {
		return remoteFileURL;
	}

	protected int getSocketReadTimeout() {
		int result = READ_TIMEOUT;
		Map localOptions = getOptions();
		if (localOptions != null) {
			// See if the connect timeout option is present, if so set
			Object o = localOptions.get(IRetrieveFileTransferOptions.READ_TIMEOUT);
			if (o != null) {
				if (o instanceof Integer) {
					result = ((Integer) o).intValue();
				} else if (o instanceof String) {
					result = Integer.parseInt(((String) o));
				}
				return result;
			}
			o = localOptions.get("org.eclipse.ecf.provider.filetransfer.httpclient.retrieve.readTimeout"); //$NON-NLS-1$
			if (o != null) {
				if (o instanceof Integer) {
					result = ((Integer) o).intValue();
				} else if (o instanceof String) {
					result = Integer.parseInt(((String) o));
				}
			}
		}
		return result;
	}

	protected int getSocketCloseTimeout() {
		int result = CLOSE_TIMEOUT;
		Map localOptions = getOptions();
		if (localOptions != null) {
			// See if the property is present, if so set
			Object o = localOptions.get("org.eclipse.ecf.provider.filetransfer.retrieve.closeTimeout"); //$NON-NLS-1$
			if (o != null) {
				if (o instanceof Integer) {
					result = ((Integer) o).intValue();
				} else if (o instanceof String) {
					result = Integer.parseInt(((String) o));
				}
			}
		}
		return result;
	}

	protected void setInputStream(InputStream ins) {
		remoteFileContents = new TimeoutInputStream(ins, TIMEOUT_INPUTSTREAM_BUFFER_SIZE, getSocketReadTimeout(), getSocketCloseTimeout());
	}

	protected void setOutputStream(OutputStream outs) {
		localFileContents = outs;
	}

	protected void setCloseOutputStream(boolean close) {
		closeOutputStream = close;
	}

	protected void setFileLength(long length) {
		fileLength = length;
	}

	protected void setLastModifiedTime(long timestamp) {
		lastModifiedTime = timestamp;
	}

	protected Map getOptions() {
		return options;
	}

	protected synchronized void handleReceivedData(byte[] buf, int bytes, double factor, IProgressMonitor monitor) throws IOException {
		if (bytes != -1) {
			bytesReceived += bytes;
			localFileContents.write(buf, 0, bytes);
			downloadRateBytesPerSecond = (bytesReceived / ((System.currentTimeMillis() + 1 - transferStartTime) / 1000.0));
			monitor.setTaskName(createJobName() + Messages.AbstractRetrieveFileTransfer_Progress_Data + NLS.bind(Messages.AbstractRetrieveFileTransfer_InfoTransferRate, toHumanReadableBytes(downloadRateBytesPerSecond)));
			monitor.worked((int) Math.round(factor * bytes));
			fireTransferReceiveDataEvent();
		} else
			setDone(true);
	}

	public static String toHumanReadableBytes(double size) {
		double convertedSize;
		String unit;

		if (size / (1024 * 1024 * 1024) >= 1) {
			convertedSize = size / (1024 * 1024 * 1024);
			unit = Messages.AbstractRetrieveFileTransfer_SizeUnitGB;
		} else if (size / (1024 * 1024) >= 1) {
			convertedSize = size / (1024 * 1024);
			unit = Messages.AbstractRetrieveFileTransfer_SizeUnitMB;
		} else if (size / 1024 >= 1) {
			convertedSize = size / 1024;
			unit = Messages.AbstractRetrieveFileTransfer_SizeUnitKB;
		} else {
			convertedSize = size;
			unit = Messages.AbstractRetrieveFileTransfer_SizeUnitBytes;
		}

		DecimalFormat df = new DecimalFormat(NLS.bind(Messages.AbstractRetrieveFileTransfer_TransferRateFormat, unit));
		return df.format(convertedSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.identity.IIdentifiable#getID()
	 */
	public ID getID() {
		return remoteFileID;
	}

	protected IStatus getFinalStatus(Throwable exception1) {
		return Status.OK_STATUS;
	}

	protected void hardClose() {
		try {
			if (remoteFileContents != null)
				remoteFileContents.close();
		} catch (final IOException e) {
			Activator.getDefault().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, IStatus.WARNING, String.format("hardClose url=%s", remoteFileURL), e)); //$NON-NLS-1$
		}
		try {
			if (localFileContents != null && closeOutputStream)
				localFileContents.close();
		} catch (final IOException e) {
			Activator.getDefault().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, String.format("hardClose url=%s", remoteFileURL), e)); //$NON-NLS-1$
		}
		// leave job intact to ensure only one done event is fired
		remoteFileContents = null;
		localFileContents = null;
	}

	protected void fireTransferReceivePausedEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceivePausedEvent() {

			public IIncomingFileTransfer getSource() {
				return AbstractRetrieveFileTransfer.this;
			}

			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceivePausedEvent["); //$NON-NLS-1$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append(";fileLength=").append(fileLength).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
				return sb.toString();
			}
		});
	}

	protected void fireTransferReceiveDoneEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceiveDoneEvent() {

			public IIncomingFileTransfer getSource() {
				return AbstractRetrieveFileTransfer.this;
			}

			public Exception getException() {
				return AbstractRetrieveFileTransfer.this.getException();
			}

			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceiveDoneEvent["); //$NON-NLS-1$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append(";fileLength=").append(fileLength).append(";exception=").append(getException()) //$NON-NLS-1$ //$NON-NLS-2$
						.append("]"); //$NON-NLS-1$
				return sb.toString();
			}
		});
	}

	protected void fireTransferReceiveDataEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceiveDataEvent() {
			public IIncomingFileTransfer getSource() {
				return AbstractRetrieveFileTransfer.this;
			}

			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceiveDataEvent["); //$NON-NLS-1$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append(";fileLength=").append(fileLength) //$NON-NLS-1$ 
						.append("]"); //$NON-NLS-1$
				return sb.toString();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#
	 * setConnectContextForAuthentication
	 * (org.eclipse.ecf.core.security.IConnectContext)
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext) {
		this.connectContext = connectContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#setProxy
	 * (org.eclipse.ecf.core.util.Proxy)
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.filetransfer.IIncomingFileTransfer#getBytesReceived()
	 */
	public long getBytesReceived() {
		return bytesReceived;
	}

	/**
	 * @return UserCancelledException if some user cancellation
	 * @since 3.0
	 */
	protected UserCancelledException newUserCancelledException() {
		return new UserCancelledException(Messages.AbstractRetrieveFileTransfer_Exception_User_Cancelled);
	}

	protected synchronized void resetDoneAndException() {
		setDone(false);
		this.exception = null;
	}

	protected synchronized void setDone(boolean done) {
		this.done = done;
	}

	protected synchronized void setDoneException(Exception e) {
		this.done = true;
		this.exception = e;
	}

	protected synchronized boolean isCanceled() {
		return done && exception instanceof UserCancelledException;
	}

	protected void setDoneCanceled() {
		setDoneCanceled(newUserCancelledException());
	}

	protected synchronized void setDoneCanceled(Exception e) {
		this.done = true;
		if (e instanceof UserCancelledException) {
			exception = e;
		} else {
			exception = newUserCancelledException();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransfer#cancel()
	 */
	public void cancel() {
		if (isPaused()) {
			setDoneCanceled();
			fireTransferReceiveDoneEvent();
			return;
		}
		synchronized (jobLock) {
			if (job != null)
				job.cancel();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransfer#getException()
	 */
	public synchronized Exception getException() {
		return exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransfer#getPercentComplete()
	 */
	public double getPercentComplete() {
		if (fileLength == -1 || fileLength == 0)
			return fileLength;
		return ((double) bytesReceived / (double) fileLength);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransfer#getFileLength()
	 */
	public long getFileLength() {
		return fileLength;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer
	 * #getRemoteLastModified()
	 */
	public Date getRemoteLastModified() {
		return lastModifiedTime == 0L ? null : new Date(lastModifiedTime);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransfer#isDone()
	 */
	public synchronized boolean isDone() {
		return done;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == null)
			return null;
		if (adapter.isInstance(this)) {
			return adapter.cast(this);
		}
		final IAdapterManager adapterManager = Activator.getDefault().getAdapterManager();
		return (T) ((adapterManager == null) ? null : adapterManager.loadAdapter(this, adapter.getName()));
	}

	/**
	 * Open incoming and outgoing streams associated with this file transfer.
	 * Subclasses must implement this method to open input and output streams.
	 * The <code>remoteFileContents</code> and <code>localFileContent</code>
	 * must be non-<code>null</code> after successful completion of the
	 * implementation of this method.
	 * 
	 * @throws IncomingFileTransferException if some problem
	 */
	protected abstract void openStreams() throws IncomingFileTransferException;

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#
	 * sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID,
	 * org.eclipse.ecf.filetransfer.IFileTransferListener, java.util.Map)
	 */
	public void sendRetrieveRequest(final IFileID remoteFileID1, IFileTransferListener transferListener, Map options1) throws IncomingFileTransferException {
		sendRetrieveRequest(remoteFileID1, null, transferListener, options1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#
	 * getRetrieveNamespace()
	 */
	public Namespace getRetrieveNamespace() {
		return IDFactory.getDefault().getNamespaceByName(FileTransferNamespace.PROTOCOL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransferPausable#isPaused()
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Subclass overridable version of {@link #pause()}. Subclasses must provide
	 * an implementation of this method to support {@link IFileTransferPausable}
	 * .
	 * 
	 * @return true if the pause is successful. <code>false</code> otherwise.
	 */
	protected abstract boolean doPause();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransferPausable#pause()
	 */
	public boolean pause() {
		return doPause();
	}

	/**
	 * Subclass overridable version of {@link #resume()}. Subclasses must
	 * provide an implementation of this method to support
	 * {@link IFileTransferPausable}.
	 * 
	 * @return true if the resume is successful. <code>false</code> otherwise.
	 */
	protected abstract boolean doResume();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IFileTransferPausable#resume()
	 */
	public boolean resume() {
		return doResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IIncomingFileTransfer#getListener()
	 */
	public IFileTransferListener getListener() {
		return listener;
	}

	protected String createRangeName() {
		if (rangeSpecification == null)
			return ""; //$NON-NLS-1$
		return "[" + rangeSpecification.getStartPosition() + "," + rangeSpecification.getEndPosition() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	protected String createJobName() {
		return getRemoteFileURL().toString() + createRangeName();
	}

	protected void setupAndScheduleJob(FileTransferJob fileTransferJob) {
		if (fileTransferJob == null) {
			// Create our own
			fileTransferJob = new FileTransferJob(createJobName());
		}
		// Now set to our runnable
		fileTransferJob.setFileTransferRunnable(fileTransferRunnable);
		fileTransferJob.setFileTransfer(this);
		if (isDone()) {
			return;
		}
		synchronized (jobLock) {
			job = fileTransferJob;
			job.schedule();
		}
	}

	protected void fireReceiveStartEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceiveStartEvent() {
			/*
			 * (non-Javadoc)
			 * 
			 * @seeorg.eclipse.ecf.filetransfer.events.
			 * IIncomingFileTransferEvent#getFileID()
			 */
			public IIncomingFileTransfer getSource() {
				return AbstractRetrieveFileTransfer.this;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @seeorg.eclipse.ecf.filetransfer.events.
			 * IIncomingFileTransferReceiveStartEvent#getFileID()
			 */
			public IFileID getFileID() {
				return remoteFileID;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @seeorg.eclipse.ecf.filetransfer.events.
			 * IIncomingFileTransferReceiveStartEvent
			 * #receive(java.io.File)
			 */
			public IIncomingFileTransfer receive(File localFileToSave) throws IOException {
				return receive(localFileToSave, null);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @seeorg.eclipse.ecf.filetransfer.events.
			 * IIncomingFileTransferReceiveStartEvent
			 * #receive(java.io.File,
			 * org.eclipse.ecf.filetransfer.FileTransferJob)
			 */
			public IIncomingFileTransfer receive(File localFileToSave, FileTransferJob fileTransferJob) throws IOException {
				setOutputStream(new BufferedOutputStream(new FileOutputStream(localFileToSave)));
				setupAndScheduleJob(fileTransferJob);
				return AbstractRetrieveFileTransfer.this;
			}

			/**
			 * @param streamToStore
			 * @return incoming file transfer instance.
			 * @throws IOException
			 *             not thrown in this implementation.
			 */
			public IIncomingFileTransfer receive(OutputStream streamToStore) throws IOException {
				return receive(streamToStore, null);
			}

			/**
			 * @throws IOException
			 *             not actually thrown by this implementation.
			 */
			public IIncomingFileTransfer receive(OutputStream streamToStore, FileTransferJob fileTransferJob) throws IOException {
				setOutputStream(streamToStore);
				setCloseOutputStream(false);
				setupAndScheduleJob(fileTransferJob);
				return AbstractRetrieveFileTransfer.this;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @seeorg.eclipse.ecf.filetransfer.events.
			 * IIncomingFileTransferReceiveStartEvent#cancel()
			 */
			public void cancel() {
				AbstractRetrieveFileTransfer.this.cancel();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Object#toString()
			 */
			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceiveStartEvent["); //$NON-NLS-1$
				sb.append("isdone=").append(isDone()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append("]"); //$NON-NLS-1$
				return sb.toString();
			}

			public Map getResponseHeaders() {
				return responseHeaders;
			}

		});
	}

	protected void fireReceiveResumedEvent() {
		listener.handleTransferEvent(new IIncomingFileTransferReceiveResumedEvent() {

			public IIncomingFileTransfer getSource() {
				return AbstractRetrieveFileTransfer.this;
			}

			public IFileID getFileID() {
				return remoteFileID;
			}

			public IIncomingFileTransfer receive(File localFileToSave, boolean append) throws IOException {
				return receive(localFileToSave, null, append);
			}

			public IIncomingFileTransfer receive(File localFileToSave, FileTransferJob fileTransferJob, boolean append) throws IOException {
				setOutputStream(new BufferedOutputStream(new FileOutputStream(localFileToSave.getName(), append)));
				setupAndScheduleJob(fileTransferJob);
				return AbstractRetrieveFileTransfer.this;
			}

			/**
			 * @param streamToStore
			 * @return incoming file transfer instance.
			 * @throws IOException
			 *             not thrown in this implementation.
			 */
			public IIncomingFileTransfer receive(OutputStream streamToStore) throws IOException {
				return receive(streamToStore, null);
			}

			/**
			 * @throws IOException
			 *             not actually thrown by this implementation.
			 */
			public IIncomingFileTransfer receive(OutputStream streamToStore, FileTransferJob fileTransferJob) throws IOException {
				setOutputStream(streamToStore);
				setCloseOutputStream(false);
				setupAndScheduleJob(fileTransferJob);
				return AbstractRetrieveFileTransfer.this;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @seeorg.eclipse.ecf.filetransfer.events.
			 * IIncomingFileTransferReceiveStartEvent#cancel()
			 */
			public void cancel() {
				hardClose();
			}

			public String toString() {
				final StringBuffer sb = new StringBuffer("IIncomingFileTransferReceiveResumedEvent["); //$NON-NLS-1$
				sb.append("isdone=").append(isDone()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("bytesReceived=").append(bytesReceived) //$NON-NLS-1$
						.append("]"); //$NON-NLS-1$
				return sb.toString();
			}

			public Map getResponseHeaders() {
				return responseHeaders;
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.filetransfer.IIncomingFileTransfer#getFileRangeSpecification
	 * ()
	 */
	public IFileRangeSpecification getFileRangeSpecification() {
		return rangeSpecification;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#
	 * sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID,
	 * org.eclipse.ecf.filetransfer.IFileRangeSpecification,
	 * org.eclipse.ecf.filetransfer.IFileTransferListener, java.util.Map)
	 */
	/**
	 * @throws IncomingFileTransferException if some problem sending retrieve request
	 */
	public void sendRetrieveRequest(IFileID rFileID, IFileRangeSpecification rangeSpec, IFileTransferListener transferListener, Map ops) throws IncomingFileTransferException {
		Assert.isNotNull(rFileID, Messages.AbstractRetrieveFileTransfer_RemoteFileID_Not_Null);
		Assert.isNotNull(transferListener, Messages.AbstractRetrieveFileTransfer_TransferListener_Not_Null);
		synchronized (jobLock) {
			this.job = null;
		}
		this.remoteFileURL = null;
		this.remoteFileID = rFileID;
		this.listener = transferListener;
		this.remoteFileContents = null;
		this.localFileContents = null;
		this.closeOutputStream = true;
		resetDoneAndException();
		this.bytesReceived = 0;
		this.fileLength = -1;
		this.options = ops;
		this.paused = false;
		this.rangeSpecification = rangeSpec;

		try {
			this.remoteFileURL = rFileID.getURL();
		} catch (final MalformedURLException e) {
			setDoneException(e);
			fireTransferReceiveDoneEvent();
			return;
		}
		try {
			setupProxies();
			openStreams();
		} catch (final IncomingFileTransferException e) {
			setDoneException(e);
			fireTransferReceiveDoneEvent();
		}
	}

	/**
	 * Setup ECF proxy. Subclasses must override this method to do appropriate
	 * proxy setup. This method will be called from within
	 * {@link #sendRetrieveRequest(IFileID, IFileTransferListener, Map)} and
	 * {@link #sendRetrieveRequest(IFileID, IFileRangeSpecification, IFileTransferListener, Map)}
	 * , prior to the actual call to {@link #openStreams()}.
	 * 
	 * @param proxy
	 *            the proxy to be setup. Will not be <code>null</code>.
	 */
	protected abstract void setupProxy(Proxy proxy);

	/**
	 * Select a single proxy from a set of proxies available for the given host.
	 * This implementation selects in the following manner: 1) If proxies
	 * provided is null or array of 0 length, null is returned. If only one
	 * proxy is available (array of length 1) then the entry is returned. If
	 * proxies provided is length greater than 1, then if the type of a proxy in the array
	 * matches the given protocol (e.g. http, https), then the first matching
	 * proxy is returned. If the protocol does not match any of the proxies,
	 * then the *first* proxy (i.e. proxies[0]) is returned. Subclasses may
	 * override if desired.
	 * 
	 * @param protocol
	 *            the target protocol (e.g. http, https, scp, etc). Will not be
	 *            <code>null</code>.
	 * @param proxies
	 *            the proxies to select from. May be <code>null</code> or array
	 *            of length 0.
	 * @return proxy data selected from the proxies provided.
	 */
	protected IProxyData selectProxyFromProxies(String protocol, IProxyData[] proxies) {
		if (proxies == null || proxies.length == 0)
			return null;
		// If only one proxy is available, then use that
		if (proxies.length == 1)
			return proxies[0];
		// If more than one proxy is available, then if http/https protocol then
		// look for that
		// one...if not found then use first
		if (protocol.equalsIgnoreCase("http")) { //$NON-NLS-1$
			for (IProxyData proxie : proxies) {
				if (proxie.getType().equals(IProxyData.HTTP_PROXY_TYPE)) {
					return proxie;
				}
			}
		} else if (protocol.equalsIgnoreCase("https")) { //$NON-NLS-1$
			for (IProxyData proxie : proxies) {
				if (proxie.getType().equals(IProxyData.HTTPS_PROXY_TYPE)) {
					return proxie;
				}
			}
		}
		// If we haven't found it yet, then return the first one.
		return proxies[0];
	}

	protected void setupProxies() {
		// If it's been set directly (via ECF API) then this overrides platform
		// settings
		if (proxy == null) {
			try {
				proxy = ProxySetupHelper.getProxy(getRemoteFileURL().toExternalForm());
			} catch (NoClassDefFoundError e) {
				// If the proxy API is not available a NoClassDefFoundError will be thrown here.
				// If that happens then we just want to continue on.
				Activator.logNoProxyWarning(e);
			}
		}
		if (proxy != null)
			setupProxy(proxy);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.filetransfer.IIncomingFileTransfer#getRemoteFileName()
	 */
	public String getRemoteFileName() {
		String pathStr = getRemoteFileURL().getPath();
		if (pathStr.length() > 0) {
			IPath path = Path.fromPortableString(pathStr);
			if (path.segmentCount() > 0)
				return path.lastSegment();
		}
		return null;
	}

	protected boolean targetHasGzSuffix(String target) {
		if (target == null)
			return false;
		if (target.endsWith(".gz")) //$NON-NLS-1$
			return true;
		return false;
	}

}
