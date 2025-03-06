/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.tests.core;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.eclipse.equinox.internal.p2.core.helpers.StringHelper;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.junit.Test;

public class StringHelperTest {
	@Test
	public void testSimilarBehavior() {
		String[] a1 = AbstractPublisherAction.getArrayFromString("foo, bar, baz ,,, zaz", ",");
		String[] a2 = StringHelper.getArrayFromString("foo, bar, baz ,,, zaz", ',');
		assertTrue("1.0", Arrays.equals(a1, a2));
		a1 = AbstractPublisherAction.getArrayFromString("foo   bar baz, ,, zaz", " ");
		a2 = StringHelper.getArrayFromString("foo   bar baz, ,, zaz", ' ');
		assertTrue("1.1", Arrays.equals(a1, a2));
		a1 = AbstractPublisherAction.getArrayFromString("   ", " ");
		a2 = StringHelper.getArrayFromString("   ", ' ');
		assertTrue("1.2", Arrays.equals(a1, a2));
		a1 = AbstractPublisherAction.getArrayFromString("", ",");
		a2 = StringHelper.getArrayFromString("", ',');
		assertTrue("1.3", Arrays.equals(a1, a2));
		a1 = AbstractPublisherAction.getArrayFromString(null, ",");
		a2 = StringHelper.getArrayFromString(null, ',');
		assertTrue("1.4", Arrays.equals(a1, a2));
	}

	@Test
	public void testPerformance() throws Exception {
		String[] strings = new String[5];
		StringBuilder inputBld = new StringBuilder();
		for (int idx = 0; idx < 5; ++idx) {
			if (idx > 0) {
				inputBld.append(',');
			}
			for (int c = 11; c > idx * 2; --c) {
				inputBld.append((char) ('a' + c));
			}
			strings[idx] = inputBld.toString();
		}

		long ts = System.currentTimeMillis();
		for (int cnt = 0; cnt < 1000000; ++cnt) {
			for (int idx = 0; idx < 5; ++idx) {
				AbstractPublisherAction.getArrayFromString(strings[idx], ",");
			}
		}
		long apaTime = System.currentTimeMillis() - ts;

		ts = System.currentTimeMillis();
		for (int cnt = 0; cnt < 1000000; ++cnt) {
			for (int idx = 0; idx < 5; ++idx) {
				StringHelper.getArrayFromString(strings[idx], ',');
			}
		}
		long shTime = System.currentTimeMillis() - ts;
		System.out.println("Ratio: " + (double) shTime / (double) apaTime);
	}
}
