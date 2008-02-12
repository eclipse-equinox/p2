/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The <code>Copyright</code> class represents a software copyright.  A copyright has 
 * required body text which may be the full text or an annotation.  An optional URL field can be specified
 * which links to full text.  
 */
public class Copyright {
	/**
	 * The <code>body</code> contains the descriptive text for the coypright. This may
	 * be a summary for a copyright specified in a URL.
	 */
	private final String body;

	/**
	 * The <code>url</code> is the URL of the copyright.
	 */
	private URL url;

	/**
	 * Creates a new copyright.
	 * The body should contain the full text of the copyright.
	 * @param urlString the string describing the URL of the full copyright text, may be <code>null</code>
	 * @param body the copyright body, cannot be <code>null</code>
	 * @throws IllegalArgumentException when the <code>body</code> is <code>null</code>
	 */
	public Copyright(String urlString, String body) {
		if (body == null)
			throw new IllegalArgumentException("body cannot be null"); //$NON-NLS-1$
		if (urlString != null)
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				url = null;
			}
		this.body = body;
	}

	/**
	 * Returns the URL containing the full description of the license.
	 * May be <code>null</code>.
	 */
	public URL getURL() {
		return url;
	}

	/**
	 * Returns the license body.
	 * @return the license body, never <code>null</code>
	 */
	public String getBody() {
		return body;
	}
}
