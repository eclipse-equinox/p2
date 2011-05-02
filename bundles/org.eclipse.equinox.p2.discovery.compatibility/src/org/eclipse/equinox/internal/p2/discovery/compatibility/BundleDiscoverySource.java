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

import java.net.URL;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.osgi.framework.Bundle;

/**
 * @author David Green
 */
public class BundleDiscoverySource extends AbstractCatalogSource {

	private final Bundle bundle;

	public BundleDiscoverySource(Bundle bundle) {
		if (bundle == null) {
			throw new IllegalArgumentException();
		}
		this.bundle = bundle;
	}

	@Override
	public Object getId() {
		return "bundle:" + bundle.getSymbolicName(); //$NON-NLS-1$
	}

	@Override
	public URL getResource(String relativeUrl) {
		return bundle.getEntry(relativeUrl);
	}

}
