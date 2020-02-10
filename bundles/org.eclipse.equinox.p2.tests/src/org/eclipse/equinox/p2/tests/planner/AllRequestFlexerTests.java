/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		TestRequestFlexerIUProperties.class, TestRequestFlexerOneInstalledOneBeingInstalled.class,
		TestRequestFlexerOneInstalledReplacingIt.class, TestRequestFlexerOneInstalledTwoBeingInstalled.class,
		TestRequestFlexerProduct.class, TestRequestFlexerProduct2.class, TestRequestFlexerProductWithLegacyMarkup.class,
		TestRequestFlexerProductWithMixedMarkup.class, TestRequestFlexerRequestWithOptionalInstall.class,
		TestRequestFlexerRequestWithRemoval.class, TestRequestFlexerSharedInstall.class
})
public class AllRequestFlexerTests {
// test suite
}
