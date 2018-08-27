/*******************************************************************************
* Copyright (c) 2009, 2018 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.MatchQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * This tests both Compound and Composite queries
 *
 */
public class AggregateQueryTest extends TestCase {

	public List<String> getABCDE() {
		return Arrays.asList("A", "B", "C", "D", "E");
	}

	public List<String> get123() {
		return Arrays.asList("1", "2", "3");
	}

	public void testEmptyCompositeQuery() {
		IQuery<String> query = QueryUtil.createPipeQuery(Collections.emptySet());
		query.perform(getABCDE().iterator());
		// We should not throw an exception.  No guarantee on what perform
		// will return in this case
	}

	public void testSymmetry() {
		IQuery getLatest = new ContextQuery() {

			@Override
			public Collector perform(Iterator iterator) {
				Collector result = new Collector();
				List list = new ArrayList();
				while (iterator.hasNext()) {
					list.add(iterator.next());
				}
				Collections.sort(list);
				result.accept(list.get(list.size() - 1));
				return result;
			}
		};

		IQuery getAllBut3 = new ContextQuery() {

			@Override
			public Collector perform(Iterator iterator) {
				Collector result = new Collector();
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (!o.equals("3"))
						result.accept(o);
				}
				return result;
			}
		};

		IQuery compoundQuery = QueryUtil.createCompoundQuery(getLatest, getAllBut3, true);
		IQueryResult<String> result = compoundQuery.perform(get123().iterator());
		assertEquals(0, AbstractProvisioningTest.queryResultSize(result));

		compoundQuery = QueryUtil.createCompoundQuery(getAllBut3, getLatest, true);
		result = compoundQuery.perform(get123().iterator());
		assertEquals(0, AbstractProvisioningTest.queryResultSize(result));

		compoundQuery = QueryUtil.createCompoundQuery(getLatest, getAllBut3, false);
		result = compoundQuery.perform(get123().iterator());
		assertEquals(3, AbstractProvisioningTest.queryResultSize(result));

