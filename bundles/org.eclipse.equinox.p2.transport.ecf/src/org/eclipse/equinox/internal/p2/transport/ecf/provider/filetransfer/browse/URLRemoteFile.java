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

package org.eclipse.equinox.internal.p2.transport.ecf.provider.filetransfer.browse;

import java.net.URI;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFile;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileInfo;

/**
 *
 */
public class URLRemoteFile implements IRemoteFile {

	URI fileID;

	long lastModified;
	long fileLength;

	public URLRemoteFile(long lastModified, long fileLength, URI fileID) {
		this.lastModified = lastModified;
		this.fileLength = fileLength;
		Assert.isNotNull(fileID);
		this.fileID = fileID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFile#getInfo()
	 */
	public IRemoteFileInfo getInfo() {
		return new IRemoteFileInfo() {

			public long getLastModified() {
				return lastModified;
			}

			public long getLength() {
				return fileLength;
			}
		};
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder("URLRemoteFile["); //$NON-NLS-1$
		buf.append("id=").append(fileID).append(";"); //$NON-NLS-1$//$NON-NLS-2$
		buf.append("length=").append(getInfo().getLength()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("lastMod=").append(getInfo().getLastModified()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		return buf.toString();
	}
}
