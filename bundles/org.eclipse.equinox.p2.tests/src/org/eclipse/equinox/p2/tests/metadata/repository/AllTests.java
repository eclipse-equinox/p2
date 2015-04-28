/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rapicorp, Inc - addition implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import junit.framework.*;

/**
 * Performs all automated metadata repository tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(BatchExecuteMetadataRepositoryTest.class);
		suite.addTestSuite(CompositeMetadataRepositoryTest.class);
		suite.addTestSuite(JarURLMetadataRepositoryTest.class);
		suite.addTestSuite(LocalMetadataRepositoryTest.class);
		suite.addTestSuite(SPIMetadataRepositoryTest.class);
		suite.addTestSuite(StandaloneSerializationTest.class);
		suite.addTestSuite(MetadataRepositoryManagerTest.class);
		suite.addTestSuite(NoFailOver.class);
		suite.addTestSuite(SiteIndexFileTest.class);
		suite.addTestSuite(XZedRepositoryTest.class);
		//		suite.addTestSuite(ResumeDownloadTest.class);
		// DISABLING until we get a test build
		//		AllServerTests.addToSuite(suite);
		return suite;
	}

}