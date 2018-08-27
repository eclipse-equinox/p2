/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.mirror;

import junit.framework.*;

/**
 * Performs all automated mirror repository tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(ArtifactMirrorApplicationTest.class);
		suite.addTestSuite(MetadataMirrorApplicationTest.class);
		suite.addTestSuite(ArtifactRepositoryCleanupTest.class);
		suite.addTestSuite(MetadataRepositoryCleanupTest.class);
		suite.addTest(new JUnit4TestAdapter(NewMirrorApplicationArtifactTest.class));
		suite.addTestSuite(NewMirrorApplicationMetadataTest.class);
		suite.addTest(new JUnit4TestAdapter(MirrorApplicationTest.class));
		return suite;
	}

}