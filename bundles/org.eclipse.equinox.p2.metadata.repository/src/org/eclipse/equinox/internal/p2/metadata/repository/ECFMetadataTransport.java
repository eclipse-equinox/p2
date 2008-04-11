/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.*;
import org.eclipse.ecf.core.security.ConnectContextFactory;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.events.*;
import org.eclipse.ecf.filetransfer.identity.FileCreateException;
import org.eclipse.ecf.filetransfer.identity.FileIDFactory;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI.AuthenticationInfo;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.security.storage.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.util.tracker.ServiceTracker;

public class ECFMetadataTransport {
	private static final String SERVER_REDIRECT = "Server redirected too many times"; //$NON-NLS-1$
	/**
	 * The number of password retry attempts allowed before failing.
	 */
	private static final int LOGIN_RETRIES = 3;
	private static final ProtocolException ERROR_401 = new ProtocolException();

	/**
	 * The singleton transport instance.
	 */
	private static ECFMetadataTransport instance;

	private final ServiceTracker retrievalFactoryTracker;

	public static synchronized ECFMetadataTransport getInstance() {
		if (instance == null) {
			instance = new ECFMetadataTransport();
		}
		return instance;
	}

	private ECFMetadataTransport() {
		retrievalFactoryTracker = new ServiceTracker(Activator.getContext(), IRetrieveFileTransferFactory.class.getName(), null);
		retrievalFactoryTracker.open();
	}

	public IStatus download(String url, OutputStream destination, IProgressMonitor monitor) {
		try {
			IConnectContext context = getConnectionContext(url, false);
			for (int i = 0; i < LOGIN_RETRIES; i++) {
				try {
					return performDownload(url, destination, context, monitor);
				} catch (ProtocolException e) {
					if (e == ERROR_401)
						context = getConnectionContext(url, true);
				}
			}
		} catch (UserCancelledException e) {
			return Status.CANCEL_STATUS;
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		//reached maximum number of retries without success
		return new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, NLS.bind(Messages.io_failedRead, url), null);
	}

