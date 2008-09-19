/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.ui.query.AvailableIUCollector;
import org.eclipse.equinox.internal.provisional.p2.ui.query.LatestIUVersionElementCollector;
import org.eclipse.equinox.p2.tests.MockQueryable;

/**
 * 
 */
public class LatestIUVersionElementCollectorTest extends LatestIUVersionCollectorTest {
	protected AvailableIUCollector createCollector(boolean makeCategories) {
		return new LatestIUVersionElementCollector(new MockQueryProvider(new IUPropertyQuery("key", "value")), new MockQueryable(), null, makeCategories);
	}
}
