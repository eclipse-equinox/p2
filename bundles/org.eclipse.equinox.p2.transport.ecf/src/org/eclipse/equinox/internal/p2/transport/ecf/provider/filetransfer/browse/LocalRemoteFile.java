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

import java.io.File;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFile;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileInfo;

/**
 * Local representation of an {@link IRemoteFile}.
 */
public class LocalRemoteFile implements IRemoteFile {

	File file = null;

	IRemoteFileInfo info;

	/**
	 * @param file the file
	 */
	public LocalRemoteFile(File file) {
		this.file = file;
		Assert.isNotNull(file);
		this.info = new IRemoteFileInfo() {

			public long getLastModified() {
				return LocalRemoteFile.this.file.lastModified();
			}

			public long getLength() {
				return LocalRemoteFile.this.file.length();
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFile#getInfo()
	 */
	public IRemoteFileInfo getInfo() {
		return info;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder("LocalRemoteFile["); //$NON-NLS-1$
		buf.append("file=").append(file).append(";"); //$NON-NLS-1$//$NON-NLS-2$
		buf.append("length=").append(getInfo().getLength()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("lastMod=").append(getInfo().getLastModified()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		return buf.toString();
	}
}
