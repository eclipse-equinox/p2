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

import java.util.Collection;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for {@link Collector}.
 */
public class CollectorTest extends AbstractProvisioningTest {
	public void testAccept() {
		Collector collector = new Collector();
		String value = "value";
		collector.accept(value);
		Object[] result = collector.toArray(Object.class);
		assertEquals("1.0", 1, result.length);
		assertEquals("1.1", value, result[0]);

		//adding a second copy of the same object is rejected
		collector.accept(new String(value));
		result = collector.toArray(Object.class);
		assertEquals("1.0", 1, result.length);
		assertEquals("1.1", value, result[0]);
	}

	public void testIsEmpty() {
		Collector collector = new Collector();
		assertEquals("1.0", true, collector.isEmpty());
		collector.accept("value");
		assertEquals("1.0", false, collector.isEmpty());
	}

	public void testToCollection() {
		Collector collector = new Collector();
		Collection result = collector.toCollection();
		assertEquals("1.0", 0, result.size());
		//collection should be immutable
		try {
			result.add("value");
			fail("1.1");
		} catch (RuntimeException e) {
			//expected
		}

		String value = "value";
		collector.accept(value);
		result = collector.toCollection();
		assertEquals("2.0", 1, result.size());
		assertEquals("2.1", value, result.iterator().next());
		//collection should be immutable
		try {
			result.clear();
			fail("2.2");
		} catch (RuntimeException e) {
			//expected
		}

	}
}
