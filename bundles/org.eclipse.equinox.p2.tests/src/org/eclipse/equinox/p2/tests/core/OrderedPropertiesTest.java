/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.util.Iterator;
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

	/**
	 * Ordered properties guarantees that iteration order is the same as the 
	 * insertion order. This test verifies the claim is true.
	 */
	public void testIterationOrder() {
		//asserts that iteration always occurs in insertion order
		OrderedProperties props = new OrderedProperties();
		props.setProperty("one", "one");
		props.setProperty("two", "two");
		for (Iterator it = props.keySet().iterator(); it.hasNext();) {
			assertEquals("one", it.next());
			assertEquals("two", it.next());
		}
		props = new OrderedProperties();
		props.setProperty("two", "two");
		props.setProperty("one", "one");
		for (Iterator it = props.keySet().iterator(); it.hasNext();) {
			assertEquals("two", it.next());
			assertEquals("one", it.next());
		}

		//removing and re-adding a property should move it to the back of the insertion order
		props.remove("two");
		props.setProperty("two", "two");
		for (Iterator it = props.keySet().iterator(); it.hasNext();) {
			assertEquals("one", it.next());
			assertEquals("two", it.next());
		}
	}
}
