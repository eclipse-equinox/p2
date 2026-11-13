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

package org.eclipse.ecf.provider.filetransfer.browse;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.eclipse.ecf.filetransfer.IRemoteFileAttributes;

/**
 * File attributes for {@link LocalRemoteFile} instances.
 */
public class LocalRemoteFileAttributes implements IRemoteFileAttributes {

	File file = null;

	static String[] fileAttributes = {IRemoteFileAttributes.READ_ATTRIBUTE, IRemoteFileAttributes.WRITE_ATTRIBUTE, IRemoteFileAttributes.HIDDEN_ATTRIBUTE, IRemoteFileAttributes.EXEC_ATTRIBUTE, IRemoteFileAttributes.ARCHIVE_ATTRIBUTE};
	static List attributeKeys = new ArrayList(Arrays.asList(fileAttributes));

	public LocalRemoteFileAttributes(File file) {
		this.file = file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRemoteFileAttributes#getAttribute(java.lang.String)
	 */
	public String getAttribute(String key) {
		if (key == null)
			return null;
		if (key.equals(IRemoteFileAttributes.READ_ATTRIBUTE)) {
			if (file.canRead())
				return Boolean.TRUE.toString();
		} else if (key.equals(IRemoteFileAttributes.WRITE_ATTRIBUTE)) {
			if (file.canWrite())
				return Boolean.TRUE.toString();
		} else if (key.equals(IRemoteFileAttributes.HIDDEN_ATTRIBUTE)) {
			if (file.isHidden())
				return Boolean.TRUE.toString();
		} else if (key.equals(IRemoteFileAttributes.EXEC_ATTRIBUTE)) {
			return Boolean.TRUE.toString();
		} else if (key.equals(IRemoteFileAttributes.ARCHIVE_ATTRIBUTE)) {
			return Boolean.TRUE.toString();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRemoteFileAttributes#getAttributeKeys()
	 */
	public Iterator getAttributeKeys() {
		return attributeKeys.iterator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRemoteFileAttributes#setAttribute(java.lang.String, java.lang.String)
	 */
	public void setAttribute(String key, String value) {
		// not supported
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder("LocalRemoteFileAttributes["); //$NON-NLS-1$
		for (Iterator i = getAttributeKeys(); i.hasNext();) {
			String key = (String) i.next();
			buf.append(key).append("=").append(getAttribute(key)); //$NON-NLS-1$
			buf.append(i.hasNext() ? ";" : "]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return buf.toString();
	}
}
