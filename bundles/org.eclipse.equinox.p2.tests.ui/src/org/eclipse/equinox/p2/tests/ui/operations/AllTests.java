/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all UI operation tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ColocatedRepositoryTrackerTest.class, SizingTest.class, InstallOperationTests.class,
		UpdateOperationTests.class, UninstallOperationTests.class })
public class AllTests {
// test suite
}
