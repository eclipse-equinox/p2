/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.sar;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.sar.SarEntry;

public class SarEntryTest extends TestCase {

	public void testJavaToDosTimeAndBack() {
		final long minute = 1000l * 60l;
		long now = (System.currentTimeMillis() / minute) * minute;
		long dos = SarEntry.javaToDosTime(now);
		assertEquals(now, SarEntry.dosToJavaTime(dos));
	}

}
