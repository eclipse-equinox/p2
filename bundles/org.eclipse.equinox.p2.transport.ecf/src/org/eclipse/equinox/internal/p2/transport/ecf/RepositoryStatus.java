/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.transport.ecf;

import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.p2.core.ProvisionException;

import java.io.FileNotFoundException;
import java.net.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.identity.IDCreateException;
import org.eclipse.ecf.filetransfer.BrowseFileTransferException;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.osgi.util.NLS;

/**
 * Utility class to transform transport errors into error messages.
 *
 */
public class RepositoryStatus {

	public static String codeToMessage(int code, String toDownload) {
		switch (code) {
			case 400 :
				return NLS.bind(Messages.TransportErrorTranslator_400, toDownload);
			case 401 :
				return NLS.bind(Messages.TransportErrorTranslator_401, toDownload);
			case 402 :
				return NLS.bind(Messages.TransportErrorTranslator_402, toDownload);
			case 403 :
				return NLS.bind(Messages.TransportErrorTranslator_403, toDownload);
			case 404 :
				return NLS.bind(Messages.TransportErrorTranslator_404, toDownload);
			case 405 :
				return NLS.bind(Messages.TransportErrorTranslator_405, toDownload);
			case 406 :
				return NLS.bind(Messages.TransportErrorTranslator_406, toDownload);
			case 407 :
				return NLS.bind(Messages.TransportErrorTranslator_407, toDownload);
			case 408 :
				return NLS.bind(Messages.TransportErrorTranslator_408, toDownload);
			case 409 :
				return NLS.bind(Messages.TransportErrorTranslator_409, toDownload);
			case 410 :
				return NLS.bind(Messages.TransportErrorTranslator_410, toDownload);
			case 411 :
				return NLS.bind(Messages.TransportErrorTranslator_411, toDownload);
			case 412 :
				return NLS.bind(Messages.TransportErrorTranslator_412, toDownload);
			case 413 :
				return NLS.bind(Messages.TransportErrorTranslator_413, toDownload);
			case 414 :
				return NLS.bind(Messages.TransportErrorTranslator_414, toDownload);
			case 415 :
				return NLS.bind(Messages.TransportErrorTranslator_415, toDownload);
			case 416 :
				return NLS.bind(Messages.TransportErrorTranslator_416, toDownload);
			case 417 :
				return NLS.bind(Messages.TransportErrorTranslator_417, toDownload);
			case 418 :
				return NLS.bind(Messages.TransportErrorTranslator_418, toDownload);
			case 422 :
				return NLS.bind(Messages.TransportErrorTranslator_422, toDownload);
			case 423 :
				return NLS.bind(Messages.TransportErrorTranslator_423, toDownload);
			case 424 :
				return NLS.bind(Messages.TransportErrorTranslator_424, toDownload);
			case 425 :
				return NLS.bind(Messages.TransportErrorTranslator_425, toDownload);
			case 426 :
				return NLS.bind(Messages.TransportErrorTranslator_426, toDownload);
			case 449 :
				return NLS.bind(Messages.TransportErrorTranslator_449, toDownload);
			case 450 :
				return NLS.bind(Messages.TransportErrorTranslator_450, toDownload);

			case 500 :
				return NLS.bind(Messages.TransportErrorTranslator_500, toDownload);
			case 501 :
				return NLS.bind(Messages.TransportErrorTranslator_501, toDownload);
			case 502 :
				return NLS.bind(Messages.TransportErrorTranslator_502, toDownload);
			case 503 :
				return NLS.bind(Messages.TransportErrorTranslator_503, toDownload);
			case 504 :
				return NLS.bind(Messages.TransportErrorTranslator_504, toDownload);
			case 505 :
				return NLS.bind(Messages.TransportErrorTranslator_505, toDownload);
			case 506 :
				return NLS.bind(Messages.TransportErrorTranslator_506, toDownload);
			case 507 :
				return NLS.bind(Messages.TransportErrorTranslator_507, toDownload);
			case 508 :
				return NLS.bind(Messages.TransportErrorTranslator_508, toDownload);
			case 510 :
				return NLS.bind(Messages.TransportErrorTranslator_510, toDownload);

			default :
				return NLS.bind(Messages.TransportErrorTranslator_UnknownErrorCode, Integer.toString(code), toDownload);
		}
	}

	public static DownloadStatus forStatus(IStatus original, URI toDownload) {
		Throwable t = original.getException();
		return forException(t, toDownload);
	}

	public static DownloadStatus forException(Throwable t, URI toDownload) {
		if (t instanceof FileNotFoundException || (t instanceof IncomingFileTransferException && ((IncomingFileTransferException) t).getErrorCode() == 404))
			return new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.ARTIFACT_NOT_FOUND, NLS.bind(Messages.artifact_not_found, toDownload), t);
		if (t instanceof ConnectException)
			return new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, NLS.bind(Messages.TransportErrorTranslator_UnableToConnectToRepository_0, toDownload), t);
		if (t instanceof UnknownHostException)
			return new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, NLS.bind(Messages.TransportErrorTranslator_UnknownHost, toDownload), t);
		if (t instanceof IDCreateException) {
			IStatus status = ((IDCreateException) t).getStatus();
			if (status != null && status.getException() != null)
				t = status.getException();

			return new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, NLS.bind(Messages.TransportErrorTranslator_MalformedRemoteFileReference, toDownload), t);
		}
		int code = 0;

		// default to report as read repository error
		int provisionCode = ProvisionException.REPOSITORY_FAILED_READ;

		if (t instanceof IncomingFileTransferException)
			code = ((IncomingFileTransferException) t).getErrorCode();
		else if (t instanceof BrowseFileTransferException)
			code = ((BrowseFileTransferException) t).getErrorCode();

		// Switch on error codes in the HTTP error code range. 
		// Note that 404 uses ARTIFACT_NOT_FOUND (as opposed to REPOSITORY_NOT_FOUND, which
		// is determined higher up in the calling chain).
		if (code == 401)
			provisionCode = ProvisionException.REPOSITORY_FAILED_AUTHENTICATION;
		else if (code == 404)
			provisionCode = ProvisionException.ARTIFACT_NOT_FOUND;

		// Add more specific translation here

		return new DownloadStatus(IStatus.ERROR, Activator.ID, provisionCode, //
				code == 0 ? NLS.bind(Messages.io_failedRead, toDownload) //
						: codeToMessage(code, toDownload.toString()), t);
	}
}
