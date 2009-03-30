/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
	private static String BUNDLE_JAR_DIRECTORY = "simpleConfiguratorTest/bundlesTxt";
	private File[] jars = null;
	private File bundleInfo = null;

	protected void setUp() throws Exception {
		super.setUp();
		jars = getBundleJars(TestData.getFile(BUNDLE_JAR_DIRECTORY, ""));
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
