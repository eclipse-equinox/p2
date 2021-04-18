/*******************************************************************************
 * Copyright (c) 2007, 2018 compeople AG and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all automated director tests.
 */
@SuppressWarnings("removal")
@RunWith(Suite.class)
@Suite.SuiteClasses({ Pack200ProcessorTest.class, ZipVerifierProcessorTest.class, ChecksumVerifierTest.class,
		ChecksumUtilitiesTest.class, PGPSignatureVerifierTest.class })
public class AllTests {
// test suite
}
