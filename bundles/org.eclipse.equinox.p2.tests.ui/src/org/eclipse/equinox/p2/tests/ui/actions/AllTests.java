/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
