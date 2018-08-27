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
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all UI wizard and dialog tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ InstallWizardTest.class, InstalledSoftwarePageTest.class, InstallWithRemediationTest.class,
		InstallationHistoryPageTest.class, UpdateWizardTest.class, UninstallWizardTest.class,
		RepositoryManipulationPageTest.class, IUPropertyPagesTest.class, PreferencePagesTest.class })
public class AllTests {
	// test suite
}
