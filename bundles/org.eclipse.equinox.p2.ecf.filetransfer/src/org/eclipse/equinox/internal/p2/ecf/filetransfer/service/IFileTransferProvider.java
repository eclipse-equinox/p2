/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ecf.filetransfer.service;

import java.util.Set;

/**
 * A file transfer provider (e.g. an HTTP client implementation) registered as
 * an OSGi service. For each protocol the provider with the highest priority
 * (the smallest number) is used, unless the system property
 * <code>org.eclipse.equinox.p2.ecf.filetransfer.httpProvider</code> names the id of
 * the provider to prefer.
 */
public interface IFileTransferProvider {

	/**
	 * @return the unique id of the provider, e.g. <code>httpclientjava</code>
	 */
	String getId();

	/**
	 * @return the priority of the provider, the smaller the number the higher the
	 *         priority
	 */
	int getPriority();

	/**
	 * @return the protocols (URI schemes) supported by the provider
	 */
	Set<String> getProtocols();

	/**
	 * @return a new retrieve file transfer instance
	 */
	IRetrieveFileTransfer newRetrieveFileTransfer();

	/**
	 * @return a new remote file system browser instance
	 */
	IRemoteFileSystemBrowser newRemoteFileSystemBrowser();
}
