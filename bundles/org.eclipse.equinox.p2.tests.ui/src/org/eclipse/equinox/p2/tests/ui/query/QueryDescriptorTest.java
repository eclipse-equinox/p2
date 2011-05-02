/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import java.util.*;
import junit.framework.TestCase;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ElementQueryDescriptor;
import org.eclipse.equinox.internal.p2.ui.ElementWrapper;
import org.eclipse.equinox.p2.query.*;

/**
 * Tests the Query Descriptor
 */
public class QueryDescriptorTest extends TestCase {

	class SimpleQueryable implements IQueryable {
		List elements = Arrays.asList(new String[] {"a", "b", "c", "d", "e"});

		public IQueryResult query(IQuery query, IProgressMonitor monitor) {
			return query.perform(elements.iterator());
		}
	}

	class WrappedString {
		String string;

		WrappedString(String string) {
			this.string = string;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof WrappedString))
				return false;
			WrappedString other = (WrappedString) obj;
			return this.string.equals(other.string);
		}

		public int hashCode() {
			return string.hashCode();
		}
	}

	class StringWrapper extends ElementWrapper {
		protected Object wrap(Object item) {
			return new WrappedString((String) item);
		}
	}

	class SimpleMatchQuery extends MatchQuery {

		/* (non-Javadoc)
		 * @see org.eclipse.equinox.internal.provisional.p2.query.MatchQuery#isMatch(java.lang.Object)
		 */
		public boolean isMatch(Object candidate) {
			if (candidate == "a" || candidate == "b")
				return true;
			return false;
		}
	}

	class SimpleMatchQuery2 extends MatchQuery {
		/* (non-Javadoc)
		 * @see org.eclipse.equinox.internal.provisional.p2.query.MatchQuery#isMatch(java.lang.Object)
		 */
		public boolean isMatch(Object candidate) {
			if (candidate == "b" || candidate == "c")
				return true;
			return false;
		}
	}

	public void testSimpleDescriptorWithWrapper() {
		ElementQueryDescriptor eqDescriptor = new ElementQueryDescriptor(new SimpleQueryable(), new SimpleMatchQuery(), new Collector(), new StringWrapper());
		Collection collection = eqDescriptor.performQuery(null);
		assertEquals("1.0", 2, collection.size());
		assertTrue("1.1", collection.contains(new WrappedString("a")));
		assertTrue("1.1", collection.contains(new WrappedString("b")));
	}

	public void testSimpleDescriptorWithoutWrapper() {
		ElementQueryDescriptor eqDescriptor = new ElementQueryDescriptor(new SimpleQueryable(), new SimpleMatchQuery(), new Collector());
		Collection collection = eqDescriptor.performQuery(null);
		assertEquals("1.0", 2, collection.size());
		assertTrue("1.1", collection.contains("a"));
		assertTrue("1.1", collection.contains("b"));
	}

	public void testCompoundDescriptorAND() {
		IQuery query = QueryUtil.createCompoundQuery(new SimpleMatchQuery(), new SimpleMatchQuery2(), true);
		ElementQueryDescriptor eqDescriptor = new ElementQueryDescriptor(new SimpleQueryable(), query, new Collector(), new StringWrapper());
		Collection collection = eqDescriptor.performQuery(null);
		assertEquals("1.0", 1, collection.size());
		assertTrue("1.1", collection.contains(new WrappedString("b")));
	}

	public void testCompoundDescriptorOR() {
		IQuery query = QueryUtil.createCompoundQuery(new SimpleMatchQuery(), new SimpleMatchQuery2(), false);
		ElementQueryDescriptor eqDescriptor = new ElementQueryDescriptor(new SimpleQueryable(), query, new Collector(), new StringWrapper());
		Collection collection = eqDescriptor.performQuery(null);
		assertEquals("1.0", 3, collection.size());
		assertTrue("1.1", collection.contains(new WrappedString("a")));
		assertTrue("1.1", collection.contains(new WrappedString("b")));
		assertTrue("1.1", collection.contains(new WrappedString("c")));
	}
}
