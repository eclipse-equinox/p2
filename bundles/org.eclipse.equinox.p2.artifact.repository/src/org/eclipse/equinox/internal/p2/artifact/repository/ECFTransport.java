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
import java.net.URLEncoder;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.security.ConnectContextFactory;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.events.*;
import org.eclipse.ecf.filetransfer.identity.FileCreateException;
import org.eclipse.ecf.filetransfer.identity.FileIDFactory;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IStateful;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI.AuthenticationInfo;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.security.storage.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A transport implementation that uses ECF file transfer API.
 */
public class ECFTransport extends Transport {
	/**
	 * The number of password retry attempts allowed before failing.
	 */
	private static final int LOGIN_RETRIES = 3;

	private static final ProtocolException ERROR_401 = new ProtocolException();

	private static final String SERVER_REDIRECT = "Server redirected too many times"; //$NON-NLS-1$

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
		startBundle("org.eclipse.ecf.provider.filetransfer"); //$NON-NLS-1$
	}

	private boolean startBundle(String bundleId) {
		PackageAdmin packageAdmin = (PackageAdmin) ServiceHelper.getService(Activator.getContext(), PackageAdmin.class.getName());
		if (packageAdmin == null)
			return false;

		Bundle[] bundles = packageAdmin.getBundles(bundleId, null);
		if (bundles != null && bundles.length > 0) {
			for (int i = 0; i < bundles.length; i++) {
				try {
					if ((bundles[0].getState() & Bundle.INSTALLED) == 0) {
						bundles[0].start();
						return true;
					}
				} catch (BundleException e) {
					// failed, try next bundle
				}
			}
		}
		return false;
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
			return statusOn(target, new Status(IStatus.ERROR, Activator.ID, Messages.ecf_configuration_error));

		return transfer(factory.newInstance(), toDownload, target, context, monitor);
	}

	private IStatus transfer(final IRetrieveFileTransferContainerAdapter retrievalContainer, final String toDownload, final OutputStream target, IConnectContext context, final IProgressMonitor monitor) throws ProtocolException {
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
						if (monitor.isCanceled()) {
							synchronized (result) {
								result[0] = Status.CANCEL_STATUS;
								source.cancel();
								result.notify();
							}
						}
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
			retrievalContainer.setConnectContextForAuthentication(context);
			retrievalContainer.sendRetrieveRequest(FileIDFactory.getDefault().createFileID(retrievalContainer.getRetrieveNamespace(), toDownload), listener, null);
		} catch (IncomingFileTransferException e) {
			if (e.getErrorCode() == 401)
				throw ERROR_401;
			//try to figure out if we have a 401 by parsing the exception message
			IStatus status = e.getStatus();
			Throwable exception = status.getException();
			if (exception instanceof IOException) {
				if (exception.getMessage() != null && (exception.getMessage().indexOf(" 401 ") != -1 || exception.getMessage().indexOf(SERVER_REDIRECT) != -1)) //$NON-NLS-1$
					throw ERROR_401;
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
		String nodeKey;
		try {
			nodeKey = URLEncoder.encode(hostLocation.toString(), "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e2) {
			//fall back to default platform encoding
			nodeKey = URLEncoder.encode(hostLocation.toString());
		}
		String nodeName = IRepository.PREFERENCE_NODE + '/' + nodeKey;
		ISecurePreferences prefNode = null;
		if (securePreferences.nodeExists(nodeName))
			prefNode = securePreferences.node(nodeName);
		if (!prompt) {
			if (prefNode == null)
				return null;
			try {
				String username = prefNode.get(IRepository.PROP_USERNAME, null);
				String password = prefNode.get(IRepository.PROP_PASSWORD, null);
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
			if (prefNode == null)
				prefNode = securePreferences.node(nodeName);
			try {
				prefNode.put(IRepository.PROP_USERNAME, loginDetails.getUserName(), true);
				prefNode.put(IRepository.PROP_PASSWORD, loginDetails.getPassword(), true);
				prefNode.flush();
			} catch (StorageException e1) {
				String msg = NLS.bind(Messages.repoMan_internalError, xmlLocation.toString());
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
			} catch (IOException e) {
				String msg = NLS.bind(Messages.repoMan_internalError, xmlLocation.toString());
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
			}
		}
		return ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());
	}

	private static IStatus statusOn(OutputStream target, IStatus status) {
		if (target instanceof IStateful)
			((IStateful) target).setStatus(status);
		return status;
	}
}
