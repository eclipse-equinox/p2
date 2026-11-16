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

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;

/**
 * Thrown when reading the stream of a {@link Transport} if the remote content
 * is reported as not modified, either because the server answered so (e.g. HTTP
 * 304) or because a {@link TransportInterceptor} decided that the content is
 * not needed.
 * <p>
 * Downloads report the same condition with a {@link IStatus#CANCEL} status that
 * has the code {@link #STATUS_CODE}.
 * </p>
 */
public class NotModifiedException extends IOException {

	private static final long serialVersionUID = 1L;

	/**
	 * The code of the {@link IStatus#CANCEL} status that downloads return if the
	 * content is not modified.
	 */
	public static final int STATUS_CODE = 1300;

	private final URI uri;
	private final transient Map<String, List<String>> headers;

	public NotModifiedException(URI uri, Map<String, List<String>> headers) {
		super("Not modified: " + uri); //$NON-NLS-1$
		this.uri = uri;
		this.headers = headers;
	}

	public URI getURI() {
		return uri;
	}

	/**
	 * @return the case-insensitive response headers, empty if unknown
	 */
	public Map<String, List<String>> getHeaders() {
		return headers;
	}
}
