/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import org.eclipse.equinox.p2.query.MatchQuery;

import java.util.Arrays;
import java.util.Iterator;
import junit.framework.TestCase;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.tests.harness.TestProgressMonitor;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests the compound queryable
 */
public class CompoundQueryableTest extends TestCase {

	public static class CompoundQueryTestProgressMonitor extends TestProgressMonitor {

		private boolean isDone;
		private int assignedWork = 0;
		private int amountWorked = 0;

		public void beginTask(String name, int totalWork) {
			super.beginTask(name, totalWork);
			this.assignedWork += totalWork;
		}

		public void worked(int work) {
			amountWorked += work;
		}

		public boolean isWorkDone() {
			return amountWorked > 0 && (assignedWork == IProgressMonitor.UNKNOWN || amountWorked == assignedWork);
		}

		public boolean isDone() {
			return this.isDone;
		}

		public void done() {
			super.done();
			this.isDone = true;
		}
	}

	static Iterator createIterator(Object[] array) {
		return Arrays.asList(array).iterator();
	}

	IQueryable<Integer> queryable1 = new IQueryable<Integer>() {
		Integer[] elements = new Integer[] {1, 2, 3, 4, 5};

		public IQueryResult<Integer> query(IQuery<Integer> query, IProgressMonitor monitor) {
			IQueryResult<Integer> collector;
			try {
				monitor.beginTask("", 10);
				collector = query.perform(createIterator(elements));
				monitor.worked(10);
			} finally {
				monitor.done();
			}
			return collector;
		}
	};

	IQueryable<Integer> queryable2 = new IQueryable<Integer>() {
		Integer[] elements = new Integer[] {4, 6, 8, 10, 12};

		public IQueryResult<Integer> query(IQuery<Integer> query, IProgressMonitor monitor) {
			IQueryResult<Integer> collector;
			try {
				monitor.beginTask("", 10);
				collector = query.perform(createIterator(elements));
				monitor.worked(10);
			} finally {
				monitor.done();
			}
			return collector;
		}
	};

	IQueryable queryable3 = new IQueryable() {
		Integer[] elements = new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};

