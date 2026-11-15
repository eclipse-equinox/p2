/****************************************************************************
 * Copyright (c) 2008 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.filetransfer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * Runnable for doing file transfer.  Used by {@link FileTransferJob}s.
 * 
 * @since 2.0
 */
public interface IFileTransferRunnable {

	/**
	 * Synchronously perform the actual file transfer.
	 * 
	 * @param monitor a progress montior.  Will not be <code>null</code>.
	 * @return IStatus a status object indicating the ending status of the file transfer job.
	 */
	public IStatus performFileTransfer(IProgressMonitor monitor);
}
