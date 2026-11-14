/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.util.List;
import java.util.Map;

/**
 * Handles the response of a single request that was intercepted by a
 * {@link TransportInterceptor}.
 */
@FunctionalInterface
public interface TransportResponseHandler {

	/**
	 * Called as soon as the response metadata is known, before any content is
	 * transferred. It can be called from a different thread than the one that made
	 * the request.
	 *
	 * @param responseHeaders the case-insensitive response headers
	 * @return <code>true</code> if the content is not needed because it is known to
	 *         be unchanged. The transfer is then canceled and the transport throws
	 *         a {@link NotModifiedException}. Returning <code>false</code>
	 *         continues normally.
	 */
	boolean handleResponse(Map<String, List<String>> responseHeaders);
}
