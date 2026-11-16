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

import java.net.URI;
import java.util.Map;

/**
 * An agent service that allows clients to observe and influence the
 * protocol-level metadata of a {@link Transport} request without changing the
 * {@link Transport} contract. The headers are expressed as generic name/value
 * pairs so that the hook is not tied to a specific protocol, transports that
 * have no such concept (e.g. plain file access) may ignore it.
 *
 * <p>
 * Implementations must be thread safe and fast, as they are called on the
 * transfer path.
 * </p>
 */
public interface TransportInterceptor {

	/**
	 * Called on the requesting thread before a request is sent. The implementation
	 * can add or change request headers, for example a conditional
	 * <code>If-None-Match</code>.
	 *
	 * @param uri            the requested URI
	 * @param requestHeaders the mutable headers that will be sent
	 * @return a handler for the response of this very request, or
	 *         <code>null</code> if the response is of no interest
	 */
	TransportResponseHandler intercept(URI uri, Map<String, String> requestHeaders);
}
