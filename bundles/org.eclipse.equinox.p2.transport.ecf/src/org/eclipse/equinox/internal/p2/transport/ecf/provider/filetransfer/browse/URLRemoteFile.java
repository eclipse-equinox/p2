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

package org.eclipse.equinox.internal.p2.transport.ecf.provider.filetransfer.browse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFile;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileAttributes;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileInfo;

/**
 *
 */
public class URLRemoteFile implements IRemoteFile {

	URI fileID;

	long lastModified;
	long fileLength;
	IRemoteFileAttributes remoteFileAttributes;

	public URLRemoteFile(long lastModified, long fileLength, URI fileID) {
		this.lastModified = lastModified;
		this.fileLength = fileLength;
		Assert.isNotNull(fileID);
		this.fileID = fileID;
		remoteFileAttributes = new URLRemoteFileAttributes();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFile#getInfo()
	 */
	public IRemoteFileInfo getInfo() {
		return new IRemoteFileInfo() {

			public IRemoteFileAttributes getAttributes() {
				return remoteFileAttributes;
			}

			public long getLastModified() {
				return lastModified;
			}

			public long getLength() {
				return fileLength;
			}

			public String getName() {
				URL url;
				String result = null;
				try {
					url = fileID.toURL();
					String path = url.getPath();
					int index = path.lastIndexOf("/"); //$NON-NLS-1$
					if (index == -1)
						return path;
					result = path.substring(index + 1);
					return result;
				} catch (MalformedURLException | IllegalArgumentException e) {
					return fileID.toString();
				}
			}

			public boolean isDirectory() {
				try {
					return fileID.toURL().toString().endsWith("/"); //$NON-NLS-1$
				} catch (MalformedURLException | IllegalArgumentException e) {
					return false;
				}
			}

			public void setAttributes(IRemoteFileAttributes attributes) {
				// Not supported
			}

			public void setLastModified(long time) {
				// not supported
			}

			public void setName(String name) {
				// not supported
			}
		};
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder("URLRemoteFile["); //$NON-NLS-1$
		buf.append("id=").append(fileID).append(";"); //$NON-NLS-1$//$NON-NLS-2$
		buf.append("name=").append(getInfo().getName()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("isDir=").append(getInfo().isDirectory()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("length=").append(getInfo().getLength()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("lastMod=").append(getInfo().getLastModified()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("attr=").append(getInfo().getAttributes()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		return buf.toString();
	}
}
