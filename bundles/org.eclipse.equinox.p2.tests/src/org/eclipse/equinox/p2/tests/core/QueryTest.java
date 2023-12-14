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

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.MatchQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.junit.Test;

/**
 * Tests for the {@link org.eclipse.equinox.p2.query.Query} class.
 */
public class QueryTest {

	static class AnyStringQuery extends MatchQuery {
		@Override
		public boolean isMatch(Object candidate) {
			return candidate instanceof String;
		}
	}

	/**
	 * Tests a simple perform where all items match.
	 */
	@Test
	public void testPerformSimple() {
		List<String> items = List.of("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQueryResult collector = query.perform(items.iterator());
		assertEquals(3, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains(collector, "red");
		AbstractProvisioningTest.assertContains(collector, "green");
		AbstractProvisioningTest.assertContains(collector, "blue");
	}

	/**
	 * Tests a perform where only some items match.
	 */
	@Test
	public void testPerformSomeMatches() {
		List<Object> items = List.of(new Object(), "green", new Object());
		IQuery query = new AnyStringQuery();
		IQueryResult collector = query.perform(items.iterator());
		assertEquals(1, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains(collector, "green");
	}

	@Test
	public void testLimitQuery() {
		List<String> items = List.of("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = QueryUtil.createLimitQuery(query, 1);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals(1, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains(collector, "red");
	}

	@Test
	public void testLimitQuery2() {
		List<String> items = List.of("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = QueryUtil.createLimitQuery(query, 2);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals(2, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains(collector, "red");
		AbstractProvisioningTest.assertContains(collector, "green");
	}

	@Test
	public void testLimitQuery3() {
		List<String> items = List.of("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = QueryUtil.createLimitQuery(query, 3);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals(3, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains(collector, "red");
		AbstractProvisioningTest.assertContains(collector, "green");
		AbstractProvisioningTest.assertContains(collector, "blue");
	}

	@Test
	public void testLimitQuery0() {
		List<String> items = List.of("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = QueryUtil.createLimitQuery(query, 0);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals(0, AbstractProvisioningTest.queryResultSize(collector));
	}
}
