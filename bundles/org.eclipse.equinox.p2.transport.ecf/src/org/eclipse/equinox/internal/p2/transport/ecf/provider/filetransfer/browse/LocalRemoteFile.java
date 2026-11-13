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

package org.eclipse.equinox.internal.p2.transport.ecf.provider.filetransfer.browse;

import java.io.File;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFile;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileAttributes;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileInfo;
import org.eclipse.equinox.internal.p2.transport.ecf.internal.provider.filetransfer.Activator;

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

			IRemoteFileAttributes attributes = new LocalRemoteFileAttributes(LocalRemoteFile.this.file);

			public IRemoteFileAttributes getAttributes() {
				return attributes;
			}

			public long getLastModified() {
				return LocalRemoteFile.this.file.lastModified();
			}

			public long getLength() {
				return LocalRemoteFile.this.file.length();
			}

			public String getName() {
				return LocalRemoteFile.this.file.getName();
			}

			public boolean isDirectory() {
				return LocalRemoteFile.this.file.isDirectory();
			}

			public void setAttributes(IRemoteFileAttributes attributes) {
				// can't set attributes
			}

			public void setLastModified(long time) {
				// can't set post hoc
			}

			public void setName(String name) {
				// Can't modify post hoc
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFile#getInfo()
	 */
	public IRemoteFileInfo getInfo() {
		return info;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public <T> T getAdapter(Class<T> adapter) {
		IAdapterManager adapterManager = Activator.getDefault().getAdapterManager();
		if (adapterManager == null)
			return null;
		return (T) adapterManager.loadAdapter(this, adapter.getName());
	}

	public String toString() {
		StringBuilder buf = new StringBuilder("LocalRemoteFile["); //$NON-NLS-1$
		buf.append("file=").append(file).append(";"); //$NON-NLS-1$//$NON-NLS-2$
		buf.append("name=").append(getInfo().getName()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("isDir=").append(getInfo().isDirectory()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("length=").append(getInfo().getLength()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("lastMod=").append(getInfo().getLastModified()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("attr=").append(getInfo().getAttributes()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		return buf.toString();
	}
}
