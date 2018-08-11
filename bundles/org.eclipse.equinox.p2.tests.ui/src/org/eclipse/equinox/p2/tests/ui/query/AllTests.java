/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all UI query tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ AnyRequiredCapabilityTest.class, AvailableIUWrapperTest.class, CategoryElementWrapperTest.class,
		TranslationSupportTests.class, LatestIUVersionElementWrapperTest.class, QueryDescriptorTest.class,
		QueryProviderTests.class, QueryableMetadataRepositoryManagerTest.class,
		// This must come after QueryableMetadataRepositoryManager or it causes
		// side-effects in those tests.
		QueryableArtifactRepositoryManagerTest.class })
public class AllTests {
//test suite
}
