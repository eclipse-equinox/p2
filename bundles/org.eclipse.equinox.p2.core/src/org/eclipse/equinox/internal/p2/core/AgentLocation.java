/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.net.URI;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.IAgentLocation;

/**
 * Internal class.
 */
public class AgentLocation implements IAgentLocation {

	private URI location = null;

	public AgentLocation(URI location) {
		this.location = location;
	}

	public synchronized URI getRootLocation() {
		return location;
	}

	public URI getDataArea(String touchpointId) {
		return URIUtil.append(getRootLocation(), touchpointId + '/');
	}

	public String toString() {
		if (location == null)
			return "No location specified"; //$NON-NLS-1$
		return location.toString();
	}
}
