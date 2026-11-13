/****************************************************************************
 * Copyright (c) 2004 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.filetransfer;

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ecf.filetransfer.events.IFileTransferRequestEvent;

/**
 * File transfer information delivered to
 * {@link IIncomingFileTransferRequestListener} via an event implementing
 * {@link IFileTransferRequestEvent#getFileTransferInfo()}
 * 
 */
public interface IFileTransferInfo extends IAdaptable {
	/**
	 * Get the file for the proposed file transfer
	 * 
	 * @return the proposed file. Will not return <code>null</code>.
	 */
	public File getFile();

	/**
	 * Get the file size (in bytes).
	 * 
	 * @return long file size (in bytes).  If file size is unknown, will return -1.
	 */
	public long getFileSize();

	/**
	 * Get any properties associated with this file transfer. The map keys and
	 * values are assumed to be Strings.
	 * 
	 * @return Map of properties associated with this file transfer info. Will
	 *         not return <code>null</code>.
	 */
	public Map getProperties();

	/**
	 * Get any description associated with this file transfer info.
	 * 
	 * @return String description. May be <code>null</code> if no description
	 *         provided.
	 */
	public String getDescription();

	/**
	 * Get the mime type string for this file transfer info.
	 * 
	 * @return String mime type.  May return <code>null</code> if mime type is not known.
	 */
	public String getMimeType();
}
