/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.core.location;

import java.net.URL;

public interface AgentLocation {
	public static final String SERVICE_NAME = AgentLocation.class.getName();

	public URL getArtifactRepositoryURL();

	public URL getMetadataRepositoryURL();

	/**
	 * Returns the location where the bundle with the given namespace
	 * may write its agent-related data.
	 * @param namespace The namespace of the bundle storing the data
	 * @return The data location
	 */
	public URL getDataArea(String namespace);

	/**
	 * Returns the actual {@link URL} of this location.  If the location's value has been set, 
	 * that value is returned.  If the value is not set and the location allows defaults, 
	 * the value is set to the default and returned.  In all other cases <code>null</code>
	 * is returned.
	 * 
	 * @return the URL for this location or <code>null</code> if none
	 */
	public URL getURL();

}
