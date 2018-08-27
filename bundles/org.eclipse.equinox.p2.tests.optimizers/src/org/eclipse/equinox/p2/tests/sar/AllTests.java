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

import junit.framework.TestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all sar tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({DirectByteArrayOutputStreamTest.class, SarTest.class, SarEntryTest.class})
public class AllTests extends TestCase {
	// test suite
}
