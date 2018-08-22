/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public synchronized URI getRootLocation() {
		return location;
	}

	@Override
	public URI getDataArea(String touchpointId) {
		return URIUtil.append(getRootLocation(), touchpointId + '/');
	}

	@Override
	public String toString() {
		if (location == null)
			return "No location specified"; //$NON-NLS-1$
		return location.toString();
	}
}
