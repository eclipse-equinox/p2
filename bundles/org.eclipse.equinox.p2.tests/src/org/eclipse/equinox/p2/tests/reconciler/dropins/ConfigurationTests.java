/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import java.net.MalformedURLException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.update.*;

/*
 * Tests related to the platform configuration before and after reconciliation.
 * 
 * Tests to add and regression tests to add:
 * - ensure there is a platform:base: entry
 * - 222505 - IUs in the dropins only rely on each other and not on things already in the install
 * - ...
 */
public class ConfigurationTests extends AbstractReconcilerTest {

	/*
	 * Constructor for the class.
	 */
	public ConfigurationTests(String name) {
		super(name);
	}

	/*
	 * The list of tests for this class. Order is important since some of them rely
	 * on the state from the previous test run.
	 */
	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.addTest(new ConfigurationTests("testDiscoverOne"));
		suite.addTest(new ConfigurationTests("test_247095"));
		return suite;
	}

	public void testDiscoverOne() {
		// copy feature and bundle to dropins and reconcile
		File file = getTestData("2.0", "testData/reconciler/features/myFeature_1.0.0");
		add("2.2", "dropins/features", file);
		file = getTestData("2.3", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("2.4", "dropins/plugins", file);
		assertDoesNotExistInBundlesInfo("2.5", "myBundle");
		reconcile("2.6");

		// make sure the feature is listed in a site in the configuration
		Configuration config = getConfiguration();
		assertFeatureExists("3.0", config, "myFeature", "1.0.0");
	}

	/*
	 * We have a user-include site which lists some plug-ins and has a feature as
	 * a sub-element of the site. When the feature and its plug-ins are removed
	 * from the site we need to ensure the plug-ins are removed from the install.
	 */
	public void test_247095() {
		Configuration configuration = getConfiguration();
		File temp = getTempFolder();
		toRemove.add(temp);
		String siteLocation = null;
		try {
			siteLocation = new File(temp, "eclipse").toURL().toExternalForm();
		} catch (MalformedURLException e) {
			fail("0.9", e);
		}

		// copy the data to the temp folder
		File source = getTestData("1.0", "testData/reconciler/247095");
		copy("1.1", source, temp);

		/* this is the entry to add to the site.xml file
			<site enabled="true" policy="USER-INCLUDE" updateable="false"
					url="file:C:/share/1/" list="plugins/hello_1.0.0.jar" >
				<feature id="hello_feature" version="1.0.0" />
			</site>
		 */
		Site site = createSite("USER-INCLUDE", true, false, siteLocation, new String[] {"plugins/bbb_1.0.0.jar,plugins/ccc_1.0.0.jar"});
		Feature feature = createFeature(site, "bbb.feature", "1.0.0", "features/bbb.feature_1.0.0/");
		site.addFeature(feature);
		configuration.add(site);
		save("5.0", configuration);
		reconcile("6.0");
		assertExistsInBundlesInfo("7.0", "bbb", "1.0.0");
		assertExistsInBundlesInfo("7.1", "ccc", "1.0.0");
		configuration = getConfiguration();
		assertFeatureExists("7.2", configuration, "bbb.feature", "1.0.0");

		// remove the feature and its bundle from the platform.xml but leave the second bundle
		configuration = getConfiguration();
		assertTrue("9.0", removeSite(configuration, siteLocation));
		site = createSite("USER-INCLUDE", true, false, siteLocation, new String[] {"plugins/ccc_1.0.0.jar"});
		configuration.add(site);
		save("9.1", configuration);
		reconcile("10.0");
		assertDoesNotExistInBundlesInfo("10.1", "bbb", "1.0.0");
		assertExistsInBundlesInfo("10.2", "ccc", "1.0.0");

		// cleanup
		configuration = getConfiguration();
		removeSite(configuration, siteLocation);
		save("99.2", configuration);
	}
}
