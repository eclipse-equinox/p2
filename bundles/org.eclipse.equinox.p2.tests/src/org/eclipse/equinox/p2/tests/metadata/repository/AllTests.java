/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *     Rapicorp, Inc - addition implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Performs all automated metadata repository tests.
 */
@Suite
@SelectClasses({
		BatchExecuteMetadataRepositoryTest.class, CompositeMetadataRepositoryTest.class,
		JarURLMetadataRepositoryTest.class, LocalMetadataRepositoryTest.class, SPIMetadataRepositoryTest.class,
		StandaloneSerializationTest.class, MetadataRepositoryManagerTest.class, NoFailOver.class,
		SiteIndexFileTest.class, XZedRepositoryTest.class
})
public class AllTests {
	// ResumeDownloadTest.class,
		// DISABLING until we get a test build
	// AllServerTests.addToSuite(suite,

}