/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;
import org.eclipse.equinox.p2.metadata.query.IQuery;
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

	/**
	 * This tests the query method on the collector.
	 */
	public void testCompositeCollectors() {
		String[] s = new String[] {"A", "B", "C", "D", "E", "F", "G", "1", "2", "3", "4", "5", "6", "7"};
		List list = Arrays.asList(s);
		IQuery numeric = new MatchQuery() {

			public boolean isMatch(Object candidate) {
				if (((String) candidate).compareTo("0") > 0 && ((String) candidate).compareTo("8") < 0) {
					return true;
				}
				return false;
			}
		};

		IQuery fourOrFiveOrABC = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (((String) candidate).equals("4") || ((String) candidate).equals("5") || ((String) candidate).equals("A") || ((String) candidate).equals("B") || ((String) candidate).equals("C")) {
					return true;
				}
				return false;
			}
		};
		Collector collector = numeric.perform(list.iterator(), new Collector());
		assertEquals("1.0", 7, collector.toCollection().size());

		collector = collector.query(fourOrFiveOrABC, null);
		Collection collection = collector.toCollection();
		assertEquals("2.0", 2, collection.size());
		assertTrue("2.1", collection.contains("4"));
		assertTrue("2.2", collection.contains("5"));
	}

	public void testSameCollector() {
		String[] s = new String[] {"A", "B", "C", "D", "E", "F", "G", "1", "2", "3", "4", "5", "6", "7"};
		List list = Arrays.asList(s);
		IQuery numeric = new MatchQuery() {

			public boolean isMatch(Object candidate) {
				if (((String) candidate).compareTo("0") > 0 && ((String) candidate).compareTo("8") < 0) {
					return true;
				}
				return false;
			}
		};

		IQuery fourOrFiveOrABC = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (((String) candidate).equals("4") || ((String) candidate).equals("5") || ((String) candidate).equals("A") || ((String) candidate).equals("B") || ((String) candidate).equals("C")) {
					return true;
				}
				return false;
			}
		};
		Collector collector = numeric.perform(list.iterator(), new Collector());
		assertEquals("1.0", 7, collector.toCollection().size());

		collector.addAll(collector.query(fourOrFiveOrABC, null));
		Collection collection = collector.toCollection();
		assertEquals("2.0", 7, collection.size());
	}

	/**
	 * This tests the query method on the collector.
	 */
	public void testEmptyCompositeCollectors() {
		String[] s = new String[] {"A", "B", "C", "D", "E", "F", "G", "1", "2", "3", "4", "5", "6", "7"};
		List list = Arrays.asList(s);
		IQuery eightOrNine = new MatchQuery() {

			public boolean isMatch(Object candidate) {
				if (((String) candidate).compareTo("8") > 0 && ((String) candidate).compareTo("9") < 0) {
					return true;
				}
				return false;
			}
		};

		IQuery fourOrFiveOrABC = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (((String) candidate).equals("4") || ((String) candidate).equals("5") || ((String) candidate).equals("A") || ((String) candidate).equals("B") || ((String) candidate).equals("C")) {
					return true;
				}
				return false;
			}
		};
		Collector collector = eightOrNine.perform(list.iterator(), new Collector());
		assertEquals("1.0", 0, collector.toCollection().size());

		collector = collector.query(fourOrFiveOrABC, null);
		Collection collection = collector.toCollection();
		assertEquals("2.0", 0, collection.size());
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
