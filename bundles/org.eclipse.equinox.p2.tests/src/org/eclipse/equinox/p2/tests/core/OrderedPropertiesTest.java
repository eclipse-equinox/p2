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
package org.eclipse.equinox.p2.tests.core;

import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * @since 3.5
 */
public class OrderedPropertiesTest extends AbstractProvisioningTest {

	public void test_249125() {
		OrderedProperties one = new OrderedProperties();
		OrderedProperties two = new OrderedProperties();
		assertEquals("1.0", one, two);

		one = new OrderedProperties();
		two = new OrderedProperties(1);
		assertEquals("1.1", one, two);

		one = new OrderedProperties(1);
		two = new OrderedProperties();
		assertEquals("1.2", one, two);
	}

}
