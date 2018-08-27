/*******************************************************************************
 *  Copyright (c) 2010, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.metadata.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.ExpressionParseException;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IFilterExpression;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

public class FilterTest {
	@Test
	public void testComparable() throws Exception {
		IFilterExpression f1 = ExpressionUtil.parseLDAP("(comparable=42)");
		Object comp;
		Map<String, Object> hash = new HashMap<>();

		comp = new SampleComparable("42");
		hash.put("comparable", comp);
		assertTrue("does not match filter", f1.match(hash));
		assertTrue("does not match filter", f1.match(new DictionaryServiceReference(hash)));

		comp = Long.valueOf(42);
		hash.put("comparable", comp);
		assertTrue("does not match filter", f1.match(hash));
		assertTrue("does not match filter", f1.match(new DictionaryServiceReference(hash)));

		IFilterExpression f2 = ExpressionUtil.parseLDAP("(comparable=42)");
		hash = new Hashtable<>();

		comp = new SampleComparable("42");
		hash.put("comparable", comp);
		assertTrue("does not match filter", f2.match(hash));
		assertTrue("does not match filter", f2.match(new DictionaryServiceReference(hash)));

		comp = Long.valueOf(42);
		hash.put("comparable", comp);
		assertTrue("does not match filter", f2.match(hash));
		assertTrue("does not match filter", f2.match(new DictionaryServiceReference(hash)));

		assertEquals("not equal", f1, f2);
	}

	@Test
	public void testFilterEquality() {
		Filter f1 = ExpressionUtil.parseLDAP("( a = bedroom  )");
		Filter f2 = ExpressionUtil.parseLDAP(" (a= bedroom  ) ");
		assertEquals("not equal", "(a= bedroom  )", f1.toString());
		assertEquals("not equal", "(a= bedroom  )", f2.toString());
		assertEquals("not equal", f1, f2);
		assertEquals("not equal", f2, f1);
		assertEquals("not equal", f1.hashCode(), f2.hashCode());

		f1 = ExpressionUtil.parseLDAP("(status =\\28o*\\5c\\29\\2a)");
		assertEquals("not equal", "(status=\\28o*\\5c\\29\\2a)", f1.toString());

		f1 = ExpressionUtil.parseLDAP("(|(a=1)(&(a=1)(b=1)))");
		f2 = ExpressionUtil.parseLDAP("(a=1)");

		f1 = ExpressionUtil.parseLDAP("(|(&(os=macos)(ws=cocoa)(arch=x86))(&(ws=cocoa)(os=macos)(arch=ppc)))");
		f2 = ExpressionUtil.parseLDAP("(&(os=macos)(ws=cocoa)(|(arch=x86)(arch=ppc)))");
		assertEquals("not equal: f1:" + f1.toString() + ", f2:" + f1.toString(), f1, f2);

		f1 = ExpressionUtil.parseLDAP("(&(|(x=a)(y=b)(z=a))(|(x=a)(y=b)(z=b)))");
		f2 = ExpressionUtil.parseLDAP("(|(x=a)(y=b)(&(z=a)(z=b)))");
		assertEquals("not equal: f1:" + f1.toString() + ", f2:" + f1.toString(), f1, f2);

		f1 = ExpressionUtil.parseLDAP("(&(a=1)(|(a=1)(b=1)))");
		f2 = ExpressionUtil.parseLDAP("(a=1)");

		f1 = ExpressionUtil.parseLDAP("(|(a=1)(&(a=1)(b=1)))");
		f2 = ExpressionUtil.parseLDAP("(a=1)");
		assertEquals("not equal: f1:" + f1.toString() + ", f2:" + f1.toString(), f1, f2);
	}

	@Test
	public void testFilterMatching() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("room", "bedroom");
		props.put("channel", Integer.valueOf(34));
		props.put("status", "(on\\)*");
		props.put("max record time", Long.valueOf(150));
		props.put("canrecord", "true(x)");
		props.put("shortvalue", Short.valueOf((short) 1000));
		props.put("bytevalue", Byte.valueOf((byte) 10));
		props.put("floatvalue", Float.valueOf(1.01f));
		props.put("doublevalue", Double.valueOf(2.01));
		props.put("charvalue", Character.valueOf('A'));
		props.put("booleanvalue", Boolean.FALSE);
		props.put("listvalue", Arrays.asList(1, 2, 3));
		props.put("versionlistvalue", Arrays.asList(Version.create("1"), Version.create("2"), Version.create("3")));
		props.put("weirdvalue", new Hashtable<>());
		props.put("bigintvalue", new BigInteger("4123456"));
		props.put("bigdecvalue", new BigDecimal("4.123456"));

		assertMatch("(room=*)", props);
		assertNoMatch("(rooom=*)", props);
		assertMatch("(room=bedroom)", props);
		assertMatch("(room~= B E D R O O M )", props);
		assertNoMatch("(room=abc)", props);
		assertMatch(" ( room >=aaaa)", props);
		assertNoMatch("(room <=aaaa)", props);
		assertMatch("  ( room =b*) ", props);
		assertMatch("  ( room =*m) ", props);
		assertMatch("(room=bed*room)", props);
		assertMatch("  ( room =b*oo*m) ", props);
		assertMatch("  ( room =*b*oo*m*) ", props);
		assertNoMatch("  ( room =b*b*  *m*) ", props);
		assertMatch("  (& (room =bedroom) (channel = 34))", props);
		assertNoMatch("  (&  (room =b*)  (room =*x) (channel=34))", props);
		assertMatch("(| (room =bed*)(channel=222)) ", props);
		assertMatch("(| (room =boom*)(channel=34)) ", props);
		assertMatch("  (! (room =ab*b*oo*m*) ) ", props);
		assertMatch("  (status =\\(o*\\\\\\)\\*) ", props);
		assertMatch("  (status =\\28o*\\5c\\29\\2a) ", props);
		assertMatch("  (status =\\28o*\\5C\\29\\2A) ", props);
		assertMatch("  (canRecord =true\\(x\\)) ", props);
		assertMatch("(max Record Time <=150) ", props);
		assertMatch("(shortValue >= 100) ", props);
		assertMatch("  (  &  (  byteValue <= 100  )  (  byteValue >= 10  )  )  ", props);
		assertMatch("(bigIntValue = 4123456) ", props);
		assertMatch("(bigDecValue = 4.123456) ", props);
		assertMatch("(floatValue >= 1.0) ", props);
		assertMatch("(doubleValue <= 2.011) ", props);
		assertMatch("(charValue ~= a) ", props);
		assertMatch("(booleanValue = false) ", props);
		assertMatch("(listvalue>=0)", props);
		assertMatch("(listvalue=3)", props);
		assertMatch("(!(listvalue>=4))", props);
		assertMatch("(versionlistvalue>=0)", props);
		assertMatch("(versionlistvalue=3)", props);
		assertMatch("(!(versionlistvalue>=4))", props);
		assertMatch("(& (| (room =d*m) (room =bed*) (room=abc)) (! (channel=999)))", props);
		assertNoMatch("(room=bedroom)", null);
		assertNoMatch("(weirdValue = 100) ", props);
	}

	@Test
	public void testFilterParserErrors() {
		assertParseError("()");
		assertParseError("(=foo)");
		assertParseError("(");
		assertParseError("(abc = ))");
		assertParseError("(& (abc = xyz) (& (345))");
		assertParseError("  (room = b**oo!*m*) ) ");
		assertParseError("  (room = b**oo)*m*) ) ");
		assertParseError("  (room = *=b**oo*m*) ) ");
		assertParseError("  (room = =b**oo*m*) ) ");
	}

	private void assertMatch(String query, Dictionary<String, Object> props) {
		expectMatch(query, props, true);
	}

	private void assertNoMatch(String query, Dictionary<String, Object> props) {
		expectMatch(query, props, false);
	}

	private void expectMatch(String query, Dictionary<String, Object> props, boolean match) {
		Filter f = ExpressionUtil.parseLDAP(query);

		// TODO Doing raw conversion here for simplicity; could convert to Dictionary<String, ?>
		// but the filter impl must still handle cases where non String keys are used.
		assertEquals(match, f.match(props));

		ServiceReference ref = new DictionaryServiceReference((Map<String, ? extends Object>) props);
		assertEquals(match, f.match(ref));
	}

	private void assertParseError(String query) {
		try {
			ExpressionUtil.parseLDAP(query);
			fail("expected exception");
		} catch (ExpressionParseException e) {
			// Pass
		}
	}

	private static class SampleComparable implements Comparable<SampleComparable> {
		private int value = -1;

		public SampleComparable(String value) {
			this.value = Integer.parseInt(value);
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof SampleComparable && value == ((SampleComparable) o).value;
		}

		@Override
		public int compareTo(SampleComparable o) {
			return value - o.value;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}

	private static class DictionaryServiceReference implements ServiceReference {
		private final Map<String, ? extends Object> dictionary;

		private final String[] keys;

		DictionaryServiceReference(Map<String, ? extends Object> dictionary) {
			if (dictionary == null) {
				this.dictionary = null;
				this.keys = new String[] {};
				return;
			}
			this.dictionary = dictionary;
			List<String> keyList = new ArrayList<>(dictionary.size());
			for (Iterator<String> e = dictionary.keySet().iterator(); e.hasNext();) {
				String key = e.next();
				for (Iterator<String> i = keyList.iterator(); i.hasNext();) {
					if (key.equalsIgnoreCase(i.next())) {
						throw new IllegalArgumentException();
					}
				}
				keyList.add(key);
			}
			this.keys = keyList.toArray(new String[keyList.size()]);
		}

		@Override
		public int compareTo(Object reference) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Bundle getBundle() {
			return null;
		}

		@Override
		public Object getProperty(String k) {
			for (String key : keys) {
				if (key.equalsIgnoreCase(k)) {
					return dictionary.get(key);
				}
			}
			return null;
		}

		@Override
		public String[] getPropertyKeys() {
			return keys.clone();
		}

		@Override
		public Bundle[] getUsingBundles() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isAssignableTo(Bundle bundle, String className) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Dictionary<String, Object> getProperties() {
			return new Hashtable<>(dictionary);
		}
	}
}
