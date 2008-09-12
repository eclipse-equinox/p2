/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.optimizers;

import junit.framework.*;

/**
 * @since 1.0
 */
public class AutomatedTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AutomatedTests.class.getName());
		suite.addTest(org.eclipse.equinox.p2.tests.artifact.optimizers.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.artifact.processors.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.sar.AllTests.suite());
		return suite;
	}

}
