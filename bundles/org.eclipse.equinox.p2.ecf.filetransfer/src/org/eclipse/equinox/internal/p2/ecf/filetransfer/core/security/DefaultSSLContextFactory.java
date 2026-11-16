/*******************************************************************************
 * Copyright (c) 2025, 2026 Christoph Läubrich and others.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 * Contributors:
 * 	Christoph Läubrich - initial implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.ecf.filetransfer.core.security;

import java.security.*;
import javax.net.ssl.SSLContext;

/**
 * The {@link SSLContextFactory} used when no other one is registered as a
 * service. It uses the default SSL context of the JRE.
 */
public class DefaultSSLContextFactory implements SSLContextFactory {

	public static final SSLContextFactory INSTANCE = new DefaultSSLContextFactory();

	@Override
	public SSLContext getDefault() throws NoSuchAlgorithmException {
		// the default is guaranteed to be initialized always
		return SSLContext.getDefault();
	}

	@Override
	public SSLContext getInstance(String protocol) throws NoSuchAlgorithmException, NoSuchProviderException {
		SSLContext context = SSLContext.getInstance(protocol);
		try {
			// init it as we have a new context here
			context.init(null, null, null);
		} catch (KeyManagementException e) {
			throw new NoSuchProviderException();
		}
		return context;
	}

	@Override
	public SSLContext getInstance(String protocol, String providerName)
			throws NoSuchAlgorithmException, NoSuchProviderException {
		SSLContext context = SSLContext.getInstance(protocol, providerName);
		try {
			// init it as we have a new context here
			context.init(null, null, null);
		} catch (KeyManagementException e) {
			throw new NoSuchProviderException();
		}
		return context;
	}

}
