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

import java.util.*;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.provisional.p2.query.*;

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
		boolean prepared = false;
		boolean complete = false;

		protected void prepareToPerform() {
			prepared = true;
		}

		protected void performComplete() {
			complete = true;
		}

		public boolean isMatch(Object candidate) {
			if (!(candidate instanceof String))
				throw new RuntimeException("Exception intentionally thrown by test");
			return candidate instanceof String;
		}
	}

	/**
	 * A collector that only accepts the first element and then short-circuits.
	 */
	static class ShortCircuitCollector extends Collector {
		@Override
		public boolean accept(Object object) {
			super.accept(object);
			return false;
		}
	}

	/**
	 * Tests a simple perform where all items match.
	 */
	public void testPerformSimple() {
		List items = Arrays.asList("red", "green", "blue");
		Query query = new AnyStringQuery();
		Collector collector = new Collector();
		query.perform(items.iterator(), collector);
		Collection result = collector.toCollection();
		assertEquals("1.0", 3, result.size());
		assertTrue("1.1", result.contains("red"));
		assertTrue("1.2", result.contains("green"));
		assertTrue("1.3", result.contains("blue"));
	}

	/**
	 * Tests a perform where only some items match.
	 */
	public void testPerformSomeMatches() {
		List items = Arrays.asList(new Object(), "green", new Object());
		Query query = new AnyStringQuery();
		Collector collector = new Collector();
		query.perform(items.iterator(), collector);
		Collection result = collector.toCollection();
		assertEquals("1.0", 1, result.size());
		assertTrue("1.1", result.contains("green"));
	}

	public void testPerformHooks() {
		List items = Arrays.asList("red", "green", "blue");
		PerformHookQuery query = new PerformHookQuery();
		Collector collector = new Collector();
		assertFalse("1.0", query.complete);
		assertFalse("1.1", query.prepared);
		query.perform(items.iterator(), collector);
		assertTrue("1.2", query.complete);
		assertTrue("1.3", query.prepared);
	}

	public void testPerformHooksOnQueryFail() {
		List items = Arrays.asList("red", new Object());
		PerformHookQuery query = new PerformHookQuery();
		Collector collector = new Collector();
		assertFalse("1.0", query.complete);
		assertFalse("1.1", query.prepared);
		try {
			query.perform(items.iterator(), collector);
		} catch (RuntimeException e) {
			// expected
		}
		assertTrue("1.2", query.complete);
		assertTrue("1.3", query.prepared);
	}

	/**
	 * Tests a perform where the collector decides to short-circuit the query.
	 */
	public void testShortCircuit() {
		List items = Arrays.asList("red", "green", "blue");
		Query query = new AnyStringQuery();
		Collector collector = new ShortCircuitCollector();
		query.perform(items.iterator(), collector);
		Collection result = collector.toCollection();
		assertEquals("1.0", 1, result.size());
		assertTrue("1.1", result.contains("red"));
	}

}
