/****************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.internal.provider.filetransfer.httpclientjava;

public class HttpHost {

	@SuppressWarnings("unused")
	private String hostName;
	@SuppressWarnings("unused")
	private int port;

	public HttpHost(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}

}
