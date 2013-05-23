/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import junit.framework.*;

/**
 * Performs all metadata tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(ArtifactKeyParsingTest.class);
		suite.addTestSuite(FragmentMethodTest.class);
		suite.addTestSuite(FragmentTest.class);
		suite.addTestSuite(InstallableUnitTest.class);
		suite.addTestSuite(IUPersistenceTest.class);
		suite.addTestSuite(LatestIUTest.class);
		suite.addTestSuite(LicenseTest.class);
		suite.addTestSuite(MultipleIUAndFragmentTest.class);
		suite.addTestSuite(PersistNegation.class);
		suite.addTestSuite(PersistFragment.class);
		suite.addTestSuite(ProvidedCapabilityTest.class);
		suite.addTestSuite(RequirementToString.class);
		return suite;
	}

}