		public IQueryResult query(IQuery query, IProgressMonitor monitor) {
			IQueryResult collector;
			try {
				monitor.beginTask("", 10);
				collector = query.perform(createIterator(elements));
				monitor.worked(10);
			} finally {
				monitor.done();
			}
			return collector;
		}
	};

	IQuery matchQuery = new MatchQuery() {

		public boolean isMatch(Object candidate) {
			if (candidate instanceof Integer) {
				int x = ((Integer) candidate).intValue();
				if (x % 2 == 0)
					return true;
			}
			return false;
		}
	};

	IQuery matchMod4query = new MatchQuery() {
		public boolean isMatch(Object candidate) {
			if (candidate instanceof Integer) {
				int x = ((Integer) candidate).intValue();
				if (x % 4 == 0)
					return true;
			}
			return false;
		}
	};

	IQuery contextQuery = new ContextQuery() {

		public Collector perform(Iterator iterator) {
			Collector result = new Collector();
			while (iterator.hasNext()) {
				Object o = iterator.next();
				if (o instanceof Integer && ((Integer) o).intValue() % 2 == 0) {
					result.accept(o);
				}
			}
			return result;
		}

	};

	IQuery greatestNumberQuery = new ContextQuery() {
		public Collector perform(Iterator iterator) {
			Collector result = new Collector();
			int greatest = Integer.MIN_VALUE;
			while (iterator.hasNext()) {
				int item = ((Integer) iterator.next()).intValue();
				if (item > greatest)
					greatest = item;
			}
			if (greatest == Integer.MIN_VALUE)
				return result;
			result.accept(greatest);
			return result;
		}
	};

	public void testMatchQuery() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(queryable1, queryable2);
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		IQueryResult queryResult = cQueryable.query(matchQuery, monitor);
		assertEquals("1.0", 6, AbstractProvisioningTest.queryResultSize(queryResult));
		AbstractProvisioningTest.assertContains("1.1", queryResult, 2);
		AbstractProvisioningTest.assertContains("1.2", queryResult, 4);
		AbstractProvisioningTest.assertContains("1.3", queryResult, 6);
		AbstractProvisioningTest.assertContains("1.4", queryResult, 8);
		AbstractProvisioningTest.assertContains("1.5", queryResult, 10);
		AbstractProvisioningTest.assertContains("1.6", queryResult, 12);
	}

	public void testSingleQueryable() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(Arrays.asList(queryable1));
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		IQueryResult queryResult = cQueryable.query(matchQuery, monitor);
		assertEquals("1.0", 2, AbstractProvisioningTest.queryResultSize(queryResult));
		AbstractProvisioningTest.assertContains("1.1", queryResult, 2);
		AbstractProvisioningTest.assertContains("1.2", queryResult, 4);
	}

	public void testSingleContextQuery() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(Arrays.asList(queryable1));
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		IQueryResult queryResult = cQueryable.query(greatestNumberQuery, monitor);
		assertEquals("1.0", 1, AbstractProvisioningTest.queryResultSize(queryResult));
		AbstractProvisioningTest.assertContains("1.1", queryResult, 5);
	}

	public void testMultipleContextQueries() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(queryable1, queryable2);
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		IQueryResult queryResult = cQueryable.query(greatestNumberQuery, monitor);
		assertEquals("1.0", 1, AbstractProvisioningTest.queryResultSize(queryResult));
		AbstractProvisioningTest.assertContains("1.1", queryResult, 12);
	}

	public void testCompoundMatchAndQuery() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(queryable1, queryable2);
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		IQueryResult queryResult = cQueryable.query(QueryUtil.createCompoundQuery(matchQuery, matchMod4query, true), monitor);
		assertEquals("1.0", 3, AbstractProvisioningTest.queryResultSize(queryResult));
		AbstractProvisioningTest.assertContains("1.2", queryResult, 4);
		AbstractProvisioningTest.assertContains("1.4", queryResult, 8);
		AbstractProvisioningTest.assertContains("1.6", queryResult, 12);
	}

	public void testCompoundMatchOrQuery() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(queryable1, queryable2);
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		IQueryResult queryResult = cQueryable.query(QueryUtil.createCompoundQuery(matchQuery, matchMod4query, false), monitor);
		assertEquals("1.0", 6, AbstractProvisioningTest.queryResultSize(queryResult));
		AbstractProvisioningTest.assertContains("1.2", queryResult, 2);
		AbstractProvisioningTest.assertContains("1.2", queryResult, 4);
		AbstractProvisioningTest.assertContains("1.2", queryResult, 6);
		AbstractProvisioningTest.assertContains("1.4", queryResult, 8);
		AbstractProvisioningTest.assertContains("1.2", queryResult, 10);
		AbstractProvisioningTest.assertContains("1.6", queryResult, 12);
	}

	public void testMatchQueryProgressMonitor() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(queryable1, queryable2);
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		cQueryable.query(matchQuery, monitor);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testSingleQueryableProgressMonitor() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(Arrays.asList(queryable1));
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		cQueryable.query(matchQuery, monitor);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testSingleContextQueryProgressMonitor() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(Arrays.asList(queryable1));
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		cQueryable.query(greatestNumberQuery, monitor);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testMultipleContextQueriesProgressMonitor() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(queryable1, queryable2);
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		cQueryable.query(greatestNumberQuery, monitor);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testNullProgressMonitor() {
		IQueryable cQueryable = QueryUtil.compoundQueryable(queryable1, queryable2);
		cQueryable.query(greatestNumberQuery, null);
		// this is the same as above will null passed in, this should not throw any exceptions

	}

	public void testDoubleCompoundContextOrQuery() {
		IQueryable cQueryable1 = QueryUtil.compoundQueryable(queryable3, queryable2);
		IQueryable cQueryable = QueryUtil.compoundQueryable(cQueryable1, queryable1);
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		IQueryResult queryResult = cQueryable.query(QueryUtil.createCompoundQuery(contextQuery, greatestNumberQuery, false), monitor);
		assertEquals("1.0", 7, AbstractProvisioningTest.queryResultSize(queryResult));
		AbstractProvisioningTest.assertContains("1.2", queryResult, 2);
		AbstractProvisioningTest.assertContains("1.2", queryResult, 4);
		AbstractProvisioningTest.assertContains("1.2", queryResult, 6);
		AbstractProvisioningTest.assertContains("1.4", queryResult, 8);
		AbstractProvisioningTest.assertContains("1.2", queryResult, 10);
		AbstractProvisioningTest.assertContains("1.6", queryResult, 12);
		AbstractProvisioningTest.assertContains("1.6", queryResult, 13);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testDoubleCompositeQuery() {
		IQueryable cQueryable1 = QueryUtil.compoundQueryable(queryable3, queryable2);
		IQueryable cQueryable = QueryUtil.compoundQueryable(cQueryable1, queryable1);
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		IQueryResult queryResult = cQueryable.query(QueryUtil.createPipeQuery(contextQuery, greatestNumberQuery), monitor);
		assertEquals("1.0", 1, AbstractProvisioningTest.queryResultSize(queryResult));
		AbstractProvisioningTest.assertContains("1.2", queryResult, 12);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}
}
