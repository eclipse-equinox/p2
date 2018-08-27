/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
	private IQuery<?> query;

	public MockQueryProvider(IQuery<?> query, ProvisioningUI ui) {
		super(ui);
		this.query = query;
	}

	@Override
	public ElementQueryDescriptor getQueryDescriptor(QueriedElement element) {
		return new ElementQueryDescriptor(new MockQueryable(element), query, new Collector<>());
	}
}
