/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *		compeople AG (Stefan Liebig) - various ongoing maintenance
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.*;
import java.net.ProtocolException;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.security.ConnectContextFactory;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.events.*;
import org.eclipse.ecf.filetransfer.identity.FileCreateException;
import org.eclipse.ecf.filetransfer.identity.FileIDFactory;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IStateful;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.security.storage.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A transport implementation that uses ECF file transfer API.
 */
public class ECFTransport extends Transport {
	/**
	 * The number of password retry attempts allowed before failing.
	 */
	private static final int LOGIN_RETRIES = 3;

	protected String username;
	protected String password;
	private static final String ERROR_FILENOTFOUND = "FileNotFound"; //$NON-NLS-1$
	private static final String ERROR_401 = "401"; //$NON-NLS-1$

	/**
	 * The singleton transport instance.
	 */
	private static ECFTransport instance;

	private final ServiceTracker retrievalFactoryTracker;

	/**
	 * Returns an initialized instance of ECFTransport
	 */
	public static synchronized ECFTransport getInstance() {
		if (instance == null) {
			instance = new ECFTransport();
		}
		return instance;
	}

	/**
	 * Private to avoid client instantiation.
	 */
	private ECFTransport() {
		retrievalFactoryTracker = new ServiceTracker(Activator.getContext(), IRetrieveFileTransferFactory.class.getName(), null);
		retrievalFactoryTracker.open();
	}

	protected IStatus convertToStatus(IFileTransferEvent event, Exception failure, long startTime, String location) {
		long speed = DownloadStatus.UNKNOWN_RATE;
		if (event instanceof IIncomingFileTransferEvent) {
			long bytes = ((IIncomingFileTransferEvent) event).getSource().getBytesReceived();
			if (bytes > 0) {
				long elapsed = (System.currentTimeMillis() - startTime) / 1000;//in seconds
				if (elapsed == 0)
					elapsed = 1;
				speed = bytes / elapsed;
			}
		}
		DownloadStatus result = null;
		if (failure == null)
			result = new DownloadStatus(IStatus.OK, Activator.ID, Status.OK_STATUS.getMessage());
		else if (failure instanceof UserCancelledException)
			result = new DownloadStatus(IStatus.CANCEL, Activator.ID, failure.getMessage(), failure);
		else
			result = new DownloadStatus(IStatus.ERROR, Activator.ID, NLS.bind(Messages.io_failedRead, location), failure);
		result.setTransferRate(speed);
		return result;
	}

