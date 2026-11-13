/****************************************************************************
 * Copyright (c) 2007 Composent, Inc. and others.
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

/**
 * Information about a remote file.  Last modified day/time, length in bytes,
 * whether the remote file is a directory, a name, and file attributes.
 * @see IRemoteFile
 */
public interface IRemoteFileInfo {

	public static final int NO_LENGTH = -1;

	/**
	 * @deprecated
	 */
	public static final int NONE = NO_LENGTH;

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

	/**
	 * Returns whether this file is a directory, or <code>false</code> if this
	 * file does not exist.
	 * 
	 * @return <code>true</code> if this file is a directory, and <code>false</code>
	 * otherwise.
	 */
	public boolean isDirectory();

	/**
	 * Returns the name of this file.
	 * 
	 * @return the name of this file.  Will not return <code>null</code>.
	 */
	public String getName();

	/**
	 * Get remote file attributes.
	 * @return IRemoteFileAttributes for this IRemoteFile.  Will not return <code>null</code>.
	 */
	public IRemoteFileAttributes getAttributes();

	/**
	 * Set the attributes for this remote file info.
	 * @param attributes the new attribute values to use.
	 */
	public void setAttributes(IRemoteFileAttributes attributes);

	/**
	 * Set the underlying name for this remote file info.
	 * 
	 * @param name the new name to use.  Must not be <code>null</code>.
	 */
	public void setName(String name);

	/**
	 * Set the last modified time for this remote file info.
	 * 
	 * @param time the time to use.  See {@link #getLastModified()} for meaning of time value.
	 */
	public void setLastModified(long time);

}
