/*******************************************************************************
 * Copyright (c) 2015, 2017 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.util.Collection;
import org.hamcrest.*;

public class AdditionalCoreMatchers {

	/**
	 * Creates a matcher matching any collection with the given size.
	 *
	 * @see CoreMatchers#hasItem(Matcher)
	 */
	public static <T> Matcher<Collection<? extends T>> hasSize(final int size) {
		return new TypeSafeMatcher<Collection<? extends T>>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("a collection with size " + size);
			}

			@Override
			protected boolean matchesSafely(Collection<? extends T> item) {
				return item.size() == size;
			}
		};
	}

}
