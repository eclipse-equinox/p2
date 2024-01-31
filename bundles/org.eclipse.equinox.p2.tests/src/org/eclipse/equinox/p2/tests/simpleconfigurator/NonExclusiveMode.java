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
 *     Red Hat, Inc. - tests
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.File;
import org.eclipse.equinox.p2.tests.TestData;
import org.osgi.framework.BundleContext;

public class NonExclusiveMode extends AbstractSimpleConfiguratorTest {
	private File[] jars = null;
	private File bundleInfo = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		jars = getBundleJars(TestData.getFile("simpleConfiguratorTest/bundlesTxt", ""));
		bundleInfo = createBundlesTxt(jars);
	}

	public void testBundlesTxt() throws Exception {

		File otherBundle = getTestData("myBundle", "testData/simpleConfiguratorTest/myBundle_1.0.0.jar");

		BundleContext equinoxContext = startFramework(bundleInfo, new File[] {otherBundle});

		assertJarsInstalled(jars, equinoxContext.getBundles());
		assertJarsInstalled(new File[] {otherBundle}, equinoxContext.getBundles());
		assertEquals(4, equinoxContext.getBundles().length);
	}

}
