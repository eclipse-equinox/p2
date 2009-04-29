/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import junit.framework.*;

/**
 * Performs all automated metadata repository tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(JarURLMetadataRepositoryTest.class);
		suite.addTestSuite(LocalMetadataRepositoryTest.class);
		suite.addTestSuite(SPIMetadataRepositoryTest.class);
		suite.addTestSuite(MetadataRepositoryManagerTest.class);
		suite.addTestSuite(CompositeMetadataRepositoryTest.class);
		suite.addTestSuite(ResumeDownloadTest.class);
		AllServerTests.addToSuite(suite);
		return suite;
	}

}