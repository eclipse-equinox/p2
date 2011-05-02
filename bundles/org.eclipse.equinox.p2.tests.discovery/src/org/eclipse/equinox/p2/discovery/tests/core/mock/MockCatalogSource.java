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
package org.eclipse.equinox.p2.discovery.tests.core.mock;

import java.net.URL;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;

/**
 * @author David Green
 */
public class MockCatalogSource extends AbstractCatalogSource {

	@Override
	public Object getId() {
		return "mock:mock"; //$NON-NLS-1$
	}

	@Override
	public URL getResource(String resourceName) {
		return null;
	}

}
