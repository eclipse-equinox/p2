/****************************************************************************
 * Copyright (c) 2007, 2026 Composent, Inc. and others.
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

package org.eclipse.equinox.p2.transport.ecf.filetransfer;

/**
 * Information about a remote file: last modified day/time and length in bytes.
 * @see IRemoteFile
 */
public interface IRemoteFileInfo {

	public static final int NO_LENGTH = -1;

	public static final int NO_LAST_MODIFIED = 0;

	/**
	 * Returns the last modified time for this file, or {@link #NO_LAST_MODIFIED }
	 * if the file does not exist or the last modified time could not be computed.
	 * <p>
	 * The time is represented as the number of Universal Time (UT) 
	 * milliseconds since the epoch (00:00:00 GMT, January 1, 1970).
	 * </p>
	 * 
	 * @return the last modified time for this file, or {@link #NO_LAST_MODIFIED } if file 
	 * does not exist or last modified not known or could not be computed.
	 */
	public long getLastModified();

	/**
	 * Returns the length of this file, or {@link #NO_LENGTH}
	 * if the file does not exist, is a directory, or the length could not be computed.
	 * 
	 * @return the length of this file, or {@link #NO_LENGTH}
	 */
	public long getLength();
}
