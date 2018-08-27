/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
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
