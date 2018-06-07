/*******************************************************************************
 * Copyright (c) 2007, 2018 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.optimizers;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all automated director tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({Pack200OptimizerTest.class, JarDeltaOptimizerTest.class})
public class AllTests {
	//test suite
}
