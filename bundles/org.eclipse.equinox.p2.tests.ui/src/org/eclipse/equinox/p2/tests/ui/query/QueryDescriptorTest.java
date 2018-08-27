/******************************************************************************* 
* Copyright (c) 2009, 2017 EclipseSource and others.
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
package org.eclipse.equinox.p2.tests.ui.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ElementQueryDescriptor;
import org.eclipse.equinox.internal.p2.ui.ElementWrapper;
import org.eclipse.equinox.p2.query.*;
import org.junit.Test;

/**
 * Tests the Query Descriptor
 */
public class QueryDescriptorTest {

	class SimpleQueryable implements IQueryable<String> {
		List<String> elements = Arrays.asList(new String[] { "a", "b", "c", "d", "e" });

		@Override
		public IQueryResult<String> query(IQuery<String> query, IProgressMonitor monitor) {
			return query.perform(elements.iterator());
		}
	}

	class WrappedString {
		String string;

		WrappedString(String string) {
			this.string = string;
		}

		@Override
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

		@Override
		public int hashCode() {
			return string.hashCode();
		}
	}

	class StringWrapper extends ElementWrapper {
		@Override
		protected Object wrap(Object item) {
			return new WrappedString((String) item);
		}
	}

	class SimpleMatchQuery extends MatchQuery<Object> {

		@Override
		@Deprecated
		public boolean isMatch(Object candidate) {
			if (candidate == "a" || candidate == "b")
				return true;
			return false;
		}
	}

	class SimpleMatchQuery2 extends MatchQuery<Object> {
		@Override
		@Deprecated
		public boolean isMatch(Object candidate) {
			if (candidate == "b" || candidate == "c")
				return true;
			return false;
		}
	}

	@Test
	public void testSimpleDescriptorWithWrapper() {
		ElementQueryDescriptor eqDescriptor = new ElementQueryDescriptor(new SimpleQueryable(), new SimpleMatchQuery(),
				new Collector<>(), new StringWrapper());
		Collection<?> collection = eqDescriptor.performQuery(null);
		assertEquals("1.0", 2, collection.size());
		assertTrue("1.1", collection.contains(new WrappedString("a")));
		assertTrue("1.1", collection.contains(new WrappedString("b")));
	}

	@Test
	public void testSimpleDescriptorWithoutWrapper() {
		ElementQueryDescriptor eqDescriptor = new ElementQueryDescriptor(new SimpleQueryable(), new SimpleMatchQuery(),
				new Collector<>());
		Collection<?> collection = eqDescriptor.performQuery(null);
		assertEquals("1.0", 2, collection.size());
		assertTrue("1.1", collection.contains("a"));
		assertTrue("1.1", collection.contains("b"));
	}

	@Test
	public void testCompoundDescriptorAND() {
		IQuery<Object> query = QueryUtil.createCompoundQuery(new SimpleMatchQuery(), new SimpleMatchQuery2(), true);
		ElementQueryDescriptor eqDescriptor = new ElementQueryDescriptor(new SimpleQueryable(), query,
				new Collector<>(), new StringWrapper());
		Collection<?> collection = eqDescriptor.performQuery(null);
		assertEquals("1.0", 1, collection.size());
		assertTrue("1.1", collection.contains(new WrappedString("b")));
	}

	@Test
	public void testCompoundDescriptorOR() {
		IQuery<Object> query = QueryUtil.createCompoundQuery(new SimpleMatchQuery(), new SimpleMatchQuery2(), false);
		ElementQueryDescriptor eqDescriptor = new ElementQueryDescriptor(new SimpleQueryable(), query,
				new Collector<>(), new StringWrapper());
		Collection<?> collection = eqDescriptor.performQuery(null);
		assertEquals("1.0", 3, collection.size());
		assertTrue("1.1", collection.contains(new WrappedString("a")));
		assertTrue("1.1", collection.contains(new WrappedString("b")));
		assertTrue("1.1", collection.contains(new WrappedString("c")));
	}
}
