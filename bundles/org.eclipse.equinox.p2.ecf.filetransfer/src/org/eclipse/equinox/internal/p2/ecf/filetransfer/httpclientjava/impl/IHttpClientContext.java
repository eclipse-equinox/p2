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

import java.net.Authenticator;

public interface IHttpClientContext {

	void setAttribute(String key, Object value);

	void setCredentialsProvider(Authenticator authenticator);

	Object getAttribute(String key);

	void setProxy(HttpHost httpHost);

}
