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
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for the {@link org.eclipse.equinox.internal.provisional.p2.query.Query} class.
 */
public class QueryTest extends TestCase {

	static class AnyStringQuery extends MatchQuery {
		@Override
		public boolean isMatch(Object candidate) {
			return candidate instanceof String;
		}
	}

	/**
	* Tests a simple perform where all items match.
	*/
	public void testPerformSimple() {
		List<String> items = Arrays.asList("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQueryResult collector = query.perform(items.iterator());
		assertEquals("1.0", 3, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains("1.1", collector, "red");
		AbstractProvisioningTest.assertContains("1.2", collector, "green");
		AbstractProvisioningTest.assertContains("1.3", collector, "blue");
	}

	/**
	 * Tests a perform where only some items match.
	 */
	public void testPerformSomeMatches() {
		List<Object> items = Arrays.asList(new Object(), "green", new Object());
		IQuery query = new AnyStringQuery();
		IQueryResult collector = query.perform(items.iterator());
		assertEquals("1.0", 1, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains("1.1", collector, "green");
	}

	public void testLimitQuery() {
		List<String> items = Arrays.asList("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = QueryUtil.createLimitQuery(query, 1);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals("1.0", 1, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains("1.1", collector, "red");
	}

	public void testLimitQuery2() {
		List<String> items = Arrays.asList("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = QueryUtil.createLimitQuery(query, 2);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals("1.0", 2, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains("1.1", collector, "red");
		AbstractProvisioningTest.assertContains("1.2", collector, "green");
	}

	public void testLimitQuery3() {
		List<String> items = Arrays.asList("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = QueryUtil.createLimitQuery(query, 3);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals("1.0", 3, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains("1.1", collector, "red");
		AbstractProvisioningTest.assertContains("1.2", collector, "green");
		AbstractProvisioningTest.assertContains("1.3", collector, "blue");
	}

	public void testLimitQuery0() {
		List<String> items = Arrays.asList("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = QueryUtil.createLimitQuery(query, 0);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals("1.0", 0, AbstractProvisioningTest.queryResultSize(collector));
	}

	//	public void testPipedLimitQuery() {
	//		List items = Arrays.asList("pink", "red", "green", "pink");
	//		IQuery anyString = new AnyStringQuery();
	//		IQuery containsI = new MatchQuery() {
	//
	//			public boolean isMatch(Object candidate) {
	//				return ((String) candidate).contains("e");
	//			}
	//		};
	//		IQuery pipedQuery = new PipedQuery(new IQuery[] {anyString, containsI});
	//		IQuery limitQuery = LimitQuery.create(pipedQuery, 1);
	//		Collector collector = limitQuery.perform(items.iterator(), new Collector());
	//		Collection result = collector.toCollection();
	//		assertEquals("1.0", 1, result.size());
	//		assertTrue("1.1", result.contains("red"));
	//	}
}
