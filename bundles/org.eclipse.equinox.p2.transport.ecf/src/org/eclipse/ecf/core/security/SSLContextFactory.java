/****************************************************************************
 * Copyright (c) 2024 Composent, Inc. and others.
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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.net.ssl.SSLContext;

/**
 * This class exposes three legacy static methods from the {@link SSLContext} class as 
 * methods on an implementing instance.  Implementing instances should be registered
 * as OSGi services.
 * 
 * @since 3.12
 */
public interface SSLContextFactory {

	/**
	 * See <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/net/ssl/SSLContext.html#getDefault()">SSLContext.getDefault()</a><br>
	 * NOTE: Rather than the using the {@link #SSLContext.setDefault SSLContext.setDefault(SSLContext)}
	 * to set the default SSLContext as described in the SSLContext.getDefault() javadocs, the default for the implementer is set upon construction of the service instance.
	 */
	SSLContext getDefault() throws NoSuchAlgorithmException, NoSuchProviderException;

	/**
	 * See <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/net/ssl/SSLContext.html#getInstance(java.lang.String)">SSLContext.getInstance(String protocol)</a><br>
	 */
	SSLContext getInstance(String protocol) throws NoSuchAlgorithmException, NoSuchProviderException;

	/**
	 * See <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/net/ssl/SSLContext.html#getInstance(java.lang.String,java.security.Provider)">SSLContext.getInstance(String protocol, String provider)</a><br>
	 */
	SSLContext getInstance(String protocol, String providerName) throws NoSuchAlgorithmException, NoSuchProviderException;

}
