/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.util.*;
import junit.framework.TestCase;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.tests.harness.TestProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.query.IQuery;

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
			return amountWorked == assignedWork;
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

	IQueryable queryable1 = new IQueryable() {
		Integer[] elements = new Integer[] {1, 2, 3, 4, 5};

		public Collector query(IQuery query, Collector collector, IProgressMonitor monitor) {
			try {
				monitor.beginTask("", 10);
				collector = query.perform(createIterator(elements), collector);
				monitor.worked(10);
			} finally {
				monitor.done();
			}
			return collector;
		}
	};

	IQueryable queryable2 = new IQueryable() {
		Integer[] elements = new Integer[] {4, 6, 8, 10, 12};

		public Collector query(IQuery query, Collector collector, IProgressMonitor monitor) {
			try {
				monitor.beginTask("", 10);
				collector = query.perform(createIterator(elements), collector);
				monitor.worked(10);
			} finally {
				monitor.done();
			}
			return collector;
		}
	};

	IQueryable queryable3 = new IQueryable() {
		Integer[] elements = new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};

		public Collector query(IQuery query, Collector collector, IProgressMonitor monitor) {
			try {
				monitor.beginTask("", 10);
				collector = query.perform(createIterator(elements), collector);
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

		public Collector perform(Iterator iterator, Collector result) {
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
		public Collector perform(Iterator iterator, Collector result) {
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
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.query(matchQuery, new Collector(), monitor);
		assertEquals("1.0", 6, collector.size());
		Collection collection = collector.toCollection();
		assertTrue("1.1", collection.contains(2));
		assertTrue("1.2", collection.contains(4));
		assertTrue("1.3", collection.contains(6));
		assertTrue("1.4", collection.contains(8));
		assertTrue("1.5", collection.contains(10));
		assertTrue("1.6", collection.contains(12));
	}

	public void testSingleQueryable() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.query(matchQuery, new Collector(), monitor);
		assertEquals("1.0", 2, collector.size());
		Collection collection = collector.toCollection();
		assertTrue("1.1", collection.contains(2));
		assertTrue("1.2", collection.contains(4));
	}

	public void testSingleContextQuery() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.query(greatestNumberQuery, new Collector(), monitor);
		assertEquals("1.0", 1, collector.size());
		Collection collection = collector.toCollection();
		assertTrue("1.1", collection.contains(5));
	}

	public void testMultipleContextQueries() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.query(greatestNumberQuery, new Collector(), monitor);
		assertEquals("1.0", 1, collector.size());
		Collection collection = collector.toCollection();
		assertTrue("1.1", collection.contains(12));
	}

	public void testListCollector1() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.query(contextQuery, cQueryable.new ListCollector(), monitor);

		// We use a list so there should be 7 elements (4 is there twice)
		assertEquals("1.0", 7, collector.size());
		Collection collection = collector.toCollection();
		assertTrue("1.1", collection.contains(2));
		assertTrue("1.2", collection.contains(4));
		assertTrue("1.3", collection.contains(6));
		assertTrue("1.4", collection.contains(8));
		assertTrue("1.5", collection.contains(10));
		assertTrue("1.6", collection.contains(12));
	}

	public void testListCollector_isEmpty() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.new ListCollector();
		assertTrue("1.0", collector.isEmpty());
		collector = cQueryable.query(contextQuery, cQueryable.new ListCollector(), monitor);
		assertFalse("1.0", collector.isEmpty());
	}

	public void testListCollector_getCollection() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();

		Collector collector = cQueryable.query(contextQuery, cQueryable.new ListCollector(), monitor);

		// We use a list so there should be 7 elements (4 is there twice)
		assertEquals("1.0", 7, collector.size());
		Integer[] array = (Integer[]) collector.toArray(Integer.class);
		Collection collection = Arrays.asList(array);
		assertTrue("1.1", collection.contains(2));
		assertTrue("1.2", collection.contains(4));
		assertTrue("1.3", collection.contains(6));
		assertTrue("1.4", collection.contains(8));
		assertTrue("1.5", collection.contains(10));
		assertTrue("1.6", collection.contains(12));
	}

	public void testListCollector_toArray() {
		final CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();

		class CustomListCollector extends CompoundQueryable.ListCollector {

			public CustomListCollector() {
				cQueryable.super();
			}

			public Collection getCollection() {
				return super.getCollection();
			}
		}

		CustomListCollector collector = (CustomListCollector) cQueryable.query(contextQuery, new CustomListCollector(), monitor);

		// We use a list so there should be 7 elements (4 is there twice)
		assertEquals("1.0", 7, collector.size());
		Collection collection = collector.getCollection();
		assertTrue("1.1", collection.contains(2));
		assertTrue("1.2", collection.contains(4));
		assertTrue("1.3", collection.contains(6));
		assertTrue("1.4", collection.contains(8));
		assertTrue("1.5", collection.contains(10));
		assertTrue("1.6", collection.contains(12));
	}

	public void testCompoundMatchAndQuery() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.query(CompoundQuery.createCompoundQuery(new IQuery[] {matchQuery, matchMod4query}, true), new Collector(), monitor);
		assertEquals("1.0", 3, collector.size());
		Collection collection = collector.toCollection();
		assertTrue("1.2", collection.contains(4));
		assertTrue("1.4", collection.contains(8));
		assertTrue("1.6", collection.contains(12));
	}

	public void testCompoundMatchOrQuery() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.query(CompoundQuery.createCompoundQuery(new IQuery[] {matchQuery, matchMod4query}, false), new Collector(), monitor);
		assertEquals("1.0", 6, collector.size());
		Collection collection = collector.toCollection();
		assertTrue("1.2", collection.contains(2));
		assertTrue("1.2", collection.contains(4));
		assertTrue("1.2", collection.contains(6));
		assertTrue("1.4", collection.contains(8));
		assertTrue("1.2", collection.contains(10));
		assertTrue("1.6", collection.contains(12));
	}

	public void testMatchQueryProgressMonitor() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		cQueryable.query(matchQuery, new Collector(), monitor);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testSingleQueryableProgressMonitor() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		cQueryable.query(matchQuery, new Collector(), monitor);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testSingleContextQueryProgressMonitor() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		cQueryable.query(greatestNumberQuery, new Collector(), monitor);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testMultipleContextQueriesProgressMonitor() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		cQueryable.query(greatestNumberQuery, new Collector(), monitor);
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testNullProgressMonitor() {
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {queryable1, queryable2});
		cQueryable.query(greatestNumberQuery, new Collector(), null);
		// this is the same as above will null passed in, this should not throw any exceptions

	}

	public void testDoubleCompoundContextOrQuery() {
		CompoundQueryable cQueryable1 = new CompoundQueryable(new IQueryable[] {queryable3, queryable2});
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {cQueryable1, queryable1});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.query(CompoundQuery.createCompoundQuery(new IQuery[] {contextQuery, greatestNumberQuery}, false), new Collector(), monitor);
		assertEquals("1.0", 7, collector.size());
		Collection collection = collector.toCollection();
		assertTrue("1.2", collection.contains(2));
		assertTrue("1.2", collection.contains(4));
		assertTrue("1.2", collection.contains(6));
		assertTrue("1.4", collection.contains(8));
		assertTrue("1.2", collection.contains(10));
		assertTrue("1.6", collection.contains(12));
		assertTrue("1.6", collection.contains(13));
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}

	public void testDoubleCompositeQuery() {
		CompoundQueryable cQueryable1 = new CompoundQueryable(new IQueryable[] {queryable3, queryable2});
		CompoundQueryable cQueryable = new CompoundQueryable(new IQueryable[] {cQueryable1, queryable1});
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		Collector collector = cQueryable.query(new PipedQuery(new IQuery[] {contextQuery, greatestNumberQuery}), new Collector(), monitor);
		assertEquals("1.0", 1, collector.size());
		Collection collection = collector.toCollection();
		assertTrue("1.2", collection.contains(12));
		assertTrue("1.0", monitor.isDone());
		assertTrue("1.1", monitor.isWorkDone());
	}
}
