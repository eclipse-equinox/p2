/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	static class PerformHookQuery extends AnyStringQuery {
		private boolean prepared = false;
		private boolean complete = false;

		public boolean areHooksExecutedProperly() {
			// Either they have both been called, or neither has been called
			return (prepared & complete) || (!prepared & !complete);
		}

		public boolean isComplete() {
			return this.complete;
		}

		public boolean isPrepared() {
			return this.prepared;
		}

		public void prePerform() {
			prepared = true;
		}

		public void postPerform() {
			if (!(prepared)) // Note:  is match might not be called if it can be determined it's not needed
				fail("prePerform not called");
			complete = true;
		}

		public boolean isMatch(Object candidate) {
			if (!prepared)
				fail("prePerform not called");
			if (!(candidate instanceof String))
				throw new RuntimeException("Exception intentionally thrown by test");
			return candidate instanceof String;
		}
	}

	/**
	* Tests a simple perform where all items match.
	*/
	public void testPerformSimple() {
		List items = Arrays.asList("red", "green", "blue");
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
		List items = Arrays.asList(new Object(), "green", new Object());
		IQuery query = new AnyStringQuery();
		IQueryResult collector = query.perform(items.iterator());
		assertEquals("1.0", 1, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains("1.1", collector, "green");
	}

	public void testPerformHooks() {
		List items = Arrays.asList("red", "green", "blue");
		PerformHookQuery query = new PerformHookQuery();
		assertFalse("1.0", query.isComplete());
		assertFalse("1.1", query.isPrepared());
		query.perform(items.iterator());
		assertTrue("1.2", query.isComplete());
		assertTrue("1.3", query.isPrepared());
		assertTrue("1.4", query.areHooksExecutedProperly());
	}

	public void testPerformHooksOnQueryFail() {
		List items = Arrays.asList("red", new Object());
		PerformHookQuery query = new PerformHookQuery();
		assertFalse("1.0", query.isComplete());
		assertFalse("1.1", query.isPrepared());
		try {
			query.perform(items.iterator());
		} catch (RuntimeException e) {
			// expected
		}
		assertTrue("1.2", query.isComplete());
		assertTrue("1.3", query.isPrepared());
		assertTrue("1.4", query.areHooksExecutedProperly());
	}

	public void testPreAndPostCompoundANDQuery() {
		List items = Arrays.asList("red", "green", "blue");
		PerformHookQuery query1 = new PerformHookQuery();
		PerformHookQuery query2 = new PerformHookQuery();
		CompoundQuery cQuery = CompoundQuery.createCompoundQuery(new IQuery[] {query1, query2}, true);
		assertFalse("1.0", query1.isComplete());
		assertFalse("1.1", query1.isPrepared());
		assertFalse("1.2", query2.isComplete());
		assertFalse("1.3", query2.isPrepared());
		cQuery.perform(items.iterator());
		assertTrue("1.4", query1.isComplete());
		assertTrue("1.5", query1.isPrepared());
		assertTrue("1.6", query2.isComplete());
		assertTrue("1.7", query2.isPrepared());
		assertTrue("1.8", query1.areHooksExecutedProperly());
		assertTrue("1.9", query2.areHooksExecutedProperly());
	}

	public void testPreAndPostCompoundOrQuery() {
		List items = Arrays.asList("red", "green", "blue");
		PerformHookQuery query1 = new PerformHookQuery();
		PerformHookQuery query2 = new PerformHookQuery();
		CompoundQuery cQuery = CompoundQuery.createCompoundQuery(new IQuery[] {query1, query2}, false);
		assertFalse("1.0", query1.isComplete());
		assertFalse("1.1", query1.isPrepared());
		assertFalse("1.2", query2.isComplete());
		assertFalse("1.3", query2.isPrepared());
		cQuery.perform(items.iterator());
		assertTrue("1.4", query1.isComplete());
		assertTrue("1.5", query1.isPrepared());
		assertTrue("1.6", query2.isComplete());
		assertTrue("1.7", query2.isPrepared());
		assertTrue("1.8", query1.areHooksExecutedProperly());
		assertTrue("1.9", query2.areHooksExecutedProperly());
	}

	public void testPreAndPostCompositeQuery() {
		List items = Arrays.asList("red", "green", "blue");
		PerformHookQuery query1 = new PerformHookQuery();
		PerformHookQuery query2 = new PerformHookQuery();
		IQuery cQuery = PipedQuery.createPipe(query1, query2);
		assertFalse("1.0", query1.isComplete());
		assertFalse("1.1", query1.isPrepared());
		assertFalse("1.2", query2.isComplete());
		assertFalse("1.3", query2.isPrepared());
		cQuery.perform(items.iterator());
		assertTrue("1.4", query1.isComplete());
		assertTrue("1.5", query1.isPrepared());
		assertTrue("1.6", query2.isComplete());
		assertTrue("1.7", query2.isPrepared());
		assertTrue("1.8", query1.areHooksExecutedProperly());
		assertTrue("1.9", query2.areHooksExecutedProperly());
	}

	public void testPreAndPostCompoundQueryFail() {
		List items = Arrays.asList("red", new Object());
		PerformHookQuery query1 = new PerformHookQuery();
		PerformHookQuery query2 = new PerformHookQuery();
		CompoundQuery cQuery = CompoundQuery.createCompoundQuery(new IQuery[] {query1, query2}, true);
		assertFalse("1.0", query1.isComplete());
		assertFalse("1.1", query1.isPrepared());
		assertFalse("1.2", query2.isComplete());
		assertFalse("1.3", query2.isPrepared());
		try {
			cQuery.perform(items.iterator());
			fail("This query is expected to fail");
		} catch (RuntimeException e) {
			// expected
		}
		assertTrue("1.4", query1.isComplete());
		assertTrue("1.5", query1.isPrepared());
		assertTrue("1.6", query2.isComplete());
		assertTrue("1.7", query2.isPrepared());
		assertTrue("1.8", query1.areHooksExecutedProperly());
		assertTrue("1.9", query2.areHooksExecutedProperly());
	}

	public void testPreAndPostCompositeQueryFail() {
		List items = Arrays.asList("red", new Object());
		PerformHookQuery query1 = new PerformHookQuery();
		PerformHookQuery query2 = new PerformHookQuery();
		IQuery cQuery = PipedQuery.createPipe(query1, query2);
		assertFalse("1.0", query1.isComplete());
		assertFalse("1.1", query1.isPrepared());
		assertFalse("1.2", query2.isComplete());
		assertFalse("1.3", query2.isPrepared());
		try {
			cQuery.perform(items.iterator());
			fail("This query is expected to fail");
		} catch (RuntimeException e) {
			// expected
		}
		assertTrue("1.4", query1.isComplete());
		assertTrue("1.5", query1.isPrepared());
		assertFalse("1.6", query2.isComplete()); // This should fail, the second query was never executed
		assertFalse("1.7", query2.isPrepared()); // This should fail, the second query was never executed
		assertTrue("1.8", query1.areHooksExecutedProperly());
		assertTrue("1.9", query2.areHooksExecutedProperly());
	}

	public void testLimitQuery() {
		List items = Arrays.asList("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = new LimitQuery(query, 1);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals("1.0", 1, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains("1.1", collector, "red");
	}

	public void testLimitQuery2() {
		List items = Arrays.asList("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = new LimitQuery(query, 2);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals("1.0", 2, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains("1.1", collector, "red");
		AbstractProvisioningTest.assertContains("1.2", collector, "green");
	}

	public void testLimitQuery3() {
		List items = Arrays.asList("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = new LimitQuery(query, 3);
		IQueryResult collector = limitQuery.perform(items.iterator());
		assertEquals("1.0", 3, AbstractProvisioningTest.queryResultSize(collector));
		AbstractProvisioningTest.assertContains("1.1", collector, "red");
		AbstractProvisioningTest.assertContains("1.2", collector, "green");
		AbstractProvisioningTest.assertContains("1.3", collector, "blue");
	}

	public void testLimitQuery0() {
		List items = Arrays.asList("red", "green", "blue");
		IQuery query = new AnyStringQuery();
		IQuery limitQuery = new LimitQuery(query, 0);
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
	//		IQuery limitQuery = new LimitQuery(pipedQuery, 1);
	//		Collector collector = limitQuery.perform(items.iterator(), new Collector());
	//		Collection result = collector.toCollection();
	//		assertEquals("1.0", 1, result.size());
	//		assertTrue("1.1", result.contains("red"));
	//	}

}
