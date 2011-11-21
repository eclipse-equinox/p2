/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import junit.framework.*;

/**
 * Performs all automated artifact repository tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(ArtifactLockingTest.class);
		suite.addTestSuite(ArtifactOutputStreamTest.class);
		suite.addTestSuite(ArtifactRepositoryManagerTest.class);
		suite.addTestSuite(ArtifactRepositoryMissingSizeData.class);
		suite.addTestSuite(ArtifactRepositoryWithReferenceDescriptors.class);
		suite.addTestSuite(BatchExecuteArtifactRepositoryTest.class);
		suite.addTestSuite(Bug252308.class);
		suite.addTestSuite(Bug265577.class);
		suite.addTestSuite(Bug351944.class);
		suite.addTestSuite(CompositeArtifactRepositoryTest.class);
		suite.addTestSuite(CorruptedJar.class);
		suite.addTestSuite(FoldersRepositoryTest.class);
		suite.addTestSuite(JarURLArtifactRepositoryTest.class);
		suite.addTestSuite(LocationTest.class);
		suite.addTestSuite(MD5Tests.class);
		suite.addTestSuite(MirrorSelectorTest.class);
		suite.addTestSuite(MirrorRequestTest.class);
		suite.addTestSuite(SimpleArtifactRepositoryTest.class);
		suite.addTestSuite(TransferTest.class);
		return suite;
	}

}