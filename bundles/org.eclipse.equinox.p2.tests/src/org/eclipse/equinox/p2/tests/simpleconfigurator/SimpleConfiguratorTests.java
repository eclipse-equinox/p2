/*******************************************************************************
 * Copyright (c) 2008, 2026 Red Hat, Inc. and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ SimpleConfiguratorTest.class, SimpleConfiguratorTestExtended.class,
		SimpleConfiguratorTestExtendedConfigured.class, SimpleConfiguratorUtilsTest.class,
		SimpleConfiguratorUtilsExtendedTest.class, SimpleConfiguratorUtilsExtendedConfiguredTest.class,
		BundlesTxtTest.class, BundlesTxtTestExtended.class, BundlesTxtTestExtendedConfigured.class,
		NonExclusiveMode.class, NonExclusiveModeExtended.class, NonExclusiveModeExtendedConfigured.class })
public class SimpleConfiguratorTests {

}
