/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.configurator;

import java.io.IOException;
import java.net.URL;

/**
 * The implementation of this interface will be registered into a service registry
 * by a Configurator Bundle.
 * 
 * The client bundle can apply configuration which can be interpreted by referring
 * the specified location to the current running OSGi environment. In addition, 
 * the client can expect bundle state in advance .  
 * 
 * TODO: this interface might not be required to be defined.
 * 
 * 
 * **********************************************
 * Current Definition of Configurator Bundle: 
 * 
 * Configurator Bundle will do the following operation at its startup.
 * 
 * 1. Create a Configurator object.
 * 2. Register it as a service to the service registry.
 * 3. Get where to read for knowing what kinds of bundles in its implementation dependent way.
 * 4. Call {@link Configurator#applyConfiguration(URL)} with the URL.
 * 
 * At its stopping, the service registered will be unregistered.
 * 
 * see org.eclipse.equinox.internal.provisional.configuratormanipulato.ConfiguratorManipulator
 *
 */
public interface Configurator {

	/**
	 * Apply configuration read from the specified url to the OSGi 
	 * environment currently running.
	 * 
	 * @param url URL to be read.
	 * @throws IOException - If reading information from the specified url fails. 
	 */
	void applyConfiguration(URL url) throws IOException;

	/**
	 * Apply configuration read from the previously used url to the OSGi 
	 * environment currently running. If it is never used, do nothing.
	 * 
	 * @throws IOException - If reading information from the specified url fails. 
	 */
	void applyConfiguration() throws IOException;

	/**
	 * Return the url in use.
	 * If it is never used, return null.
	 * 
	 * @return URL
	 */
	URL getUrlInUse();
}
