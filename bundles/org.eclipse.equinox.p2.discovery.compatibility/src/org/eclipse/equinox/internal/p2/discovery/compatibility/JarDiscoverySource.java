/*******************************************************************************
 * Copyright (c) 2009, 2020 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.compatibility;

import java.io.File;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;

/**
 * @author David Green
 */
public class JarDiscoverySource extends AbstractCatalogSource {

	private final String id;

	private final File jarFile;

	public JarDiscoverySource(String id, File jarFile) {
		this.id = id;
		this.jarFile = jarFile;
	}

	@Override
	public Object getId() {
		return id;
	}

	@Override
	public URL getResource(String resourceName) {
		try {
			String prefix = jarFile.toURI().toURL().toExternalForm();

			return new URL("jar:" + prefix + "!/" + URLEncoder.encode(resourceName, StandardCharsets.UTF_8)); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}

}
