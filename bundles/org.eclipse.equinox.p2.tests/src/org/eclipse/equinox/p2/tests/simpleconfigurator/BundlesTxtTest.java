/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     Red Hat, Inc. - fragment support
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.File;
import org.eclipse.equinox.p2.tests.TestData;
import org.osgi.framework.BundleContext;

public class BundlesTxtTest extends AbstractSimpleConfiguratorTest {

	{
		BUNDLE_JAR_DIRECTORY = "simpleConfiguratorTest/bundlesTxt";
	}

	protected File[] jars = null;
	protected File bundleInfo = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		jars = getBundleJars(TestData.getFile(BUNDLE_JAR_DIRECTORY, ""));
		// Create a bundles.info containing all the jars passed
		bundleInfo = createBundlesTxt(jars);
	}

	public void testBundlesTxt() throws Exception {
		BundleContext equinoxContext = startFramework(bundleInfo, null);
		assertJarsInstalled(jars, equinoxContext.getBundles());
		assertEquals(jars.length + 2, equinoxContext.getBundles().length);
	}
}
