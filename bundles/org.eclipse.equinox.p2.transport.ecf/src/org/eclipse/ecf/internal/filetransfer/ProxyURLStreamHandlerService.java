/****************************************************************************
 * Copyright (c) 2023 Christoph Läubrich
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Christoph Läubrich - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.internal.filetransfer;

import java.io.IOException;
import java.net.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerSetter;

public class ProxyURLStreamHandlerService implements URLStreamHandlerService {

	private static final String SERVICE_CLASS_ATTRIBUTE = "serviceClass"; //$NON-NLS-1$

	private final IConfigurationElement configurationElement;

	private URLStreamHandlerService delegate;

	public ProxyURLStreamHandlerService(IConfigurationElement configurationElement) {
		this.configurationElement = configurationElement;
	}

	@Override
	public URLConnection openConnection(URL u) throws IOException {
		return getDelegate().openConnection(u);
	}

	@Override
	public void parseURL(URLStreamHandlerSetter realHandler, URL u, String spec, int start, int limit) {
		getDelegate().parseURL(realHandler, u, spec, start, limit);
	}

	@Override
	public String toExternalForm(URL u) {
		return getDelegate().toExternalForm(u);
	}

	@Override
	public boolean equals(URL u1, URL u2) {
		return getDelegate().equals(u1, u2);
	}

	@Override
	public int getDefaultPort() {
		return getDelegate().getDefaultPort();
	}

	@Override
	public InetAddress getHostAddress(URL u) {
		return getDelegate().getHostAddress(u);
	}

	@Override
	public int hashCode(URL u) {
		return getDelegate().hashCode(u);
	}

	@Override
	public boolean hostsEqual(URL u1, URL u2) {
		return getDelegate().hostsEqual(u1, u2);
	}

	@Override
	public boolean sameFile(URL u1, URL u2) {
		return getDelegate().sameFile(u1, u2);
	}

	synchronized URLStreamHandlerService getDelegate() {
		if (delegate == null) {
			try {
				delegate = (URLStreamHandlerService) configurationElement.createExecutableExtension(SERVICE_CLASS_ATTRIBUTE);
			} catch (CoreException e) {
				throw new IllegalStateException("can't create executable extension", e); //$NON-NLS-1$
			}
		}
		return delegate;
	}

}
