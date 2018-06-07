/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
