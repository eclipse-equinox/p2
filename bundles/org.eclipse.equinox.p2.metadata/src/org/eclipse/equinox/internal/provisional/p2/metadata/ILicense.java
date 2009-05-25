/******************************************************************************* 
* Copyright (c) 2008, 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
 *   IBM - ongoing development
******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

import java.math.BigInteger;
import java.net.URI;

/**
 * The <code>ILicense</code> interface represents a software license.  A license has required body text
 * which may be the full text or an annotation.  An optional URL field can be specified
 * which links to full text.  Licenses can be easily compared using their digests.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ILicense {

	/**
	 * Returns the location of a document containing the full license.
	 * 
	 * @return the location of the license document, or <code>null</code>
	 */
	public URI getLocation();

	/**
	 * Returns the license body.
	 * @return the license body, never <code>null</code>
	 */
	public String getBody();

	/**
	 * Returns the message digest of the license body.  The digest is calculated on a normalized
	 * version of the license where all whitespace has been reduced to one space.
	 * 
	 * Any SPI must maintain the same semantics as:
	 * <code>{@link org.eclipse.equinox.internal.provisional.p2.metadata.ILicense#getDigest()}</code>
	 * @return the message digest as a <code>BigInteger</code>, never <code>null</code>
	 */
	public BigInteger getDigest();

	/**
	 * Returns whether this license is equal to the given object.
	 * 
	 * This method returns <i>true</i> if:
	 * <ul>
	 *  <li> Both this object and the given object are of type ILicense
	 *  <li> The result of <b>getDigest()</b> on both objects are equal
	 * </ul> 
	 */
	public boolean equals(Object obj);

}