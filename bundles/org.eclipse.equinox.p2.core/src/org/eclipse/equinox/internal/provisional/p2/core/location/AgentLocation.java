/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.osgi.service.datalocation.Location;

/**
 * TODO: We are not allowed to extend Location because it's not intended
 * to be implemented by clients.
 */
public interface AgentLocation extends Location {

	public URL getArtifactRepositoryURL();

	public URL getMetadataRepositoryURL();

	/**
	 * Returns the location where the bundle with the given namespace
	 * may write its agent-related data.
	 * @param namespace The namespace of the bundle storing the data
	 * @return The data location
	 */
	public URL getDataArea(String namespace);
}
