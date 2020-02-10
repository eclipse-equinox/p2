/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.ant;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all automated artifact repository tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CompositeRepositoryTaskTest.class, MirrorTaskTest.class, Repo2RunnableTaskTests.class })
public class AllTests {
// test suite
}