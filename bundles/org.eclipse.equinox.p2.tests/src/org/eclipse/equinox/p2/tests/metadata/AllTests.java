/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all metadata tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ArtifactKeyParsingTest.class, FragmentMethodTest.class, FragmentTest.class,
		InstallableUnitTest.class, InstallableUnitPatchTest.class, IUPersistenceTest.class, LatestIUTest.class,
		LicenseTest.class, MultipleIUAndFragmentTest.class, PersistNegation.class, PersistFragment.class,
		ProvidedCapabilityTest.class, RequirementToString.class, RequirementParsingTest.class })
public class AllTests {
//test suite
}