/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.net.*;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;

/**
 * Internal class.
 */
public class BasicLocation implements AgentLocation {

	private URL location = null;

	public BasicLocation(URL location) {
		this.location = location;
	}

	public synchronized URL getURL() {
		return location;
	}

	public URI getArtifactRepositoryURI() {
		//the cache is a co-located repository
		return getMetadataRepositoryURI();
	}

	public URI getMetadataRepositoryURI() {
		try {
			return URIUtil.toURI(new URL(getDataArea(Activator.ID), "cache/")); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
		}
		return null;
	}

	public URL getDataArea(String touchpointId) {
		try {
			return new URL(getURL(), touchpointId + '/');
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
}
