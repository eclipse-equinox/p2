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
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;

/**
 * This tests both Compound and Composite queries
 * 
 */
public class AggregateQueryTest extends TestCase {

	public List getABCDE() {
		return Arrays.asList("A", "B", "C", "D", "E");
	}

	public List get123() {
		return Arrays.asList("1", "2", "3");
	}

	public void testEmptyCompositeQuery() {
		PipedQuery query = new PipedQuery(new Query[0]);
		query.perform(getABCDE().iterator(), new Collector());
		// We should not throw an exception.  No guarantee on what perform
		// will return in this case
	}

	public void testSymmetry() {
		Query getLatest = new ContextQuery() {

			public Collector perform(Iterator iterator, Collector result) {
				List list = new ArrayList();
				while (iterator.hasNext()) {
					list.add(iterator.next());
				}
				Collections.sort(list);
				result.accept(list.get(list.size() - 1));
				return result;
			}
		};

		Query getAllBut3 = new ContextQuery() {

			public Collector perform(Iterator iterator, Collector result) {
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (!o.equals("3"))
						result.accept(o);
				}
				return result;
			}
		};

		CompoundQuery compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {getLatest, getAllBut3}, true);
		Collector result = compoundQuery.perform(get123().iterator(), new Collector());
		assertEquals(0, result.size());

		compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {getAllBut3, getLatest}, true);
		result = compoundQuery.perform(get123().iterator(), new Collector());
		assertEquals(0, result.size());

		compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {getLatest, getAllBut3}, false);
		result = compoundQuery.perform(get123().iterator(), new Collector());
		assertEquals(3, result.size());

		compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {getAllBut3, getLatest}, false);
		result = compoundQuery.perform(get123().iterator(), new Collector());
		assertEquals(3, result.size());

	}

	/**
	 * The CompositeQuery should not support symmetry.
	 * This method tests that
	 */
	public void testNonSymmetry() {
		Query getLatest = new ContextQuery() {

			public Collector perform(Iterator iterator, Collector result) {
				List list = new ArrayList();
				while (iterator.hasNext()) {
					list.add(iterator.next());
				}
				Collections.sort(list);
				result.accept(list.get(list.size() - 1));
				return result;
			}
		};

		Query getAllBut3 = new ContextQuery() {

			public Collector perform(Iterator iterator, Collector result) {
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (!o.equals("3"))
						result.accept(o);
				}
				return result;
			}
		};

		PipedQuery compoundQuery = new PipedQuery(new Query[] {getLatest, getAllBut3});
		Collector result = compoundQuery.perform(get123().iterator(), new Collector());
		assertEquals(0, result.size());

		compoundQuery = new PipedQuery(new Query[] {getAllBut3, getLatest});
		result = compoundQuery.perform(get123().iterator(), new Collector());
		assertEquals(1, result.size());
		assertEquals("2", result.toCollection().iterator().next());

	}

	public void testCompoundAllMatchQueries() {
		Query A = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				return false;
			}
		};
		Query B = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				return false;
			}
		};
		Query C = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				return false;
			}
		};
		CompoundQuery compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {A, B, C}, true);
		assertTrue("1.0", compoundQuery instanceof IMatchQuery);
		assertEquals("1.1", 3, compoundQuery.getQueries().length);
		assertEquals("1.2", A, compoundQuery.getQueries()[0]);
		assertEquals("1.3", B, compoundQuery.getQueries()[1]);
		assertEquals("1.4", C, compoundQuery.getQueries()[2]);
	}

	public void testCompoundSomeMatchQueries() {
		Query A = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				return false;
			}
		};
		Query B = new ContextQuery() {
			public Collector perform(Iterator iterator, Collector result) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		Query C = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				return false;
			}
		};
		CompoundQuery compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {A, B, C}, true);
		assertTrue("1.0", !(compoundQuery instanceof IMatchQuery));
		assertEquals("1.1", 3, compoundQuery.getQueries().length);
		assertEquals("1.2", A, compoundQuery.getQueries()[0]);
		assertEquals("1.3", B, compoundQuery.getQueries()[1]);
		assertEquals("1.4", C, compoundQuery.getQueries()[2]);
	}

	public void testCompoundNoMatchQueries() {
		Query A = new ContextQuery() {
			public Collector perform(Iterator iterator, Collector result) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		Query B = new ContextQuery() {
			public Collector perform(Iterator iterator, Collector result) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		Query C = new ContextQuery() {
			public Collector perform(Iterator iterator, Collector result) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		CompoundQuery compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {A, B, C}, true);
		assertTrue("1.0", !(compoundQuery instanceof IMatchQuery));
		assertEquals("1.1", 3, compoundQuery.getQueries().length);
		assertEquals("1.2", A, compoundQuery.getQueries()[0]);
		assertEquals("1.3", B, compoundQuery.getQueries()[1]);
		assertEquals("1.4", C, compoundQuery.getQueries()[2]);
	}

	public void testIntersection() {
		Query ABC = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (candidate.equals("A") || candidate.equals("B") || candidate.equals("C"))
					return true;
				return false;
			}
		};

		Query BCDE = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (candidate.equals("B") || candidate.equals("C") || candidate.equals("D") || candidate.equals("E"))
					return true;
				return false;
			}
		};

		CompoundQuery compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {ABC, BCDE}, true);
		Collector result = compoundQuery.perform(getABCDE().iterator(), new Collector());
		assertEquals("1.0", result.size(), 2);
		assertTrue("1.1", result.toCollection().contains("B"));
		assertTrue("1.2", result.toCollection().contains("C"));
	}

	public void testIntersection2() {
		Query ABC = new ContextQuery() {
			public Collector perform(Iterator iterator, Collector result) {
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (o.equals("A") || o.equals("B") || o.equals("C"))
						result.accept(o);
				}
				return result;
			}
		};

		Query BCDE = new ContextQuery() {
			public Collector perform(Iterator iterator, Collector result) {
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (o.equals("B") || o.equals("C") || o.equals("D") || o.equals("E"))
						result.accept(o);
				}
				return result;
			}
		};

		CompoundQuery compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {ABC, BCDE}, true);
		Collector result = compoundQuery.perform(getABCDE().iterator(), new Collector());
		assertEquals("1.0", result.size(), 2);
		assertTrue("1.1", result.toCollection().contains("B"));
		assertTrue("1.2", result.toCollection().contains("C"));
	}

	public void testUnion() {
		Query ABC = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (candidate.equals("A") || candidate.equals("B") || candidate.equals("C"))
					return true;
				return false;
			}
		};

		Query BCDE = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (candidate.equals("B") || candidate.equals("C") || candidate.equals("D") || candidate.equals("E"))
					return true;
				return false;
			}
		};

		CompoundQuery compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {ABC, BCDE}, false);
		Collector result = compoundQuery.perform(getABCDE().iterator(), new Collector());
		assertEquals("1.0", result.size(), 5);
		assertTrue("1.1", result.toCollection().contains("A"));
		assertTrue("1.2", result.toCollection().contains("B"));
		assertTrue("1.3", result.toCollection().contains("C"));
		assertTrue("1.4", result.toCollection().contains("D"));
		assertTrue("1.5", result.toCollection().contains("E"));
	}

	public void testUnion2() {
		Query ABC = new ContextQuery() {
			public Collector perform(Iterator iterator, Collector result) {
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (o.equals("A") || o.equals("B") || o.equals("C"))
						result.accept(o);
				}
				return result;
			}
		};

		Query BCDE = new ContextQuery() {
			public Collector perform(Iterator iterator, Collector result) {
				while (iterator.hasNext()) {
					Object o = iterator.next();
					if (o.equals("B") || o.equals("C") || o.equals("D") || o.equals("E"))
						result.accept(o);
				}
				return result;
			}
		};

		CompoundQuery compoundQuery = CompoundQuery.createCompoundQuery(new Query[] {ABC, BCDE}, false);
		Collector result = compoundQuery.perform(getABCDE().iterator(), new Collector());
		assertEquals("1.0", result.size(), 5);
		assertTrue("1.1", result.toCollection().contains("A"));
		assertTrue("1.2", result.toCollection().contains("B"));
		assertTrue("1.3", result.toCollection().contains("C"));
		assertTrue("1.4", result.toCollection().contains("D"));
		assertTrue("1.5", result.toCollection().contains("E"));
	}
}
