/*******************************************************************************
 * Copyright (c) 2006-2009, IBM Corporation and other.
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

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.security.ConnectContextFactory;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.UserCancelledException;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI.AuthenticationInfo;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.osgi.util.NLS;

/**
 * RepositoryTransport adapts p2 to ECF file download and file browsing.
 * Download is performed by {@link FileReader}, and file browsing is performed by
 * {@link FileInfoReader}.
 * 
 * @author henrik.lindberg@cloudsmith.com
 *
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
	 * @deprecated - use {@link #download(URI, OutputStream, IProgressMonitor) }
	 * @returns status with {@link URISyntaxException} on malformed <code>toDownload</code>
	 */
	public IStatus download(String toDownload, OutputStream target, IProgressMonitor monitor) {
		// It is not meaningful to continue if the toDownload string is not a valid URI
		// so deal with this here immediately instead of getting deep exceptions
		//
		URI uri = null;
		try {
			uri = new URI(toDownload); // URIUtil.fromString(toDownload);

		} catch (URISyntaxException e1) {
			return RepositoryStatusHelper.malformedAddressStatus(toDownload, e1);
		}
		return download(uri, target, monitor);
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
	 */
	public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
		boolean promptUser = false;
		AuthenticationInfo loginDetails = null;
		for (int i = RepositoryPreferences.getLoginRetryCount(); i > 0; i++) {
			try {
				loginDetails = Credentials.forLocation(toDownload, promptUser, loginDetails);
				IConnectContext context = (loginDetails == null) ? null : ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());

				// perform the download
				FileReader reader = new FileReader(context);
				reader.readInto(toDownload, target, monitor);
				// Download status is expected on success
				DownloadStatus status = new DownloadStatus(IStatus.OK, Activator.ID, Status.OK_STATUS.getMessage());
				status.setTransferRate(reader.getLastFileInfo().getAverageSpeed());
				return statusOn(target, status);
			} catch (UserCancelledException e) {
				return statusOn(target, Status.CANCEL_STATUS);
			} catch (CoreException e) {
				return statusOn(target, RepositoryStatus.forStatus(e.getStatus(), toDownload));
			} catch (FileNotFoundException e) {
				return statusOn(target, RepositoryStatus.forException(e, toDownload));
			} catch (AuthenticationFailedException e) {
				promptUser = true;
			}
		}
		// reached maximum number of retries without success
		IStatus status = new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, //
				NLS.bind(Messages.UnableToRead_0_TooManyAttempts, toDownload), null);
		return statusOn(target, status);
	}

	private static IStatus statusOn(OutputStream target, IStatus status) {
		if (target instanceof IStateful)
			((IStateful) target).setStatus(status);
		return status;
	}

	/**
	 * Returns the last modified date for a URI, or 0 on any error.
	 * (If more control over errors is needed, use {@link FileInfoReader#getRemoteFiles(URI, IProgressMonitor)}).
	 * @param toDownload
	 * @param monitor
	 * @return last modified date or 0 on any error
	 */
	public long getLastModified(URI toDownload, IProgressMonitor monitor) throws UserCancelledException, CoreException, FileNotFoundException, AuthenticationFailedException {
		boolean promptUser = false;
		AuthenticationInfo loginDetails = null;
		for (int i = RepositoryPreferences.getLoginRetryCount(); i > 0; i++) {
			try {
				loginDetails = Credentials.forLocation(toDownload, promptUser, loginDetails);
				IConnectContext context = (loginDetails == null) ? null : ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());

				// get the remote info
				FileInfoReader reader = new FileInfoReader(context);
				return reader.getLastModified(toDownload, monitor);
				//			} catch (UserCancelledException e) {
				//				return 0;
			} catch (CoreException e) {
				// must translate this core exception as it is most likely not informative to a user
				throw new CoreException(RepositoryStatus.forStatus(e.getStatus(), toDownload));
				//			} catch (FileNotFoundException e) {
				//				return 0;
			} catch (AuthenticationFailedException e) {
				promptUser = true;
			}
		}
		// reached maximum number of authentication retries without success
		// return 0;
		throw new AuthenticationFailedException();
	}

}
