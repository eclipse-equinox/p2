/*******************************************************************************
 * Copyright (c) 2006, 2010, IBM Corporation and other.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 * 
 * Contributors
 * 	IBM Corporation - Initial API and implementation.
 *  Cloudsmith Inc - Implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.repository;

import java.io.*;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.security.ConnectContextFactory;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.UserCancelledException;
import org.eclipse.equinox.internal.p2.repository.Credentials.LoginCanceledException;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.UIServices.AuthenticationInfo;
import org.eclipse.osgi.util.NLS;

/**
 * RepositoryTransport adapts p2 to ECF file download and file browsing.
 * Download is performed by {@link FileReader}, and file browsing is performed by
 * {@link FileInfoReader}.
 */
public class RepositoryTransport extends Transport {
	private static RepositoryTransport instance;

	/**
	 * Returns an shared instance of Generic Transport
	 */
	public static synchronized RepositoryTransport getInstance() {
		if (instance == null) {
			instance = new RepositoryTransport();
		}
		return instance;
	}

	/**
	 * Perform a download, writing into the target output stream. Progress is reported on the
	 * monitor. If the <code>target</code> is an instance of {@link IStateful} the resulting status
	 * is also set on the target. An IStateful target is updated with status even if this methods
	 * throws {@link OperationCanceledException}.
	 * 
	 * @returns IStatus, that is a {@link DownloadStatus} on success.
	 * @param toDownload URI of file to download
	 * @param target OutputStream where result is written
	 * @param startPos the starting position of the download, or -1 for from start
	 * @param monitor where progress should be reported
	 * @throws OperationCanceledException if the operation was canceled.
	 */
	public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {

		boolean promptUser = false;
		boolean useJREHttp = false;
		AuthenticationInfo loginDetails = null;
		for (int i = RepositoryPreferences.getLoginRetryCount(); i > 0; i--) {
			FileReader reader = null;
			try {
				loginDetails = Credentials.forLocation(toDownload, promptUser, loginDetails);
				IConnectContext context = (loginDetails == null) ? null : ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());

				// perform the download
				reader = new FileReader(context);
				reader.readInto(toDownload, target, startPos, monitor);

				// check that job ended ok - throw exceptions otherwise
				IStatus result = reader.getResult();
				if (result == null) {
					String msg = NLS.bind(Messages.RepositoryTransport_failedReadRepo, toDownload);
					DownloadStatus ds = new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, null);
					return statusOn(target, ds, reader);
				}
				if (result.getSeverity() == IStatus.CANCEL)
					throw new UserCancelledException();
				if (!result.isOK())
					throw new CoreException(result);

				// Download status is expected on success
				DownloadStatus status = new DownloadStatus(IStatus.OK, Activator.ID, Status.OK_STATUS.getMessage());
				return statusOn(target, status, reader);
			} catch (UserCancelledException e) {
				statusOn(target, new DownloadStatus(IStatus.CANCEL, Activator.ID, 1, "", null), reader); //$NON-NLS-1$
				throw new OperationCanceledException();
			} catch (OperationCanceledException e) {
				statusOn(target, new DownloadStatus(IStatus.CANCEL, Activator.ID, 1, "", null), reader); //$NON-NLS-1$
				throw e;
			} catch (CoreException e) {
				if (e.getStatus().getException() == null)
					return statusOn(target, RepositoryStatus.forException(e, toDownload), reader);
				return statusOn(target, RepositoryStatus.forStatus(e.getStatus(), toDownload), reader);
			} catch (FileNotFoundException e) {
				return statusOn(target, RepositoryStatus.forException(e, toDownload), reader);
			} catch (AuthenticationFailedException e) {
				promptUser = true;
			} catch (Credentials.LoginCanceledException e) {
				DownloadStatus status = new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, //
						NLS.bind(Messages.UnableToRead_0_UserCanceled, toDownload), null);
				return statusOn(target, status, null);
			} catch (JREHttpClientRequiredException e) {
				if (!useJREHttp) {
					useJREHttp = true; // only do this once
					i++; // need an extra retry
					Activator.getDefault().useJREHttpClient();
				}
			}
		}
		// reached maximum number of retries without success
		DownloadStatus status = new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, //
				NLS.bind(Messages.UnableToRead_0_TooManyAttempts, toDownload), null);
		return statusOn(target, status, null);
	}

	/**
	 * Perform a download, writing into the target output stream. Progress is reported on the
	 * monitor. If the <code>target</code> is an instance of {@link IStateful} the resulting status
	 * is also set on the target.
	 * 
	 * @returns IStatus, that is a {@link DownloadStatus} on success.
	 * @param toDownload URI of file to download
	 * @param target OutputStream where result is written
	 * @param monitor where progress should be reported
	 * @throws OperationCanceledException if the operation was canceled.
	 */
	public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
		return download(toDownload, target, -1, monitor);
	}

	/**
	 * Perform a stream download, writing into an InputStream that is returned. Performs authentication if needed.
	 * 
	 * @returns InputStream a stream with the content from the toDownload URI, or null
	 * @param toDownload URI of file to download
	 * @param monitor monitor checked for cancellation
	 * @throws OperationCanceledException if the operation was canceled.
	 * @throws AuthenticationFailedException if authentication failed, or too many attempt were made
	 * @throws FileNotFoundException if the toDownload was reported as non existing
	 * @throws CoreException on errors
	 */
	public InputStream stream(URI toDownload, IProgressMonitor monitor) throws FileNotFoundException, CoreException, AuthenticationFailedException {

		boolean promptUser = false;
		boolean useJREHttp = false;
		AuthenticationInfo loginDetails = null;
		for (int i = RepositoryPreferences.getLoginRetryCount(); i > 0; i--) {
			FileReader reader = null;
			try {
				loginDetails = Credentials.forLocation(toDownload, promptUser, loginDetails);
				IConnectContext context = (loginDetails == null) ? null : ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());

				// perform the streamed download
				reader = new FileReader(context);
				return reader.read(toDownload, monitor);
			} catch (UserCancelledException e) {
				throw new OperationCanceledException();
			} catch (AuthenticationFailedException e) {
				promptUser = true;
			} catch (CoreException e) {
				// must translate this core exception as it is most likely not informative to a user
				if (e.getStatus().getException() == null)
					throw new CoreException(RepositoryStatus.forException(e, toDownload));
				throw new CoreException(RepositoryStatus.forStatus(e.getStatus(), toDownload));
			} catch (LoginCanceledException e) {
				// i.e. same behavior when user cancels as when failing n attempts.
				throw new AuthenticationFailedException();
			} catch (JREHttpClientRequiredException e) {
				if (!useJREHttp) {
					useJREHttp = true; // only do this once
					i++; // need an extra retry
					Activator.getDefault().useJREHttpClient();
				}
			}
		}
		throw new AuthenticationFailedException();
	}

	/**
	 * Set the status on the output stream if it implements IStateful. 
	 * Update the DownloadStatus with information from FileReader.
	 * @param target an OutputStream possibly implementing IStateful
	 * @param status a DownloadStatus configured with status message, code, etc
	 * @param reader a FileReade that was used to download (or null if not known).
	 * @throws OperationCanceledException if the operation was canceled by the user.
	 * @return the configured DownloadStatus status.
	 */
	private static DownloadStatus statusOn(OutputStream target, DownloadStatus status, FileReader reader) {
		if (reader != null) {
			FileInfo fi = reader.getLastFileInfo();
			if (fi != null) {
				status.setFileSize(fi.getSize());
				status.setLastModified(fi.getLastModified());
				status.setTransferRate(fi.getAverageSpeed());
			}
		}
		if (target instanceof IStateful)
			((IStateful) target).setStatus(status);
		return status;
	}

	/**
	 * Returns the last modified date for a URI. A last modified of 0 typically indicates that
	 * the server response is wrong, but should not be interpreted as a file not found.
	 * @param toDownload
	 * @param monitor
	 * @throws OperationCanceledException if the operation was canceled by the user.
	 * @return last modified date (possibly 0)
	 */
	public long getLastModified(URI toDownload, IProgressMonitor monitor) throws CoreException, FileNotFoundException, AuthenticationFailedException {
		boolean promptUser = false;
		boolean useJREHttp = false;
		AuthenticationInfo loginDetails = null;
		for (int i = RepositoryPreferences.getLoginRetryCount(); i > 0; i--) {
			try {
				loginDetails = Credentials.forLocation(toDownload, promptUser, loginDetails);
				IConnectContext context = (loginDetails == null) ? null : ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());
				// get the remote info
				FileInfoReader reader = new FileInfoReader(context);
				return reader.getLastModified(toDownload, monitor);
			} catch (UserCancelledException e) {
				throw new OperationCanceledException();
			} catch (CoreException e) {
				// must translate this core exception as it is most likely not informative to a user
				if (e.getStatus().getException() == null)
					throw new CoreException(RepositoryStatus.forException(e, toDownload));
				throw new CoreException(RepositoryStatus.forStatus(e.getStatus(), toDownload));
			} catch (AuthenticationFailedException e) {
				promptUser = true;
			} catch (LoginCanceledException e) {
				// same behavior as if user failed n attempts.
				throw new AuthenticationFailedException();
			} catch (JREHttpClientRequiredException e) {
				if (!useJREHttp) {
					useJREHttp = true; // only do this once
					i++; // need an extra retry
					Activator.getDefault().useJREHttpClient();
				}
			}

		}
		// reached maximum number of authentication retries without success
		throw new AuthenticationFailedException();
	}

}
