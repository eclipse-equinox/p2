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

/**
 * 
 * @author David Green
 */
public abstract class AbstractMockFactory<MockType> {

	protected int seed = 0;

	private MockType mockObject;

	protected MockCatalogSource source = new MockCatalogSource();

	public final MockType get() {
		MockType object = getMockObject();
		mockObject = null;
		return object;
	}

	public final MockType getMockObject() {
		if (mockObject == null) {
			++seed;
			mockObject = createMockObject();
			populateMockData();
		}
		return mockObject;
	}

	protected abstract void populateMockData();

	protected abstract MockType createMockObject();

	/**
	 * get the number of objects created by this factory
	 */
	public int getCreatedCount() {
		return seed;
	}
}
