/******************************************************************************* 
* Copyright (c) 2008, 2012 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
 *   IBM - ongoing development
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.net.URI;

/**
 * The <code>ILicense</code> interface represents a software license.  A license has required body text
 * which may be the full text or an annotation.  An optional URL field can be specified
 * which links to full text.  Licenses can be easily compared using their digests.
 * Licenses can be added to arbitrary installable units, and will typically be displayed to 
 * end users and require explicit agreement before installation proceeds.
 * <p>
 * Instances of this class are handle objects and do not necessarily
 * reflect entities that exist in any particular profile or repository. These handle 
 * objects can be created using {@link MetadataFactory}.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
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
	 * @return the message digest as a <code>BigInteger</code>, never <code>null</code>
	 */
	public String getUUID();

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