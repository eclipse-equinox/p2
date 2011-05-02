/*******************************************************************************
 * Copyright (c) 2008, 2010 Genuitec, LLC and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 		Genuitec, LLC - initial API and implementation
 * 		IBM Corporation - ongoing development
 *      EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.eclipse.equinox.p2.metadata.ILicense;

/**
 * The <code>License</code> class represents a software license.  A license has required body text
 * which may be the full text or an annotation.  An optional URL field can be specified
 * which links to full text.  Licenses can be easily compared using their digests.
 */
public class License implements ILicense {
	/**
	 * The <code>body</code> contains the descriptive text for the license. This may
	 * be a summary for a full license specified in a URL.
	 */
	private final String body;

	/**
	 * The <code>location</code> is the URL of the license.
	 */
	private URI location;

	/**
	 * The <code>digest</code> is the cached message digest of the normalized body
	 */
	private String digest;

	/**
	 * Creates a new license object which is identified by users using the <code>body</code> field.
	 * The body should contain either the full text of the license or an summary for a license
	 * fully specified in the given location.
	 * 
	 * @param location the location of a document containing the full license, or <code>null</code>
	 * @param body the license body, cannot be <code>null</code>
	 * @throws IllegalArgumentException when the <code>body</code> is <code>null</code>
	 */
	public License(URI location, String body, String uuid) {
		if (body == null)
			throw new IllegalArgumentException("body cannot be null"); //$NON-NLS-1$
		this.body = body;
		this.location = location;
		this.digest = uuid;
	}

	/**
	 * Returns the location of a document containing the full license.
	 * 
	 * @return the location of the license document, or <code>null</code>
	 */
	public URI getLocation() {
		return location;
	}

	/**
	 * Returns the license body.
	 * @return the license body, never <code>null</code>
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Returns the message digest of the license body.  The digest is calculated on a normalized
	 * version of the license where all whitespace has been reduced to one space.
	 * @return the message digest as a <code>BigInteger</code>, never <code>null</code>
	 */
	public synchronized String getUUID() {
		if (digest == null)
			digest = calculateLicenseDigest().toString(16);

		return digest;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof ILicense) {
			ILicense other = (ILicense) obj;
			if (other.getUUID().equals(getUUID()))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getUUID().hashCode();
	}

	private BigInteger calculateLicenseDigest() {
		String message = normalize(getBody());
		try {
			MessageDigest algorithm = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
			algorithm.reset();
			algorithm.update(message.getBytes("UTF-8")); //$NON-NLS-1$
			byte[] digestBytes = algorithm.digest();
			return new BigInteger(1, digestBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Replace all sequences of whitespace with a single whitespace character.
	 */
	private String normalize(String license) {
		String text = license.trim();
		StringBuffer result = new StringBuffer();
		int length = text.length();
		for (int i = 0; i < length; i++) {
			char c = text.charAt(i);
			boolean foundWhitespace = false;
			while (Character.isWhitespace(c) && i < length) {
				foundWhitespace = true;
				c = text.charAt(++i);
			}
			if (foundWhitespace)
				result.append(' ');
			if (i < length)
				result.append(c);
		}
		return result.toString();
	}
}
