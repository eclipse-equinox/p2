/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation, and others.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 * Contributors:
 * 	IBM Corporation - initial implementation
 * 	Cloudsmith Inc - modified API, and implementation
 *  Red Hat Inc. - Bug 460967
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.transport.ecf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.*;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.events.IRemoteFileSystemBrowseEvent;
import org.eclipse.ecf.filetransfer.identity.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.*;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.internal.p2.repository.Messages;
import org.eclipse.osgi.util.NLS;

class FileInfoReader {
	private final int connectionRetryCount;
	private final long connectionRetryDelay;
	private final IConnectContext connectContext;

	/**
	 * Create a new FileInfoReader that will retry failed connection attempts and
	 * sleep some amount of time between each attempt.
	 */
	FileInfoReader(IConnectContext aConnectContext) {
		connectionRetryCount = RepositoryPreferences.getConnectionRetryCount();
		connectionRetryDelay = RepositoryPreferences.getConnectionMsRetryDelay();
		connectContext = aConnectContext;
	}

	long getLastModified(URI uri, IProgressMonitor monitor)
			throws AuthenticationFailedException, FileNotFoundException, CoreException, JREHttpClientRequiredException {
		SubMonitor convert = SubMonitor.convert(monitor, uri.toString(), connectionRetryCount);
		IContainer container;
		try {
			container = ContainerFactory.getDefault().createContainer();
		} catch (ContainerCreateException e) {
			throw RepositoryStatusHelper.fromExceptionMessage(e, Messages.ecf_configuration_error);
		}
		IRemoteFileSystemBrowserContainerAdapter adapter = container
				.getAdapter(IRemoteFileSystemBrowserContainerAdapter.class);
		if (adapter == null) {
			throw RepositoryStatusHelper.fromMessage(Messages.ecf_configuration_error);
		}
		adapter.setConnectContextForAuthentication(connectContext);
		CoreException summary = new CoreException(Status.error("All download attempts failed")); //$NON-NLS-1$
		for (int retryCount = 0; retryCount < connectionRetryCount; retryCount++) {
			if (convert.isCanceled()) {
				throw new OperationCanceledException();
			}
			AtomicReference<IRemoteFile> remote = new AtomicReference<>();
			AtomicReference<Exception> exception = new AtomicReference<>();
			try {
				IFileID fileID = FileIDFactory.getDefault().createFileID(adapter.getBrowseNamespace(), uri.toString());
				CountDownLatch latch = new CountDownLatch(1);
				IRemoteFileSystemRequest browseRequest = adapter.sendBrowseRequest(fileID, event -> {
					Exception e = event.getException();
					if (e != null) {
						exception.set(e);
					} else if (event instanceof IRemoteFileSystemBrowseEvent fsbe) {
						IRemoteFile[] remoteFiles = fsbe.getRemoteFiles();
						if (remoteFiles != null && remoteFiles.length > 0) {
							remote.set(remoteFiles[0]);
						}
						convert.worked(1);
					}
					latch.countDown();
				});
				while (!latch.await(1, TimeUnit.SECONDS)) {
					if (convert.isCanceled()) {
						browseRequest.cancel();
						throw new OperationCanceledException();
					}
				}
			} catch (RemoteFileSystemException | FileCreateException e) {
				exception.set(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LogHelper.log(new Status(IStatus.WARNING, Activator.ID,
						"Unexpected interrupt while waiting on ECF browse request", e)); //$NON-NLS-1$
				throw new OperationCanceledException();
			}
			Exception e = exception.get();
			if (e == null) {
				IRemoteFile remoteFile = remote.get();
				if (remoteFile == null) {
					throw new FileNotFoundException(uri.toString());
				}
				return remoteFile.getInfo().getLastModified();
			}
			checkException(uri, retryCount, e);
			summary.addSuppressed(e);
		}
		throw summary;
	}

	/**
	 * Utility method to check exception condition and determine if retry should be
	 * done. If there was an exception it is translated into one of the specified
	 * exceptions and thrown.
	 *
	 * @param uri            the URI being read - used for logging purposes
	 * @param attemptCounter - the current attempt number (start with 0)
	 * @return true if the exception is an IOException and attemptCounter <
	 *         connectionRetryCount, false otherwise
	 * @throws JREHttpClientRequiredException
	 */
	private boolean checkException(URI uri, int attemptCounter, Exception exception)
			throws CoreException, FileNotFoundException, AuthenticationFailedException, JREHttpClientRequiredException {
		// check if HTTP client needs to be changed
		RepositoryStatusHelper.checkJREHttpClientRequired(exception);

		// if this is a authentication failure - it is not meaningful to continue
		RepositoryStatusHelper.checkPermissionDenied(exception);

		// if this is a file not found - it is not meaningful to continue
		RepositoryStatusHelper.checkFileNotFound(exception, uri);

		Throwable t = RepositoryStatusHelper.unwind(exception);
		if (t instanceof CoreException) {
			throw RepositoryStatusHelper.unwindCoreException((CoreException) t);
		}
		if (t instanceof IOException && attemptCounter < connectionRetryCount - 1) {
			LogHelper
					.log(new Status(IStatus.WARNING, Activator.ID,
							NLS.bind(Messages.connection_to_0_failed_on_1_retry_attempt_2,
									uri.toString(), t.getMessage(), String.valueOf(attemptCounter)),
							t));
			try {
				Thread.sleep(connectionRetryDelay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new OperationCanceledException();
			}
		}
		throw RepositoryStatusHelper.wrap(exception);
	}
}