		compoundQuery = QueryUtil.createCompoundQuery(getAllBut3, getLatest, false);
		result = compoundQuery.perform(get123().iterator());
		assertEquals(3, AbstractProvisioningTest.queryResultSize(result));

	}

	/**
	 * The CompositeQuery should not support symmetry.
	 * This method tests that
	 */
	public void testNonSymmetry() {
		IQuery getLatest = new ContextQuery() {

			@Override
			public Collector perform(Iterator iterator) {
				Collector result = new Collector();
				List list = new ArrayList();
				while (iterator.hasNext()) {
					list.add(iterator.next());
				}
				Collections.sort(list);
				result.accept(list.get(list.size() - 1));
				return result;
			}
		};

		IQuery getAllBut3 = new ContextQuery() {

			@Override
			public Collector perform(Iterator iterator) {
				Collector result = new Collector();

				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (!o.equals("3"))
						result.accept(o);
				}
				return result;
			}
		};

		IQuery compoundQuery = QueryUtil.createPipeQuery(getLatest, getAllBut3);
		IQueryResult result = compoundQuery.perform(get123().iterator());
		assertEquals(0, AbstractProvisioningTest.queryResultSize(result));

		compoundQuery = QueryUtil.createPipeQuery(getAllBut3, getLatest);
		result = compoundQuery.perform(get123().iterator());
		assertEquals(1, AbstractProvisioningTest.queryResultSize(result));
		assertEquals("2", result.iterator().next());

	}

	public void testIntersection() {
		IQuery<String> ABC = new MatchQuery<String>() {
			@Override
			public boolean isMatch(String candidate) {
				if (candidate.equals("A") || candidate.equals("B") || candidate.equals("C"))
					return true;
				return false;
			}
		};

		IQuery<String> BCDE = new MatchQuery<String>() {
			@Override
			public boolean isMatch(String candidate) {
				if (candidate.equals("B") || candidate.equals("C") || candidate.equals("D") || candidate.equals("E"))
					return true;
				return false;
			}
		};

		IQuery<String> compoundQuery = QueryUtil.createCompoundQuery(ABC, BCDE, true);
		IQueryResult<String> result = compoundQuery.perform(getABCDE().iterator());
		assertEquals("1.0", AbstractProvisioningTest.queryResultSize(result), 2);
		AbstractProvisioningTest.assertContains("1.1", result, "B");
		AbstractProvisioningTest.assertContains("1.2", result, "C");
	}

	public void testIntersection2() {
		IQuery ABC = new ContextQuery() {
			@Override
			public Collector perform(Iterator iterator) {
				Collector result = new Collector();
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (o.equals("A") || o.equals("B") || o.equals("C"))
						result.accept(o);
				}
				return result;
			}
		};

		IQuery BCDE = new ContextQuery() {
			@Override
			public Collector perform(Iterator iterator) {
				Collector result = new Collector();
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (o.equals("B") || o.equals("C") || o.equals("D") || o.equals("E"))
						result.accept(o);
				}
				return result;
			}
		};

		IQuery compoundQuery = QueryUtil.createCompoundQuery(ABC, BCDE, true);
		IQueryResult result = compoundQuery.perform(getABCDE().iterator());
		assertEquals("1.0", AbstractProvisioningTest.queryResultSize(result), 2);
		AbstractProvisioningTest.assertContains("1.1", result, "B");
		AbstractProvisioningTest.assertContains("1.2", result, "C");
	}

	public void testUnion() {
		IQuery<String> ABC = new MatchQuery<String>() {
			@Override
			public boolean isMatch(String candidate) {
				if (candidate.equals("A") || candidate.equals("B") || candidate.equals("C"))
					return true;
				return false;
			}
		};

		IQuery<String> BCDE = new MatchQuery<String>() {
			@Override
			public boolean isMatch(String candidate) {
				if (candidate.equals("B") || candidate.equals("C") || candidate.equals("D") || candidate.equals("E"))
					return true;
				return false;
			}
		};

		IQuery<String> compoundQuery = QueryUtil.createCompoundQuery(ABC, BCDE, false);
		IQueryResult<String> result = compoundQuery.perform(getABCDE().iterator());
		assertEquals("1.0", AbstractProvisioningTest.queryResultSize(result), 5);
		AbstractProvisioningTest.assertContains("1.1", result, "A");
		AbstractProvisioningTest.assertContains("1.2", result, "B");
		AbstractProvisioningTest.assertContains("1.3", result, "C");
		AbstractProvisioningTest.assertContains("1.4", result, "D");
		AbstractProvisioningTest.assertContains("1.5", result, "E");
	}

	public void testUnion2() {
		IQuery ABC = new ContextQuery() {
			@Override
			public Collector perform(Iterator iterator) {
				Collector result = new Collector();
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (o.equals("A") || o.equals("B") || o.equals("C"))
						result.accept(o);
				}
				return result;
			}
		};

		IQuery BCDE = new ContextQuery() {
			@Override
			public Collector perform(Iterator iterator) {
				Collector result = new Collector();
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (o.equals("B") || o.equals("C") || o.equals("D") || o.equals("E"))
						result.accept(o);
				}
				return result;
			}
		};

		IQuery<String> compoundQuery = QueryUtil.createCompoundQuery(ABC, BCDE, false);
		IQueryResult<String> result = compoundQuery.perform(getABCDE().iterator());
		assertEquals("1.0", AbstractProvisioningTest.queryResultSize(result), 5);
		AbstractProvisioningTest.assertContains("1.1", result, "A");
		AbstractProvisioningTest.assertContains("1.2", result, "B");
		AbstractProvisioningTest.assertContains("1.3", result, "C");
		AbstractProvisioningTest.assertContains("1.4", result, "D");
		AbstractProvisioningTest.assertContains("1.5", result, "E");
	}
}
