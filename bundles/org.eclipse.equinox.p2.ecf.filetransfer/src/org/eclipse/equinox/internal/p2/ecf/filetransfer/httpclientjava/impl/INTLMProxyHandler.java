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
package org.eclipse.ecf.internal.provider.filetransfer.httpclientjava;

import java.util.Map;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.core.util.Proxy;

public interface INTLMProxyHandler {
	void handleNTLMProxy(Proxy proxy, int code) throws ECFException;

	void handleSPNEGOProxy(Proxy proxy, int code) throws ECFException;

	boolean allowNTLMAuthentication(Map<?, ?> connectOptions);
}
