/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.ui;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This is the master test suite for all automated provisioning UI tests. It
 * runs every test that is suitable for running in an automated fashion as part
 * of a build.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ org.eclipse.equinox.p2.tests.ui.operations.AllTests.class,
		org.eclipse.equinox.p2.tests.ui.query.AllTests.class, org.eclipse.equinox.p2.tests.ui.actions.AllTests.class,
		org.eclipse.equinox.p2.tests.ui.dialogs.AllTests.class, org.eclipse.equinox.p2.tests.ui.misc.AllTests.class,
		org.eclipse.equinox.p2.tests.ui.repohandling.AllTests.class,
		org.eclipse.equinox.p2.tests.importexport.AllTests.class })
public class AutomatedTests {
	// test suite
}
