/****************************************************************************
 * Copyright (c) 2019 Yatta Solutions and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Yatta Solutions - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.internal.provider.filetransfer.httpclient5.win32;

import java.util.Map;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.BrowseFileTransferException;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.DefaultNTLMProxyHandler;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.INTLMProxyHandler;
import org.osgi.service.component.annotations.Component;

@Component(service = {INTLMProxyHandler.class})
public class Win32NTLMProxyHandler extends DefaultNTLMProxyHandler {

	@Override
	public boolean allowNTLMAuthentication(Map<?, ?> connectOptions) {
		DefaultNTLMProxyHandler.setSeenNTLM();
		return true;
	}

	protected boolean isExplicitAllowNTLMAuthentication() {
		return super.allowNTLMAuthentication(null);
	}

	@Override
	public void handleNTLMProxy(Proxy proxy, int code) throws IncomingFileTransferException {
		DefaultNTLMProxyHandler.setSeenNTLM();
		if (code != 407 || isExplicitAllowNTLMAuthentication()) {
			return;
		}
		super.handleNTLMProxy(proxy, code);
	}

	@Override
	public void handleSPNEGOProxy(Proxy proxy, int code) throws BrowseFileTransferException {
		if (code != 407 || isExplicitAllowNTLMAuthentication()) {
			return;
		}
		super.handleSPNEGOProxy(proxy, code);
	}

}
