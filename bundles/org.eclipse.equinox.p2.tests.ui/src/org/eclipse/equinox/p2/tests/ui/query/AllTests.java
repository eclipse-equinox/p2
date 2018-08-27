/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
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