	public IStatus performDownload(String toDownload, OutputStream target, IConnectContext context, IProgressMonitor monitor) throws ProtocolException {
		IRetrieveFileTransferFactory factory = (IRetrieveFileTransferFactory) retrievalFactoryTracker.getService();
		if (factory == null)
			return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.io_failedRead, toDownload));
		return transfer(factory.newInstance(), toDownload, target, context, monitor);
	}

	/**
	 * Gets the last modified date for the specified file.
	 * @param location - The URL location of the file.
	 * @return A <code>long</code> representing the date. Returns <code>0</code> if the file is not found or an error occurred.
	 * @exception OperationCanceledException if the request was canceled.
	 */
	public long getLastModified(URL location) throws ProvisionException {
		String locationString = location.toExternalForm();
		try {
			IConnectContext context = getConnectionContext(locationString, false);
			for (int i = 0; i < LOGIN_RETRIES; i++) {
				try {
					return doGetLastModified(locationString, context);
				} catch (ProtocolException e) {
					if (ERROR_401 == e)
						context = getConnectionContext(locationString, true);
				} catch (Exception e) {
					e.getMessage();
				}
			}
		} catch (UserCancelledException e) {
			throw new OperationCanceledException();
		}
		//too many retries, so report as failure
		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, NLS.bind(Messages.io_failedRead, locationString), null));
	}

	/**
	 * Perform the ECF call to get the last modified time, failing if there is any
	 * protocol failure such as an authentication failure.
	 */
	private long doGetLastModified(String location, IConnectContext context) throws ProtocolException {
		IContainer container;
		try {
			container = ContainerFactory.getDefault().createContainer();
		} catch (ContainerCreateException e) {
			return 0;
		}
		IRemoteFileSystemBrowserContainerAdapter adapter = (IRemoteFileSystemBrowserContainerAdapter) container.getAdapter(IRemoteFileSystemBrowserContainerAdapter.class);
		if (adapter == null) {
			return 0;
		}
		IRemoteFile remoteFile = checkFile(adapter, location, context);
		if (remoteFile == null) {
			return 0;
		}
		return remoteFile.getInfo().getLastModified();
	}

	/**
	 * Returns the connection context for the given URL. This may prompt the
	 * user for user name and password as required.
	 * 
	 * @param xmlLocation - the file location requiring login details
	 * @param prompt - use <code>true</code> to prompt the user instead of
	 * looking at the secure preference store for login, use <code>false</code>
	 * to only try the secure preference store
	 * @throws UserCancelledException when the user cancels the login prompt 
	 * @throws ProvisionException if the password cannot be read or saved
	 * @return The connection context
	 */
	public IConnectContext getConnectionContext(String xmlLocation, boolean prompt) throws UserCancelledException, ProvisionException {
		ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
		IPath hostLocation = new Path(xmlLocation).removeLastSegments(1);
		int repositoryHash = hostLocation.hashCode();
		ISecurePreferences metadataNode = securePreferences.node(IRepository.PREFERENCE_NODE + "/" + repositoryHash); //$NON-NLS-1$
		if (!prompt) {
			try {
				String username = metadataNode.get(IRepository.PROP_USERNAME, null);
				String password = metadataNode.get(IRepository.PROP_PASSWORD, null);
				//if we don't have stored connection data just return a null connection context
				if (username == null || password == null)
					return null;
				return ConnectContextFactory.createUsernamePasswordConnectContext(username, password);
			} catch (StorageException e) {
				String msg = NLS.bind(Messages.repoMan_internalError, xmlLocation.toString());
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
			}
		}
		//need to prompt user for user name and password
		ServiceTracker adminUITracker = new ServiceTracker(Activator.getContext(), IServiceUI.class.getName(), null);
		adminUITracker.open();
		IServiceUI adminUIService = (IServiceUI) adminUITracker.getService();
		AuthenticationInfo loginDetails = null;
		if (adminUIService != null)
			loginDetails = adminUIService.getUsernamePassword(hostLocation.toString());
		//null result means user canceled password dialog
		if (loginDetails == null)
			throw new UserCancelledException();
		//save user name and password if requested by user
		if (loginDetails.saveResult()) {
			try {
				metadataNode.put(IRepository.PROP_USERNAME, loginDetails.getUserName(), true);
				metadataNode.put(IRepository.PROP_PASSWORD, loginDetails.getPassword(), true);
			} catch (StorageException e1) {
				String msg = NLS.bind(Messages.repoMan_internalError, xmlLocation.toString());
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
			}
		}
		return ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());
	}

	private IRemoteFile checkFile(final IRemoteFileSystemBrowserContainerAdapter retrievalContainer, final String location, IConnectContext context) throws ProtocolException {
		final Object[] result = new Object[2];
		final Boolean[] done = new Boolean[1];
		done[0] = new Boolean(false);
		IRemoteFileSystemListener listener = new IRemoteFileSystemListener() {
			public void handleRemoteFileEvent(IRemoteFileSystemEvent event) {
				Exception exception = event.getException();
				if (exception != null) {
					synchronized (result) {
						result[0] = null;
						result[1] = exception;
						done[0] = new Boolean(true);
						result.notify();
					}
				} else if (event instanceof IRemoteFileSystemBrowseEvent) {
					IRemoteFileSystemBrowseEvent fsbe = (IRemoteFileSystemBrowseEvent) event;
					IRemoteFile[] remoteFiles = fsbe.getRemoteFiles();
					if (remoteFiles != null && remoteFiles.length > 0) {
						synchronized (result) {
							result[0] = remoteFiles[0];
							done[0] = new Boolean(true);
							result.notify();
						}
					} else {
						synchronized (result) {
							result[0] = null;
							done[0] = new Boolean(true);
							result.notify();
						}
					}
				}
			}
		};
		try {
			retrievalContainer.setConnectContextForAuthentication(context);
			retrievalContainer.sendBrowseRequest(FileIDFactory.getDefault().createFileID(retrievalContainer.getBrowseNamespace(), location), listener);
		} catch (RemoteFileSystemException e) {
			return null;
		} catch (FileCreateException e) {
			return null;
		}
		synchronized (result) {
			while (!done[0].booleanValue()) {
				boolean logged = false;
				try {
					result.wait();
				} catch (InterruptedException e) {
					if (!logged)
						LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Unexpected interrupt while waiting on ECF browse", e)); //$NON-NLS-1$
				}
			}
		}
		if (result[0] == null && result[1] instanceof IOException) {
			IOException ioException = (IOException) result[1];
			//throw a special exception for authentication failure so we know to prompt for username/password
			String message = ioException.getMessage();
			if (message != null && (message.indexOf("401") != -1 || message.indexOf(SERVER_REDIRECT) != -1)) //$NON-NLS-1$
				throw ERROR_401;
		}
		return (IRemoteFile) result[0];
	}

	private IStatus transfer(final IRetrieveFileTransferContainerAdapter retrievalContainer, final String toDownload, final OutputStream target, IConnectContext context, final IProgressMonitor monitor) throws ProtocolException {
		final IStatus[] result = new IStatus[1];
		IFileTransferListener listener = new IFileTransferListener() {

			public void handleTransferEvent(IFileTransferEvent event) {
				if (event instanceof IIncomingFileTransferReceiveStartEvent) {
					IIncomingFileTransferReceiveStartEvent rse = (IIncomingFileTransferReceiveStartEvent) event;
					if (target != null) {
						try {
							rse.receive(target);
						} catch (IOException e) {
							IStatus status = convertToStatus(e);
							synchronized (result) {
								result[0] = status;
								result.notify();
							}
						}
					}
				}
				if (event instanceof IIncomingFileTransferReceiveDataEvent) {
					IIncomingFileTransfer source = ((IIncomingFileTransferReceiveDataEvent) event).getSource();
					if (monitor != null) {
						if (monitor.isCanceled())
							source.cancel();
					}
				}
				if (event instanceof IIncomingFileTransferReceiveDoneEvent) {
					IStatus status = convertToStatus(((IIncomingFileTransferReceiveDoneEvent) event).getException());
					synchronized (result) {
						result[0] = status;
						result.notify();
					}
				}
			}
		};

		try {
			retrievalContainer.setConnectContextForAuthentication(context);
			retrievalContainer.sendRetrieveRequest(FileIDFactory.getDefault().createFileID(retrievalContainer.getRetrieveNamespace(), toDownload), listener, null);
		} catch (IncomingFileTransferException e) {
			IStatus status = e.getStatus();
			Throwable exception = status.getException();
			if (exception instanceof IOException) {
				String message = exception.getMessage();
				if (message != null && (message.indexOf("401") != -1 || message.indexOf(SERVER_REDIRECT) != -1)) //$NON-NLS-1$
					throw ERROR_401;
			}
			return status;
		} catch (FileCreateException e) {
			return e.getStatus();
		}
		synchronized (result) {
			while (result[0] == null) {
				boolean logged = false;
				try {
					result.wait();
				} catch (InterruptedException e) {
					if (!logged)
						LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Unexpected interrupt while waiting on ECF transfer", e)); //$NON-NLS-1$
				}
			}
		}

		return result[0];
	}

	protected IStatus convertToStatus(Exception e) {
		if (e == null)
			return Status.OK_STATUS;
		if (e instanceof UserCancelledException)
			return new Status(IStatus.CANCEL, Activator.ID, e.getMessage(), e);
		return new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e);
	}
}
