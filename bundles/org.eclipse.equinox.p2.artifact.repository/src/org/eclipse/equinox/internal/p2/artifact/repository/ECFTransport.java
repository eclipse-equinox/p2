/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation Stefan Liebig -
 * random tweaks
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.events.*;
import org.eclipse.ecf.filetransfer.identity.FileCreateException;
import org.eclipse.ecf.filetransfer.identity.FileIDFactory;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.artifact.repository.IStateful;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A transport implementation that uses ECF file transfer API.
 */
public class ECFTransport extends Transport {

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

	protected IStatus convertToStatus(Exception failure) {
		if (failure == null)
			return Status.OK_STATUS;
		if (failure instanceof UserCancelledException)
			return new Status(IStatus.CANCEL, Activator.ID, failure.getMessage(), failure);
		return new Status(IStatus.ERROR, Activator.ID, "error during transfer", failure);
	}

	public IStatus download(String toDownload, OutputStream target, IProgressMonitor monitor) {
		IRetrieveFileTransferFactory factory = (IRetrieveFileTransferFactory) retrievalFactoryTracker.getService();
		if (factory == null)
			return statusOn(target, new Status(IStatus.ERROR, Activator.ID, "ECF Transfer manager not available"));

		return transfer(factory.newInstance(), toDownload, target, monitor);
	}

	private IStatus transfer(final IRetrieveFileTransferContainerAdapter retrievalContainer, final String toDownload, final OutputStream target, final IProgressMonitor monitor) {
		final IStatus[] result = new IStatus[1];
		IFileTransferListener listener = new IFileTransferListener() {
			public void handleTransferEvent(IFileTransferEvent event) {
				if (event instanceof IIncomingFileTransferReceiveStartEvent) {
					IIncomingFileTransferReceiveStartEvent rse = (IIncomingFileTransferReceiveStartEvent) event;
					try {
						if (target != null) {
							rse.receive(target);
						}
					} catch (IOException e) {
						IStatus status = convertToStatus(e);
						synchronized (result) {
							result[0] = status;
							result.notify();
						}
					}
				}
				if (event instanceof IIncomingFileTransferReceiveDataEvent) {
					IIncomingFileTransfer source = ((IIncomingFileTransferReceiveDataEvent) event).getSource();
					// TODO do proper monitor things here.
					if (monitor != null) {
						monitor.subTask("Transferring " + prettyName(source.getID()) + " (" + (int) (source.getPercentComplete() * 100) + "% complete)");
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

			/**
			 * Create a nice, simple name for the given id, suitable for progress messages.
			 */
			private String prettyName(ID id) {
				if (id == null)
					return ""; //$NON-NLS-1$
				String simpleName = id.getName();
				//if the name is not too long, just return it
				if (simpleName.length() < 30)
					return simpleName;
				int lastSlash = simpleName.lastIndexOf('/');
				//if it is a segmented name, use only the last segment for a cleaner task message
				if (lastSlash > 0 && lastSlash < (simpleName.length() - 1))
					simpleName = simpleName.substring(lastSlash + 1);
				return simpleName;
			}
		};

		try {
			retrievalContainer.sendRetrieveRequest(FileIDFactory.getDefault().createFileID(retrievalContainer.getRetrieveNamespace(), toDownload), listener, null);
		} catch (IncomingFileTransferException e) {
			return statusOn(target, new Status(IStatus.ERROR, Activator.ID, "error during transfer", e));
		} catch (FileCreateException e) {
			return statusOn(target, new Status(IStatus.ERROR, Activator.ID, "error during transfer - could not create file", e));
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

	private static IStatus statusOn(OutputStream target, IStatus status) {
		if (target instanceof IStateful)
			((IStateful) target).setStatus(status);
		return status;
	}
}
