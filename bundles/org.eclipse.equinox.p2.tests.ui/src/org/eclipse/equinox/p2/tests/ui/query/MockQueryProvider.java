/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import org.eclipse.equinox.internal.p2.ui.ElementQueryDescriptor;
import org.eclipse.equinox.internal.p2.ui.QueryProvider;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElement;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.tests.MockQueryable;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * A fake query provider for unit testing.
 */
public class MockQueryProvider extends QueryProvider {
	private IQuery query;

	public MockQueryProvider(IQuery query, ProvisioningUI ui) {
		super(ui);
		this.query = query;
	}

	public ElementQueryDescriptor getQueryDescriptor(QueriedElement element) {
		return new ElementQueryDescriptor(new MockQueryable(element), query, new Collector());
	}
}
