/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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