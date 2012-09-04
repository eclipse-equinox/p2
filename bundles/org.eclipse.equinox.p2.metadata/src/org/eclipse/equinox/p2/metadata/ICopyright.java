/******************************************************************************* 
* Copyright (c) 2008, 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.net.URI;

/**
 * The <code>ICopyright</code> interface represents a software copyright.  A copyright has 
 * required body text which may be the full text or a summary.  An optional location field can be specified
 * which links to full text.  
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
public interface ICopyright {

	/**
	 * Returns the location of a document containing the copyright notice.
	 * 
	 * @return The location of the copyright notice, or <code>null</code>
	 */
	public URI getLocation();

	/**
	 * Returns the license body.
	 * 
	 * @return the license body, never <code>null</code>
	 */
	public String getBody();

}