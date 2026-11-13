/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.security;

import java.io.IOException;

/**
 * Helper class for creating instances of IConnectContext
 * 
 */
public class ConnectContextFactory {
	private ConnectContextFactory() {
		super();
	}

	/**
	 * Create username and password connect context, where username is
	 * represented as a String and password as an Object.
	 * 
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @return IConnectContext for accessing the username and password
	 */
	public static IConnectContext createUsernamePasswordConnectContext(final String username, final Object password) {
		return new IConnectContext() {
			public CallbackHandler getCallbackHandler() {
				return new CallbackHandler() {
					/**
					 * @param callbacks
					 * @throws IOException not thrown by this implementation.
					 * @throws UnsupportedCallbackException not thrown by this implementation.
					 */
					public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
						if (callbacks == null)
							return;
						for (int i = 0; i < callbacks.length; i++) {
							if (callbacks[i] instanceof NameCallback) {
								NameCallback ncb = (NameCallback) callbacks[i];
								ncb.setName(username);
							} else if (callbacks[i] instanceof ObjectCallback) {
								ObjectCallback ocb = (ObjectCallback) callbacks[i];
								ocb.setObject(password);
							} else if (callbacks[i] instanceof PasswordCallback && password instanceof String) {
								PasswordCallback pc = (PasswordCallback) callbacks[i];
								pc.setPassword((String) password);
							} else if (callbacks[i] instanceof PassphraseCallback && password instanceof String) {
								PassphraseCallback pc = (PassphraseCallback) callbacks[i];
								pc.setPassphrase((String) password);
							}
						}
					}
				};
			}
		};
	}

	/**
	 * Create password connect context, where password is represented as a
	 * String
	 * 
	 * @param password
	 *            the password to use
	 * @return IConnectContext for accessing the given password
	 */
	public static IConnectContext createPasswordConnectContext(final String password) {
		return new IConnectContext() {
			public CallbackHandler getCallbackHandler() {
				return new CallbackHandler() {
					/**
					 * @param callbacks
					 * @throws IOException not thrown by this implementation.
					 * @throws UnsupportedCallbackException not thrown by this implementation.
					 */
					public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
						if (callbacks == null)
							return;
						for (int i = 0; i < callbacks.length; i++) {
							if (callbacks[i] instanceof ObjectCallback) {
								ObjectCallback ocb = (ObjectCallback) callbacks[i];
								ocb.setObject(password);
							} else if (callbacks[i] instanceof PasswordCallback) {
								PasswordCallback pc = (PasswordCallback) callbacks[i];
								pc.setPassword(password);
							} else if (callbacks[i] instanceof PassphraseCallback) {
								PassphraseCallback pc = (PassphraseCallback) callbacks[i];
								pc.setPassphrase(password);
							}
						}
					}
				};
			}
		};
	}

}
