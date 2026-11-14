/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
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

package org.eclipse.ecf.core.util;

import java.io.Serializable;

/**
 * Class to represent an proxy address
 */
public class ProxyAddress implements Serializable {

	private static final long serialVersionUID = 9076207407726734246L;

	protected static final int DEFAULT_PORT = -1;

	protected int port = -1;
	protected String hostname = ""; //$NON-NLS-1$

	public ProxyAddress(String hostname, int port) {
		this.hostname = (hostname == null) ? "" : hostname; //$NON-NLS-1$
		this.port = port;
	}

	public ProxyAddress(String hostname) {
		this(hostname, DEFAULT_PORT);
	}

	public String getHostName() {
		return this.hostname;
	}

	public int getPort() {
		return this.port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == null || this.hostname == null)
			return false;
		if (!(obj instanceof ProxyAddress))
			return false;
		ProxyAddress other = (ProxyAddress) obj;
		return (this.hostname.equals(other.hostname) && this.port == other.port);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (this.hostname == null)
			return super.hashCode();
		return this.hostname.hashCode() ^ Math.abs(this.port);
	}
}
