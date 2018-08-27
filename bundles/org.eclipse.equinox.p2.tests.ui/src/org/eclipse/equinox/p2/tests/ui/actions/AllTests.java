/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.actions;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all UI action tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ UninstallActionTest.class, UpdateActionTest.class, RemoveColocatedRepositoryActionTest.class,
		ElementUtilsTest.class })
public class AllTests {
//	test suite
}
