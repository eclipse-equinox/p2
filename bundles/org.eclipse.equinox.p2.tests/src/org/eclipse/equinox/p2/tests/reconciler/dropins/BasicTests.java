/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

//- add new
//- remove only
//- add higher
//- remove higher
//- add lower (no change)
//- remove lower (no change)
//
//singleton behaviour vs non-singleton behaviour
//
// platform specific fragments
//
//dropins/*/eclipse/[features|plugins]/*
//dropins/*/[features|plugins]/*
//dropins/[features|plugins]/*
//dropins/Foo   (Foo is a feature or bundle, in folder or jar shape)
//dropins/some.link
//
//handle both dropins and plugins directory
public class BasicTests extends AbstractReconcilerTest {

	/*
	 * Constructor for the class.
	 */
	public BasicTests(String name) {
		super(name);
	}

	/*
	 * The list of tests for this class. Order is important since some of them rely
	 * on the state from the previous test run.
	 */
	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(BasicTests.class.getName());
		suite.addTest(new BasicTests("testOneSessionInstallRemovalOfDependentFeatures"));
		suite.addTest(new BasicTests("testNonSingleton"));
		suite.addTest(new BasicTests("testSingleton"));
		suite.addTest(new BasicTests("testDirectoryBasedPlugin"));
		suite.addTest(new BasicTests("testSimpleRepoWithSiteXMLPlaceHolder"));
		suite.addTest(new BasicTests("testSimpleRepo"));

		//suite.addTest(new BasicTests("test_251167"));
		suite.addTest(new BasicTests("test_p2Repo"));
		suite.addTest(new BasicTests("testDisabledBundleInLink"));
		suite.addTest(new BasicTests("testMove1"));
		suite.addTest(new BasicTests("testMove2"));

