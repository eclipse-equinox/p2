/*******************************************************************************
 * Copyright (c) 2009, 2018 Cloudsmith Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.repository;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Performs all automated repository bundle tests.
 */
@Suite
@SelectClasses({ CacheManagerTest.class, RepositoryHelperTest.class, RepositoryExtensionPointTest.class,
		FileReaderTest2.class, ChecksumHelperTest.class })
public class AllTests {
	// test suite
}
