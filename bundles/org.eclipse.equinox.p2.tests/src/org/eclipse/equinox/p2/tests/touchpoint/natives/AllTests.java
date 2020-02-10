/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all automated director tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		ChmodActionTest.class, CleanupzipActionTest.class, CollectActionTest.class, LinkActionTest.class,
		MkdirActionTest.class, NativeTouchpointTest.class, RmdirActionTest.class, UnzipActionTest.class,
		CopyActionTest.class, RemoveActionTest.class, BackupStoreTest.class,
		CheckAndPromptNativePackageWindowsRegistryTest.class
})
public class AllTests {
// test suite
}
