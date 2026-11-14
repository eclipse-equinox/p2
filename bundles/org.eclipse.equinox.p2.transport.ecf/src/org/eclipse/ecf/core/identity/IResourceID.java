/****************************************************************************
 * Copyright (c) 2009 EclipseSource and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.identity;

import java.net.URI;

/**
 * Resource id. ID instances that implement this interface are expected to be
 * resources (files, directories, URLs, etc) and so can be identified via a
 * {@link URI}.
 * 
 * @since 3.0
 * 
 */
public interface IResourceID extends ID {

	/**
	 * Convert this resource ID to a {@link URI}.
	 * 
	 * @return URI for this resource ID
	 */
	public URI toURI();

}
