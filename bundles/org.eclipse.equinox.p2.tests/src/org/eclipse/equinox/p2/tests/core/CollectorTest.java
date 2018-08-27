/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.util.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for {@link Collector}.
 */
public class CollectorTest extends AbstractProvisioningTest {
	public void testAccept() {
		Collector<Object> collector = new Collector<>();
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
		Collector<String> collector = new Collector<>();
		assertEquals("1.0", true, collector.isEmpty());
		collector.accept("value");
		assertEquals("1.0", false, collector.isEmpty());
	}

	/**
	 * This tests the query method on the collector.
	 */
	public void testCompositeCollectors() {
		String[] s = new String[] {"A", "B", "C", "D", "E", "F", "G", "1", "2", "3", "4", "5", "6", "7"};
		List<String> list = Arrays.asList(s);
		IQuery<String> numeric = new MatchQuery<String>() {

			@Override
			public boolean isMatch(String candidate) {
				if (candidate.compareTo("0") > 0 && candidate.compareTo("8") < 0) {
					return true;
				}
				return false;
			}
		};

		IQuery<String> fourOrFiveOrABC = new MatchQuery<String>() {
			@Override
			public boolean isMatch(String candidate) {
				if (candidate.equals("4") || candidate.equals("5") || candidate.equals("A") || candidate.equals("B") || candidate.equals("C")) {
					return true;
				}
				return false;
			}
		};
		IQueryResult<String> queryResult = numeric.perform(list.iterator());
		assertEquals("1.0", 7, queryResultSize(queryResult));

		queryResult = queryResult.query(fourOrFiveOrABC, null);
		assertEquals("2.0", 2, queryResultSize(queryResult));
		assertContains("2.1", queryResult, "4");
		assertContains("2.2", queryResult, "5");
	}

	public void testSameCollector() {
		String[] s = new String[] {"A", "B", "C", "D", "E", "F", "G", "1", "2", "3", "4", "5", "6", "7"};
		List<String> list = Arrays.asList(s);
		IQuery<String> numeric = new MatchQuery<String>() {

			@Override
			public boolean isMatch(String candidate) {
				if (candidate.compareTo("0") > 0 && candidate.compareTo("8") < 0) {
					return true;
				}
				return false;
			}
		};

		IQuery<String> fourOrFiveOrABC = new MatchQuery<String>() {
			@Override
			public boolean isMatch(String candidate) {
				if (candidate.equals("4") || candidate.equals("5") || candidate.equals("A") || candidate.equals("B") || candidate.equals("C")) {
					return true;
				}
				return false;
			}
		};
		Collector<String> collector = new Collector<>();
		collector.addAll(numeric.perform(list.iterator()));
		assertEquals("1.0", 7, collector.toUnmodifiableSet().size());

		collector.addAll(collector.query(fourOrFiveOrABC, null));
		Collection<String> collection = collector.toUnmodifiableSet();
		assertEquals("2.0", 7, collection.size());
	}

	/**
	 * This tests the query method on the collector.
	 */
	public void testEmptyCompositeCollectors() {
		String[] s = new String[] {"A", "B", "C", "D", "E", "F", "G", "1", "2", "3", "4", "5", "6", "7"};
		List<String> list = Arrays.asList(s);
		IQuery<String> eightOrNine = new MatchQuery<String>() {

			@Override
			public boolean isMatch(String candidate) {
				if (candidate.compareTo("8") > 0 && candidate.compareTo("9") < 0) {
					return true;
				}
				return false;
			}
		};

		IQuery<String> fourOrFiveOrABC = new MatchQuery<String>() {
			@Override
			public boolean isMatch(String candidate) {
				if (candidate.equals("4") || candidate.equals("5") || candidate.equals("A") || candidate.equals("B") || candidate.equals("C")) {
					return true;
				}
				return false;
			}
		};
		IQueryResult<String> queryResult = eightOrNine.perform(list.iterator());
		assertTrue("1.0", queryResult.isEmpty());

		queryResult = queryResult.query(fourOrFiveOrABC, null);
		assertTrue("2.0", queryResult.isEmpty());
	}

	public void testToCollection() {
		Collector<String> collector = new Collector<>();
		Collection<String> result = collector.toUnmodifiableSet();
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
		result = collector.toUnmodifiableSet();
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
