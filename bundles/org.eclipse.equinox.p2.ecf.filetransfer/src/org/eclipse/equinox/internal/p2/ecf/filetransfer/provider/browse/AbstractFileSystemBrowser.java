/****************************************************************************
 * Copyright (c) 2007, 2010 Composent, Inc., IBM and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *    Henrich Kraemer - bug 263613, [transport] Update site contacting / downloading is not cancelable
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.provider.filetransfer.browse;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.IRemoteFile;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemRequest;
import org.eclipse.ecf.filetransfer.UserCancelledException;
import org.eclipse.ecf.filetransfer.events.IRemoteFileSystemBrowseEvent;
import org.eclipse.ecf.filetransfer.events.IRemoteFileSystemEvent;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.internal.provider.filetransfer.Activator;
import org.eclipse.ecf.internal.provider.filetransfer.Messages;
import org.eclipse.ecf.provider.filetransfer.util.ProxySetupHelper;

/**
 * Abstract class for browsing an efs file system.
 */
public abstract class AbstractFileSystemBrowser {

	protected IFileID fileID = null;
	protected IRemoteFileSystemListener listener = null;

	private Exception exception = null;
	protected IRemoteFile[] remoteFiles = null;

	protected Proxy proxy;
	protected URL directoryOrFile;

	protected IConnectContext connectContext;

	protected DirectoryJob job = null;

	Object lock = new Object();

	protected class DirectoryJob extends Job {

		private IRemoteFileSystemRequest request;

		public DirectoryJob() {
			super(fileID.getName());
		}

		protected IStatus run(IProgressMonitor monitor) {
			try {
				if (monitor.isCanceled())
					throw newUserCancelledException();
				runRequest();
			} catch (Exception e) {
				AbstractFileSystemBrowser.this.setException(e);
			} finally {
				listener.handleRemoteFileEvent(createRemoteFileEvent());
				cleanUp();
			}
			return Status.OK_STATUS;
		}

		public void setRequest(IRemoteFileSystemRequest request) {
			this.request = request;
		}

		public IRemoteFileSystemRequest getRequest() {
			return request;
		}

		protected void canceling() {
			request.cancel();
		}

	}

	protected void cancel() {
		synchronized (lock) {
			if (job != null) {
				job.cancel();
			}
		}

	}

	protected void cleanUp() {
		synchronized (lock) {
			job = null;
		}
	}

	/**
	 * Run the actual request.  This method is called within the job created to actually get the
	 * directory or file information.
	 * @throws Exception if some problem with making the request or receiving response to the request.
	 */
	protected abstract void runRequest() throws Exception;

	public AbstractFileSystemBrowser(IFileID directoryOrFileID, IRemoteFileSystemListener listener, URL url, IConnectContext connectContext, Proxy proxy) {
		Assert.isNotNull(directoryOrFileID);
		this.fileID = directoryOrFileID;
		Assert.isNotNull(listener);
		this.listener = listener;
		this.directoryOrFile = url;
		this.connectContext = connectContext;
		this.proxy = proxy;
	}

	public abstract class RemoteFileSystemRequest implements IRemoteFileSystemRequest {
		public void cancel() {
			synchronized (lock) {
				if (job != null)
					job.cancel();
			}
		}

		public IFileID getFileID() {
			return fileID;
		}

		public IRemoteFileSystemListener getRemoteFileListener() {
			return listener;
		}

	}

	public IRemoteFileSystemRequest sendBrowseRequest() {
		job = new DirectoryJob();

		IRemoteFileSystemRequest request = createRemoteFileSystemRequest();
		job.setRequest(request);
		if (Job.getJobManager().isSuspended()) {
			job.run(new NullProgressMonitor());
		} else {
			job.schedule();
		}
		return request;
	}

	protected IRemoteFileSystemRequest createRemoteFileSystemRequest() {
		return new RemoteFileSystemRequest() {
			public <T> T getAdapter(Class<T> adapter) {
				if (adapter == null) {
					return null;
				}
				if (adapter.isInstance(this)) {
					return adapter.cast(this);
				}
				return null;
			}

		};
	}

	/**
	 * @return file system directory event
	 */
	protected IRemoteFileSystemEvent createRemoteFileEvent() {
		return new IRemoteFileSystemBrowseEvent() {

			public IFileID getFileID() {
				return fileID;
			}

			public Exception getException() {
				return exception;
			}

			public String toString() {
				StringBuilder buf = new StringBuilder("RemoteFileSystemBrowseEvent["); //$NON-NLS-1$
				buf.append("fileID=").append(fileID).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
				List list = (remoteFiles != null) ? Arrays.asList(remoteFiles) : null;
				buf.append("files=").append(list).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
				return buf.toString();
			}

			public IRemoteFile[] getRemoteFiles() {
				return remoteFiles;
			}
		};
	}

	protected abstract void setupProxy(Proxy proxy);

	/**
	 * Select a single proxy from a set of proxies available for the given host.  This implementation
	 * selects in the following manner:  1) If proxies provided is null or array of 0 length, null 
	 * is returned.  If only one proxy is available (array of length 1) then the entry is returned.
	 * If proxies provided is length greater than 1, then if the type of a proxy in the array matches the given
	 * protocol (e.g. http, https), then the first matching proxy is returned.  If the protocol does
	 * not match any of the proxies, then the *first* proxy (i.e. proxies[0]) is returned.  Subclasses may
	 * override if desired.
	 * 
	 * @param protocol the target protocol (e.g. http, https, scp, etc).  Will not be <code>null</code>.
	 * @param proxies the proxies to select from.  May be <code>null</code> or array of length 0.
	 * @return proxy data selected from the proxies provided.  
	 */
	protected IProxyData selectProxyFromProxies(String protocol, IProxyData[] proxies) {
		try {
			return ProxySetupHelper.selectProxyFromProxies(protocol, proxies);
		} catch (NoClassDefFoundError e) {
			// If the proxy API is not available a NoClassDefFoundError will be thrown here.
			// If that happens then we just want to continue on.
			Activator.logNoProxyWarning(e);
			return null;
		}
	}

	protected void setupProxies() {
		// If it's been set directly (via ECF API) then this overrides platform settings
		if (proxy == null) {
			try {
				proxy = ProxySetupHelper.getProxy(directoryOrFile.toExternalForm());
			} catch (NoClassDefFoundError e) {
				// If the proxy API is not available a NoClassDefFoundError will be thrown here.
				// If that happens then we just want to continue on.
				Activator.logNoProxyWarning(e);
			}
		}
		if (proxy != null)
			setupProxy(proxy);
	}

	protected synchronized void setException(Exception exception) {
		this.exception = exception;
	}

	protected synchronized Exception getException() {
		return this.exception;
	}

	protected synchronized boolean isCanceled() {
		return exception instanceof UserCancelledException;
	}

	protected synchronized void setCanceled(Exception e) {
		if (e instanceof UserCancelledException) {
			exception = e;
		} else {
			exception = newUserCancelledException();
		}
	}

	protected UserCancelledException newUserCancelledException() {
		return new UserCancelledException(Messages.AbstractRetrieveFileTransfer_Exception_User_Cancelled);
	}

}
