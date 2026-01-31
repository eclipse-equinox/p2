/*******************************************************************************
 * Copyright (c) 2007, 2022 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Performs all automated director tests.
 */
@Suite
@SelectClasses({ ZipVerifierProcessorTest.class, ChecksumVerifierTest.class,
		ChecksumUtilitiesTest.class, PGPSignatureVerifierTest.class, ProduceChecksumTest.class,
		ChecksumPriorityTest.class })
public class AllTests {
// test suite
}
