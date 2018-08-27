/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.optimizers;

import junit.framework.TestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @since 1.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({org.eclipse.equinox.p2.tests.artifact.optimizers.AllTests.class, org.eclipse.equinox.p2.tests.artifact.processors.AllTests.class, org.eclipse.equinox.p2.tests.sar.AllTests.class})
public class AutomatedTests extends TestCase {
	// test suite
}
