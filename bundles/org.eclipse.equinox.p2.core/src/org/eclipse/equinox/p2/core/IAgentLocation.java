/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.core;

import java.net.URI;

/**
 * An instance of this service represents the location of a provisioning agent's 
 * metadata. 
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IAgentLocation {
	/**
	 * Service name constant for the agent location service.
	 */
	public static final String SERVICE_NAME = IAgentLocation.class.getName();

	/**
	 * Returns the location where the bundle with the given namespace
	 * may write its agent-related data.
	 * @param namespace The namespace of the bundle storing the data
	 * @return The data location
	 */
	public URI getDataArea(String namespace);

	/**
	 * Returns the root {@link URI} of the agent metadata.
	 * 
	 * @return the location of the agent metadata
	 */
	public URI getRootLocation();

}
