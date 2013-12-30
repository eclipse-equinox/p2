/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - fragment support
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.File;
import org.eclipse.equinox.internal.simpleconfigurator.Activator;
import org.eclipse.equinox.p2.tests.sharedinstall.AbstractSharedInstallTest;
import org.osgi.framework.BundleContext;

public class BundlesTxtTestExtendedConfigured extends BundlesTxtTestExtended {

	private File testData;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		//subdir extension will be loaded
		testData = getTempFolder();
		copy("preparing testData", getTestData("simpleconfigurator extensions", "testData/simpleConfiguratorTest"), testData);
		Activator.EXTENSIONS = testData.toString();
		System.setProperty("p2.fragments", Activator.EXTENSIONS);
		AbstractSharedInstallTest.setReadOnly(testData, true);
		AbstractSharedInstallTest.reallyReadOnly(testData);
	}

	@Override
	public void testBundlesTxt() throws Exception {
		BundleContext equinoxContext = startFramework(bundleInfo, null);
		assertJarsInstalled(jars, equinoxContext.getBundles());
		/**
		 * 3 = one extension + osgi + simpleconfigurator
		 */
		assertEquals(jars.length + 3, equinoxContext.getBundles().length);
	}

	@Override
	protected void tearDown() throws Exception {
		Activator.EXTENSIONS = null;
		AbstractSharedInstallTest.removeReallyReadOnly(testData);
		AbstractSharedInstallTest.setReadOnly(testData, false);
		testData.delete();
		super.tearDown();
		System.setProperty("p2.fragments", "");
	}
}