		return suite;
	}

	/*
	 * Basic add and remove tests for directory-based bundles.
	 */
	public void testDirectoryBasedPlugin() {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("1.0", "directoryBased");
		File dir = getTestData("1.1", "testData/reconciler/plugins/directoryBased_1.0.0");
		add("1.2", "dropins", dir);
		reconcile("1.3");
		assertExistsInBundlesInfo("1.4", "directoryBased", "1.0.0");

		remove("2.0", "dropins", "directoryBased_1.0.0");
		reconcile("2.1");
		assertDoesNotExistInBundlesInfo("2.2", "directoryBased");
	}

	/*
	 * Basic bundle
	 * - add to dropins
	 * - reconcile
	 * - installed OK
	 * - move from dropins to plugins
	 * - reconcile
	 * - check to see if bundles.info was updated with the new location
	 */
	public void testMove1() {
		// assert initial state
		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.2", "a");
		assertDoesNotExistInBundlesInfo("0.1", "b");

		// add bundle to dropins
		File jar = getTestData("2.0", "testData/reconciler/move/b_1.0.0.jar");
		add("2.1", "dropins", jar);

		// reconcile
		reconcile("3.0");

		// assert bundle installed
		assertExistsInBundlesInfo("4.0", "b", "1.0.0", "dropins");

		// move bundle to plugins
		remove("5.0", "dropins", "b_1.0.0.jar");
		add("5.1", "plugins", jar);

		// reconcile
		reconcile("6.0");

		// assert bundle still installed
		assertExistsInBundlesInfo("7.0", "b", "1.0.0", "plugins");

		// cleanup
		remove("99.0", "plugins", "b_1.0.0.jar");
		reconcile("99.1");
		assertDoesNotExistInBundlesInfo("99.2", "b");
	}

	public void testOneSessionInstallRemovalOfDependentFeatures() {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.1", "c");

		// add bundle to dropins
		File jar = getTestData("2.0", "testData/reconciler/installuninstall/c_1.0.0.jar");
		add("2.1", "dropins", jar);

		// reconcile
		reconcile("3.0");
		assertExistsInBundlesInfo("4.0", "c", "1.0.0", "c_1.0.0.jar");

		//remove b, do not reconcile yet
		remove("4.1", "dropins", jar.getName());

		File jar2 = getTestData("4.2", "testData/reconciler/installuninstall/d_1.0.0.jar");
		add("4.3", "dropins", jar2);

		reconcile("5.0");
		//b was removed. It should be uninstalled, too.
		assertDoesNotExistInBundlesInfo("5.0", "c");
	}

	/*
	 * A depends on B
	 * - add B to dropins
	 * - reconcile
	 * - add A to dropins
	 * - reconcile
	 * - move B from dropins to plugins
	 * - reconcile
	 * - ensure A is still ok
	 * - ensure location of B has been updated in bundles.info
	 */
	public void testMove2() {
		// assert initial state
		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.1", "a");
		assertDoesNotExistInBundlesInfo("0.2", "b");

		// add first bundle to dropins
		File jar = getTestData("2.0", "testData/reconciler/move/b_1.0.0.jar");
		add("2.1", "dropins", jar);
		reconcile("2.2");
		assertExistsInBundlesInfo("2.3", "b", "1.0.0", "dropins");

		// add second bundle to dropins
		jar = getTestData("3.0", "testData/reconciler/move/a_1.0.0.jar");
		add("3.1", "dropins", jar);
		reconcile("3.2");
		assertExistsInBundlesInfo("3.3", "a", "1.0.0", "dropins");

		// move bundle to plugins
		remove("5.0", "dropins", "b_1.0.0.jar");
		jar = getTestData("5.1", "testData/reconciler/move/b_1.0.0.jar");
		add("5.2", "plugins", jar);
		reconcile("5.3");

		// assert bundle still installed
		assertExistsInBundlesInfo("7.0", "b", "1.0.0", "plugins");
		assertExistsInBundlesInfo("7.1", "a", "1.0.0", "dropins");

		// cleanup
		remove("99.0", "dropins", "a_1.0.0.jar");
		remove("99.1", "plugins", "b_1.0.0.jar");
		reconcile("99.2");
		assertDoesNotExistInBundlesInfo("99.3", "a");
		assertDoesNotExistInBundlesInfo("99.4", "b");
	}

	/*
	 * Basic add and remove operations for non-singleton bundles.
	 */
	public void testNonSingleton() {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.1", "myBundle");
		// copy bundle to dropins and reconcile
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("0.2", "dropins", jar);
		reconcile("0.3");
		// bundle should exist
		assertExistsInBundlesInfo("0.4", "myBundle");

		// remove the bundle from the dropins and reconcile
		remove("1.0", "dropins", "myBundle_1.0.0.jar");
		reconcile("1.1");
		// bundle should not exist anymore
		assertDoesNotExistInBundlesInfo("1.2", "myBundle");

		// Add 2 versions of the same non-singleton bundle to the dropins folder and
		// ensure that both of them exist after reconciliation. 
		jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("2.1", "dropins", jar);
		jar = getTestData("2.2", "testData/reconciler/plugins/myBundle_2.0.0.jar");
		add("2.3", "dropins", jar);
		reconcile("2.4");
		// bundle should exist - both versions since we have non-singleton bundles
		assertExistsInBundlesInfo("2.5", "myBundle", "1.0.0");
		assertExistsInBundlesInfo("2.6", "myBundle", "2.0.0");

		// start with 2 non-singleton versions of the same bundle and remove the lower
		// version and reconcile. should have just the higher version left.
		remove("3.0", "dropins", "myBundle_1.0.0.jar");
		reconcile("3.1");
		// only the higher version should exist
		assertDoesNotExistInBundlesInfo("3.2", "myBundle", "1.0.0");
		assertExistsInBundlesInfo("3.3", "myBundle", "2.0.0");

		// cleanup
		remove("99.0", "dropins", "myBundle_2.0.0.jar");
		reconcile("99.1");
		assertDoesNotExistInBundlesInfo("99.2", "myBundle", "2.0.0");
	}

	/*
	 * Perform some add and remove operations with two different versions
	 * of a singleton bundle.
	 */
	public void testSingleton() {
		assertInitialized();
		// empty state
		assertDoesNotExistInBundlesInfo("1.0", "mySingletonBundle");

		// add first version
		File jar = getTestData("2.0", "testData/reconciler/plugins/mySingletonBundle_1.0.0.jar");
		add("2.1", "dropins", jar);
		reconcile("2.3");

		// only lowest version of the bundle exists
		assertExistsInBundlesInfo("3.0", "mySingletonBundle", "1.0.0");
		assertDoesNotExistInBundlesInfo("3.1", "mySingletonBundle", "2.0.0");

		// add higher version
		jar = getTestData("4.0", "testData/reconciler/plugins/mySingletonBundle_2.0.0.jar");
		add("4.1", "dropins", jar);
		reconcile("4.3");

		// highest version of the bundle has replaced the lower one
		assertDoesNotExistInBundlesInfo("5.1", "mySingletonBundle", "1.0.0");
		assertExistsInBundlesInfo("5.2", "mySingletonBundle", "2.0.0");

		// re-add the lower version
		jar = getTestData("6.0", "testData/reconciler/plugins/mySingletonBundle_1.0.0.jar");
		add("6.1", "dropins", jar);
		reconcile("6.3");

		// nothing changes
		assertDoesNotExistInBundlesInfo("7.1", "mySingletonBundle", "1.0.0");
		assertExistsInBundlesInfo("7.2", "mySingletonBundle", "2.0.0");

		// add back lower version
		jar = getTestData("8.0", "testData/reconciler/plugins/mySingletonBundle_1.0.0.jar");
		add("8.1", "dropins", jar);
		reconcile("8.3");

		// no change
		assertDoesNotExistInBundlesInfo("9.1", "mySingletonBundle", "1.0.0");
		assertExistsInBundlesInfo("9.2", "mySingletonBundle", "2.0.0");

		// remove higher version
		remove("10.1", "dropins", "mySingletonBundle_2.0.0.jar");
		reconcile("10.2");

		// lower should be there
		assertExistsInBundlesInfo("11.0", "mySingletonBundle", "1.0.0");
		assertDoesNotExistInBundlesInfo("11.1", "mySingletonBundle", "2.0.0");

		// cleanup
		remove("99.0", "dropins", "mySingletonBundle_1.0.0.jar");
		remove("99.1", "dropins", "mySingletonBundle_2.0.0.jar");
		reconcile("99.2");
		assertDoesNotExistInBundlesInfo("99.3", "mySingletonBundle", "1.0.0");
		assertDoesNotExistInBundlesInfo("99.4", "mySingletonBundle", "2.0.0");
	}

	/*
	 * Tests adding a simplerepo to the dropins folder. Note we need a dummy site.mxl for now
	 * We likely still need this tests for backwards compatability
	 */
	public void testSimpleRepoWithSiteXMLPlaceHolder() {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.1", "myBundle");

		// copy bundles and repo files to dropins and reconcile
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("1.0", "dropins/simplerepo/plugins", jar);
		jar = getTestData("2.2", "testData/reconciler/plugins/myBundle_2.0.0.jar");
		add("1.1", "dropins/simplerepo/plugins", jar);

		File artifactRepo = getTestData("2.0", "testData/reconciler/simplerepo/artifacts.xml");
		add("1.1", "dropins/simplerepo", artifactRepo);
		File metadataRepo = getTestData("2.0", "testData/reconciler/simplerepo/content.xml");
		add("1.1", "dropins/simplerepo", metadataRepo);
		File dummySiteXML = getTestData("2.0", "testData/reconciler/simplerepo/site.xml");
		add("1.1", "dropins/simplerepo", dummySiteXML);

		reconcile("2.0");

		// bundle should exist - both versions since we have non-singleton bundles
		assertDoesNotExistInBundlesInfo("2.1", "myBundle", "1.0.0");
		assertExistsInBundlesInfo("2.2", "myBundle", "2.0.0");

		// cleanup
		remove("99.0", "dropins", "simplerepo");
		reconcile("99.1");
		assertDoesNotExistInBundlesInfo("99.2", "myBundle");
	}

	/*
	 * Tests adding a simplerepo to the dropins folder. Note we need a dummy site.mxl for now
	 * We likely still need this tests for backwards compatability
	 */
	public void testSimpleRepo() {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.1", "myBundle");

		// copy bundles and repo files to dropins and reconcile
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("1.0", "dropins/simplerepo/plugins", jar);
		jar = getTestData("2.2", "testData/reconciler/plugins/myBundle_2.0.0.jar");
		add("1.1", "dropins/simplerepo/plugins", jar);

		File artifactRepo = getTestData("2.0", "testData/reconciler/simplerepo/artifacts.xml");
		add("1.1", "dropins/simplerepo", artifactRepo);
		File metadataRepo = getTestData("2.0", "testData/reconciler/simplerepo/content.xml");
		add("1.1", "dropins/simplerepo", metadataRepo);

		reconcile("2.0");

		// bundle should exist - both versions since we have non-singleton bundles
		assertDoesNotExistInBundlesInfo("2.1", "myBundle", "1.0.0");
		assertExistsInBundlesInfo("2.2", "myBundle", "2.0.0");

		// cleanup
		remove("99.0", "dropins", "simplerepo");
		reconcile("99.1");
		assertDoesNotExistInBundlesInfo("99.2", "myBundle");
	}

	/*
	 * Add 2 bundles to the dropins and reconcile. A has an optional dependency on B.
	 * Remove B and re-reconcile. The problem was that B was no longer installed or in
	 *  the bundles.info but it was still in the profile.
	 */
	public void test_251167() {
		assertInitialized();

		// empty state
		assertDoesNotExistInBundlesInfo("1.0", "A");
		assertDoesNotExistInBundlesInfo("1.1", "B");

		// setup the test data
		File jar = getTestData("2.0", "testData/reconciler/251167/A_1.0.0.jar");
		add("2.1", "dropins", jar);
		jar = getTestData("2.2", "testData/reconciler/251167/B_1.0.0.jar");
		add("2.3", "dropins", jar);
		reconcile("2.4");

		assertExistsInBundlesInfo("3.0", "A");
		assertExistsInBundlesInfo("3.1", "B");
		assertTrue("3.2", isInstalled("A", "1.0.0"));
		assertTrue("3.3", isInstalled("B", "1.0.0"));

		// remove B
		remove("4.0", "dropins", "B_1.0.0.jar");
		reconcile("4.1");

		assertExistsInBundlesInfo("5.0", "A");
		assertDoesNotExistInBundlesInfo("5.1", "B");
		assertTrue("5.2", isInstalled("A", "1.0.0"));
		assertFalse("5.3", isInstalled("B", "1.0.0"));

		// cleanup
		remove("6.0", "dropins", "A_1.0.0.jar");
		reconcile("6.1");
		assertFalse("6.2", isInstalled("A", "1.0.0"));
	}

	/*
	 * - add content.jar
	 * - add artifacts.jar
	 * - add indexs + plugins/features/binary dir (bug 252752)
	 */
	public void test_p2Repo() {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.1", "zzz");
		assertFalse("0.2", isInstalled("zFeature", "1.0.0"));

		File source = getTestData("1.0", "testData/reconciler/basicRepo.jar");
		add("1.1", "dropins", source);

		reconcile("2.0");

		assertExistsInBundlesInfo("3.0", "zzz");
		assertTrue("3.1", isInstalled("zzz", "1.0.0"));
		assertTrue("3.2", isInstalled("zFeature.feature.group", "1.0.0"));
		IInstallableUnit unit = getRemoteIU("zzz", "1.0.0");
		assertEquals("3.3", "foo", unit.getProperty("test"));

		// cleanup
		remove("4.0", "dropins", "basicRepo.jar");
		reconcile("4.1");
		assertDoesNotExistInBundlesInfo("4.2", "zzz");
		assertFalse("4.3", isInstalled("zzz", "1.0.0"));
		assertFalse("4.4", isInstalled("zFeature.feature.group", "1.0.0"));
	}

	/*
	 * See bug 265121.
	 */
	public void testDisabledBundleInLink() {
		assertInitialized();
		File link = getTestData("1.0", "testData/reconciler/link");
		File temp = getTempFolder();
		// add this to the "toRemove" set in case we fail, we still want it to be removed by the cleanup
		toRemove.add(temp);
		copy("1.1", link, temp);
		String linkFilename = getUniqueString();
		createLinkFile("1.2", linkFilename, temp.getAbsolutePath());

		reconcile("2.0");

		assertDoesNotExistInBundlesInfo("3.0", "bbb");
		assertFalse("3.1", isInstalled("bbb", "1.0.0"));
		assertExistsInBundlesInfo("3.3", "ccc");
		assertTrue("3.4", isInstalled("ccc", "1.0.0"));

		// cleanup
		removeLinkFile("4.0", linkFilename);
		reconcile("4.1");
		assertDoesNotExistInBundlesInfo("5.0", "bbb");
		assertFalse("5.1", isInstalled("bbb", "1.0.0"));
		assertDoesNotExistInBundlesInfo("5.3", "ccc");
		assertFalse("5.4", isInstalled("ccc", "1.0.0"));
	}
}
