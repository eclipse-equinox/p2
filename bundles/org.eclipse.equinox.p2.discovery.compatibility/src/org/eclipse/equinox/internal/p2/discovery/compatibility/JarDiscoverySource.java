/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.compatibility;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.*;
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

			return new URL("jar:" + prefix + "!/" + URLEncoder.encode(resourceName, "utf-8")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

}
