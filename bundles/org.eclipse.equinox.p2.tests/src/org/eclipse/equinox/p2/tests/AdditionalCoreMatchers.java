/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

			public void describeTo(Description description) {
				description.appendText("a collection with size " + size);
			}

			protected boolean matchesSafely(Collection<? extends T> item) {
				return item.size() == size;
			}
		};
	}

}
