/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.net.URI;
import org.eclipse.equinox.p2.metadata.ICopyright;

/**
 * The <code>Copyright</code> class represents a software copyright.  A copyright has 
 * required body text which may be the full text or a summary.  An optional location field can be specified
 * which links to full text.  
 */
public class Copyright implements ICopyright {
	/**
	 * The <code>body</code> contains the descriptive text for the copyright. This may
	 * be a summary for a copyright specified in a URL.
	 */
	private final String body;

	/**
	 * The <code>location</code> is the location of a document containing a copyright notice.
	 */
	private final URI location;

	/**
	 * Creates a new copyright. The body must contain the full text of the copyright.
	 * 
	 * @param location the location of a document containing the copyright notice, or <code>null</code>
	 * @param body the copyright body, cannot be <code>null</code>
	 * @throws IllegalArgumentException when the <code>body</code> is <code>null</code>
	 */
	public Copyright(URI location, String body) {
		if (body == null) {
			throw new IllegalArgumentException("body cannot be null"); //$NON-NLS-1$
		}
		this.location = location;
		this.body = body;
	}

	/**
	 * Returns the location of a document containing the copyright notice.
	 * 
	 * @return The location of the copyright notice, or <code>null</code>
	 */
	@Override
	public URI getLocation() {
		return location;
	}

	/**
	 * Returns the license body.
	 * 
	 * @return the license body, never <code>null</code>
	 */
	@Override
	public String getBody() {
		return body;
	}
}
