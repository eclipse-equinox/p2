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

package org.eclipse.equinox.p2.transport.ecf.filetransfer;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Remote file request.
 */
public interface IRemoteFileSystemRequest extends IAdaptable {

	/**
	 * Cancel this request.
	 */
	public void cancel();

	/**
	 * Get the listener associated with this request
	 * @return IRemoteFileSystemListener associated with this request.
	 */
	public IRemoteFileSystemListener getRemoteFileListener();
}
