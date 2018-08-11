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
