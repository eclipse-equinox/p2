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

import java.util.Locale;
import java.util.Objects;

public class AuthScope {

	private final String host;
	private final int port;

	public AuthScope(final String host, final int port) {
		this.host = host.toLowerCase(Locale.ROOT);
		this.port = port;
	}

	@Override
	public int hashCode() {
		return Objects.hash(host, port);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthScope other = (AuthScope) obj;
		return Objects.equals(host, other.host) && port == other.port;
	}

	public String getSchemeName() {
		return null;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

}