	public IStatus download(String url, OutputStream destination, IProgressMonitor monitor) {
		try {
			setLogin(url, false);
			for (int i = 0; i < LOGIN_RETRIES; i++) {
				try {
					return performDownload(url, destination, monitor);
				} catch (ProtocolException e) {
					if (ERROR_401.equals(e.getMessage())) {
						setLogin(url, true);
					}
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

	public IStatus performDownload(String toDownload, OutputStream target, IProgressMonitor monitor) throws ProtocolException {
		IRetrieveFileTransferFactory factory = (IRetrieveFileTransferFactory) retrievalFactoryTracker.getService();
		if (factory == null)
			return statusOn(target, new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.io_failedRead, toDownload)));

		return transfer(factory.newInstance(), toDownload, target, monitor);
	}

	private IStatus transfer(final IRetrieveFileTransferContainerAdapter retrievalContainer, final String toDownload, final OutputStream target, final IProgressMonitor monitor) throws ProtocolException {
		final IStatus[] result = new IStatus[1];
		final long startTime = System.currentTimeMillis();
		IFileTransferListener listener = new IFileTransferListener() {
			public void handleTransferEvent(IFileTransferEvent event) {
				if (event instanceof IIncomingFileTransferReceiveStartEvent) {
					IIncomingFileTransferReceiveStartEvent rse = (IIncomingFileTransferReceiveStartEvent) event;
					try {
						if (target != null) {
							rse.receive(target);
						}
					} catch (IOException e) {
						IStatus status = convertToStatus(event, e, startTime, toDownload);
						synchronized (result) {
							result[0] = status;
							result.notify();
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
					Exception exception = ((IIncomingFileTransferReceiveDoneEvent) event).getException();
					IStatus status = convertToStatus(event, exception, startTime, toDownload);
					synchronized (result) {
						result[0] = status;
						result.notify();
					}
				}
			}
		};

		try {
			if (username != null && password != null) {
				IConnectContext connectContext = ConnectContextFactory.createUsernamePasswordConnectContext(username, password);
				retrievalContainer.setConnectContextForAuthentication(connectContext);
			} else {
				retrievalContainer.setConnectContextForAuthentication(null);
			}
			retrievalContainer.sendRetrieveRequest(FileIDFactory.getDefault().createFileID(retrievalContainer.getRetrieveNamespace(), toDownload), listener, null);
		} catch (IncomingFileTransferException e) {
			IStatus status = e.getStatus();
			Throwable exception = status.getException();
			if (exception instanceof FileNotFoundException) {
				throw new ProtocolException(ERROR_FILENOTFOUND);
			} else if (exception instanceof IOException) {
				//throw exception that login details are required
				throw new ProtocolException(ERROR_401);
			}
			return statusOn(target, status);
		} catch (FileCreateException e) {
			return statusOn(target, e.getStatus());
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

		return statusOn(target, result[0]);
	}

	/**
	 * Sets the login details from the user for the specified URL. If the login
	 * details are not available, the user is prompted.
	 * @param xmlLocation - the file location requiring login details
	 * @param prompt - use <code>true</code> to prompt the user instead of
	 * looking at the secure preference store for login, use <code>false</code>
	 * to only try the secure preference store
	 * @throws UserCancelledException when the user cancels the login prompt 
	 * @throws ProvisionException if the password cannot be read or saved
	 */
	public void setLogin(String xmlLocation, boolean prompt) throws UserCancelledException, ProvisionException {
		ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
		IPath hostLocation = new Path(xmlLocation).removeLastSegments(1);
		int repositoryHash = hostLocation.hashCode();
		ISecurePreferences metadataNode = securePreferences.node(IRepository.PREFERENCE_NODE + "/" + repositoryHash); //$NON-NLS-1$
		String[] loginDetails = new String[3];
		if (!prompt) {
			try {
				loginDetails[0] = metadataNode.get(IRepository.PROP_USERNAME, null);
				loginDetails[1] = metadataNode.get(IRepository.PROP_PASSWORD, null);
			} catch (StorageException e) {
				String msg = NLS.bind(Messages.repoMan_internalError, xmlLocation.toString());
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
			}
		}
		if ((loginDetails[0] == null || loginDetails[1] == null) && prompt) {
			ServiceTracker adminUITracker = new ServiceTracker(Activator.getContext(), IServiceUI.class.getName(), null);
			adminUITracker.open();
			IServiceUI adminUIService = (IServiceUI) adminUITracker.getService();
			if (adminUIService != null)
				loginDetails = adminUIService.getUsernamePassword(hostLocation.toString());
		}
		if (loginDetails == null) {
			setUsername(null);
			setPassword(null);
			throw new UserCancelledException();
		}
		if (loginDetails[2] != null && Boolean.valueOf(loginDetails[2]).booleanValue()) {
			try {
				metadataNode.put(IRepository.PROP_USERNAME, loginDetails[0], true);
				metadataNode.put(IRepository.PROP_PASSWORD, loginDetails[1], true);
			} catch (StorageException e1) {
				String msg = NLS.bind(Messages.repoMan_internalError, xmlLocation.toString());
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
			}
		}
		setUsername(loginDetails[0]);
		setPassword(loginDetails[1]);
	}

	/**
	 * Sets the username for login purposes on the next connection. Password
	 * must also be set.
	 * @param username - the username, may be <code>null</code>
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Sets the password for login purposes on the next connection. Username
	 * must also be set.
	 * @param password - the password, may be <code>null</code>
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	private static IStatus statusOn(OutputStream target, IStatus status) {
		if (target instanceof IStateful)
			((IStateful) target).setStatus(status);
		return status;
	}
}
