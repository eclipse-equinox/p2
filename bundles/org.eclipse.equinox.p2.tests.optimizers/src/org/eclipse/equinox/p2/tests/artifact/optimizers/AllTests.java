/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
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

import junit.framework.*;

/**
 * Performs all automated director tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(JBDiffStepTest.class);
		suite.addTestSuite(JBDiffZipStepTest.class);
		suite.addTestSuite(Pack200OptimizerTest.class);
		suite.addTestSuite(JarDeltaOptimizerTest.class);
		suite.addTestSuite(Bug209233Test.class);
		return suite;
	}
}
