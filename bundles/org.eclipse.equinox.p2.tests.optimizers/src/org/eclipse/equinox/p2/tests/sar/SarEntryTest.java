/*******************************************************************************
 * Copyright (c) 2007, 2018 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.sar;

import static org.junit.Assert.assertEquals;

import org.eclipse.equinox.internal.p2.sar.SarEntry;
import org.junit.Test;

public class SarEntryTest {

	@Test
	public void testJavaToDosTimeAndBack() {
		final long minute = 1000l * 60l;
		long now = (System.currentTimeMillis() / minute) * minute;
		long dos = SarEntry.javaToDosTime(now);
		assertEquals(now, SarEntry.dosToJavaTime(dos));
	}

}
