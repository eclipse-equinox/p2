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

import java.io.ObjectStreamException;
import java.io.Serializable;

public class Proxy implements Serializable {

	private static final long serialVersionUID = -2481483596779542834L;

	public static class Type implements Serializable {

		private static final long serialVersionUID = 361071073081975271L;

		private static final String DIRECT_NAME = "direct"; //$NON-NLS-1$

		private static final String HTTP_NAME = "http"; //$NON-NLS-1$

		private static final String SOCKS_NAME = "socks"; //$NON-NLS-1$

		private final transient String name;

		protected Type(String name) {
			this.name = name;
		}

		public static final Type DIRECT = new Type(DIRECT_NAME);

		public static final Type HTTP = new Type(HTTP_NAME);

		public static final Type SOCKS = new Type(SOCKS_NAME);

		public static Type fromString(String type) {
			if (type == null)
				return null;
			else if (type.equals(DIRECT_NAME))
				return DIRECT;
			else if (type.equals(HTTP_NAME))
				return HTTP;
			else if (type.equals(SOCKS_NAME))
				return SOCKS;
			else
				return null;
		}

		public String toString() {
			return name;
		}

		// For serialization
		private static int nextOrdinal = 0;

		private final int ordinal = nextOrdinal++;

		private static final Type[] VALUES = {DIRECT, HTTP, SOCKS};

		/**
		 * @return Object
		 * @throws ObjectStreamException not thrown by this implementation.
		 */
		Object readResolve() throws ObjectStreamException {
			return VALUES[ordinal];
		}
	}

	ProxyAddress address;

	Type type;

	String username;

	String password;

	public final static Proxy NO_PROXY = new Proxy();

	private Proxy() {
		this.type = Type.DIRECT;
		this.address = null;
	}

	public Proxy(Type type, ProxyAddress address, String username, String password) {
		this.type = type;
		this.address = address;
		this.username = username;
		this.password = password;
	}

	public Proxy(Type type, ProxyAddress address) {
		this(type, address, null, null);
	}

	public Type getType() {
		return type;
	}

	public ProxyAddress getAddress() {
		return address;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public boolean hasCredentials() {
		return (username != null && !username.equals("")); //$NON-NLS-1$
	}

	public String toString() {
		if (getType() == Type.DIRECT)
			return "DIRECT"; //$NON-NLS-1$
		return getType() + " @ " + getAddress(); //$NON-NLS-1$
	}

	public final boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Proxy))
			return false;
		Proxy p = (Proxy) obj;
		if (p.getType() == getType()) {
			if (getAddress() == null) {
				return (p.getAddress() == null);
			}
			return getAddress().equals(p.getAddress());
		}
		return false;
	}

	public final int hashCode() {
		if (getAddress() == null)
			return getType().hashCode();
		return getType().hashCode() + getAddress().hashCode();
	}

}
