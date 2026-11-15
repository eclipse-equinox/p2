/****************************************************************************
 * Copyright (c) 2007, 2009 Composent, Inc., IBM and others.
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

package org.eclipse.ecf.filetransfer.events;

import org.eclipse.ecf.filetransfer.IRemoteFile;

/**
 * Event that indicates that a directory list is available via {@link #getRemoteFiles()}.
 * <p>
 * This event is fired even if the browse failed or has been canceled. In this case
 * the cause is provided via {@link #getException()}.
 * </p> 
 */
public interface IRemoteFileSystemBrowseEvent extends IRemoteFileSystemEvent {

	/**
	 * Get the list of files associated with this browse event.  If the original
	 * file ID available via {@link IRemoteFileSystemEvent#getFileID()} is a regular
	 * file, the array will be of length 1.  If a directory, will be of length n.
	 * Is null if the associated IRemoteFileSystemRequest failed or was canceled.
	 * The cause will be available in {@link #getException()}. 
	 * @return IRemoteFile[] the array of remote files for the given browse.  If the original
	 * file ID available via {@link IRemoteFileSystemEvent#getFileID()} is a regular
	 * file, the array will be of length 1.  If a directory, will be of length n.
	 * May be null. 
	 */
	public IRemoteFile[] getRemoteFiles();

}
