/*******************************************************************************
 *  Copyright (c) 2008, 2026 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import java.io.IOException;
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
	public void testDirectoryBasedPlugin() throws IOException {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("directoryBased");
		File dir = getTestData("1.1", "testData/reconciler/plugins/directoryBased_1.0.0");
		add("dropins", dir);
		reconcile();
		assertExistsInBundlesInfo("directoryBased", "1.0.0");

		remove("dropins", "directoryBased_1.0.0");
		reconcile();
		assertDoesNotExistInBundlesInfo("directoryBased");
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
	public void testMove1() throws IOException {
		// assert initial state
		assertInitialized();
		assertDoesNotExistInBundlesInfo("a");
		assertDoesNotExistInBundlesInfo("b");

		// add bundle to dropins
		File jar = getTestData("2.0", "testData/reconciler/move/b_1.0.0.jar");
		add("dropins", jar);

		// reconcile
		reconcile();

		// assert bundle installed
		assertExistsInBundlesInfo("b", "1.0.0", "dropins");

		// move bundle to plugins
		remove("dropins", "b_1.0.0.jar");
		add("plugins", jar);

		// reconcile
		reconcile();

		// assert bundle still installed
		assertExistsInBundlesInfo("b", "1.0.0", "plugins");

		// cleanup
		remove("plugins", "b_1.0.0.jar");
		reconcile();
		assertDoesNotExistInBundlesInfo("b");
	}

	public void testOneSessionInstallRemovalOfDependentFeatures() throws IOException {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("c");

		// add bundle to dropins
		File jar = getTestData("2.0", "testData/reconciler/installuninstall/c_1.0.0.jar");
		add("dropins", jar);

		// reconcile
		reconcile();
		assertExistsInBundlesInfo("c", "1.0.0", "c_1.0.0.jar");

		//remove b, do not reconcile yet
		remove("dropins", jar.getName());

		File jar2 = getTestData("4.2", "testData/reconciler/installuninstall/d_1.0.0.jar");
		add("dropins", jar2);

		reconcile();
		//b was removed. It should be uninstalled, too.
		assertDoesNotExistInBundlesInfo("c");
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
	public void testMove2() throws IOException {
		// assert initial state
		assertInitialized();
		assertDoesNotExistInBundlesInfo("a");
		assertDoesNotExistInBundlesInfo("b");

		// add first bundle to dropins
		File jar = getTestData("2.0", "testData/reconciler/move/b_1.0.0.jar");
		add("dropins", jar);
		reconcile();
		assertExistsInBundlesInfo("b", "1.0.0", "dropins");

		// add second bundle to dropins
		jar = getTestData("3.0", "testData/reconciler/move/a_1.0.0.jar");
		add("dropins", jar);
		reconcile();
		assertExistsInBundlesInfo("a", "1.0.0", "dropins");

		// move bundle to plugins
		remove("dropins", "b_1.0.0.jar");
		jar = getTestData("5.1", "testData/reconciler/move/b_1.0.0.jar");
		add("plugins", jar);
		reconcile();

		// assert bundle still installed
		assertExistsInBundlesInfo("b", "1.0.0", "plugins");
		assertExistsInBundlesInfo("a", "1.0.0", "dropins");

		// cleanup
		remove("dropins", "a_1.0.0.jar");
		remove("plugins", "b_1.0.0.jar");
		reconcile();
		assertDoesNotExistInBundlesInfo("a");
		assertDoesNotExistInBundlesInfo("b");
	}

	/*
	 * Basic add and remove operations for non-singleton bundles.
	 */
	public void testNonSingleton() throws IOException {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("myBundle");
		// copy bundle to dropins and reconcile
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("dropins", jar);
		reconcile();
		// bundle should exist
		assertExistsInBundlesInfo("myBundle");

		// remove the bundle from the dropins and reconcile
		remove("dropins", "myBundle_1.0.0.jar");
		reconcile();
		// bundle should not exist anymore
		assertDoesNotExistInBundlesInfo("myBundle");

		// Add 2 versions of the same non-singleton bundle to the dropins folder and
		// ensure that both of them exist after reconciliation.
		jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("dropins", jar);
		jar = getTestData("2.2", "testData/reconciler/plugins/myBundle_2.0.0.jar");
		add("dropins", jar);
		reconcile();
		// bundle should exist - both versions since we have non-singleton bundles
		assertExistsInBundlesInfo("myBundle", "1.0.0");
		assertExistsInBundlesInfo("myBundle", "2.0.0");

		// start with 2 non-singleton versions of the same bundle and remove the lower
		// version and reconcile. should have just the higher version left.
		remove("dropins", "myBundle_1.0.0.jar");
		reconcile();
		// only the higher version should exist
		assertDoesNotExistInBundlesInfo("myBundle", "1.0.0");
		assertExistsInBundlesInfo("myBundle", "2.0.0");

		// cleanup
		remove("dropins", "myBundle_2.0.0.jar");
		reconcile();
		assertDoesNotExistInBundlesInfo("myBundle", "2.0.0");
	}

	/*
	 * Perform some add and remove operations with two different versions
	 * of a singleton bundle.
	 */
	public void testSingleton() throws IOException {
		assertInitialized();
		// empty state
		assertDoesNotExistInBundlesInfo("mySingletonBundle");

		// add first version
		File jar = getTestData("2.0", "testData/reconciler/plugins/mySingletonBundle_1.0.0.jar");
		add("dropins", jar);
		reconcile();

		// only lowest version of the bundle exists
		assertExistsInBundlesInfo("mySingletonBundle", "1.0.0");
		assertDoesNotExistInBundlesInfo("mySingletonBundle", "2.0.0");

		// add higher version
		jar = getTestData("4.0", "testData/reconciler/plugins/mySingletonBundle_2.0.0.jar");
		add("dropins", jar);
		reconcile();

		// highest version of the bundle has replaced the lower one
		assertDoesNotExistInBundlesInfo("mySingletonBundle", "1.0.0");
		assertExistsInBundlesInfo("mySingletonBundle", "2.0.0");

		// re-add the lower version
		jar = getTestData("6.0", "testData/reconciler/plugins/mySingletonBundle_1.0.0.jar");
		add("dropins", jar);
		reconcile();

		// nothing changes
		assertDoesNotExistInBundlesInfo("mySingletonBundle", "1.0.0");
		assertExistsInBundlesInfo("mySingletonBundle", "2.0.0");

		// add back lower version
		jar = getTestData("8.0", "testData/reconciler/plugins/mySingletonBundle_1.0.0.jar");
		add("dropins", jar);
		reconcile();

		// no change
		assertDoesNotExistInBundlesInfo("mySingletonBundle", "1.0.0");
		assertExistsInBundlesInfo("mySingletonBundle", "2.0.0");

		// remove higher version
		remove("dropins", "mySingletonBundle_2.0.0.jar");
		reconcile();

		// lower should be there
		assertExistsInBundlesInfo("mySingletonBundle", "1.0.0");
		assertDoesNotExistInBundlesInfo("mySingletonBundle", "2.0.0");

		// cleanup
		remove("dropins", "mySingletonBundle_1.0.0.jar");
		remove("dropins", "mySingletonBundle_2.0.0.jar");
		reconcile();
		assertDoesNotExistInBundlesInfo("mySingletonBundle", "1.0.0");
		assertDoesNotExistInBundlesInfo("mySingletonBundle", "2.0.0");
	}

	/*
	 * Tests adding a simplerepo to the dropins folder. Note we need a dummy site.mxl for now
	 * We likely still need this tests for backwards compatability
	 */
	public void testSimpleRepoWithSiteXMLPlaceHolder() throws IOException {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("myBundle");

		// copy bundles and repo files to dropins and reconcile
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("dropins/simplerepo/plugins", jar);
		jar = getTestData("2.2", "testData/reconciler/plugins/myBundle_2.0.0.jar");
		add("dropins/simplerepo/plugins", jar);

		File artifactRepo = getTestData("2.0", "testData/reconciler/simplerepo/artifacts.xml");
		add("dropins/simplerepo", artifactRepo);
		File metadataRepo = getTestData("2.0", "testData/reconciler/simplerepo/content.xml");
		add("dropins/simplerepo", metadataRepo);
		File dummySiteXML = getTestData("2.0", "testData/reconciler/simplerepo/site.xml");
		add("dropins/simplerepo", dummySiteXML);

		reconcile();

		// bundle should exist - both versions since we have non-singleton bundles
		assertDoesNotExistInBundlesInfo("myBundle", "1.0.0");
		assertExistsInBundlesInfo("myBundle", "2.0.0");

		// cleanup
		remove("dropins", "simplerepo");
		reconcile();
		assertDoesNotExistInBundlesInfo("myBundle");
	}

	/*
	 * Tests adding a simplerepo to the dropins folder. Note we need a dummy site.mxl for now
	 * We likely still need this tests for backwards compatability
	 */
	public void testSimpleRepo() throws IOException {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("myBundle");

		// copy bundles and repo files to dropins and reconcile
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("dropins/simplerepo/plugins", jar);
		jar = getTestData("2.2", "testData/reconciler/plugins/myBundle_2.0.0.jar");
		add("dropins/simplerepo/plugins", jar);

		File artifactRepo = getTestData("2.0", "testData/reconciler/simplerepo/artifacts.xml");
		add("dropins/simplerepo", artifactRepo);
		File metadataRepo = getTestData("2.0", "testData/reconciler/simplerepo/content.xml");
		add("dropins/simplerepo", metadataRepo);

		reconcile();

		// bundle should exist - both versions since we have non-singleton bundles
		assertDoesNotExistInBundlesInfo("myBundle", "1.0.0");
		assertExistsInBundlesInfo("myBundle", "2.0.0");

		// cleanup
		remove("dropins", "simplerepo");
		reconcile();
		assertDoesNotExistInBundlesInfo("myBundle");
	}

	/*
	 * Add 2 bundles to the dropins and reconcile. A has an optional dependency on B.
	 * Remove B and re-reconcile. The problem was that B was no longer installed or in
	 *  the bundles.info but it was still in the profile.
	 */
	public void test_251167() throws IOException {
		assertInitialized();

		// empty state
		assertDoesNotExistInBundlesInfo("A");
		assertDoesNotExistInBundlesInfo("B");

		// setup the test data
		File jar = getTestData("2.0", "testData/reconciler/251167/A_1.0.0.jar");
		add("dropins", jar);
		jar = getTestData("2.2", "testData/reconciler/251167/B_1.0.0.jar");
		add("dropins", jar);
		reconcile();

		assertExistsInBundlesInfo("A");
		assertExistsInBundlesInfo("B");
		assertTrue(isInstalled("A", "1.0.0"));
		assertTrue(isInstalled("B", "1.0.0"));

		// remove B
		remove("dropins", "B_1.0.0.jar");
		reconcile();

		assertExistsInBundlesInfo("A");
		assertDoesNotExistInBundlesInfo("B");
		assertTrue(isInstalled("A", "1.0.0"));
		assertFalse(isInstalled("B", "1.0.0"));

		// cleanup
		remove("dropins", "A_1.0.0.jar");
		reconcile();
		assertFalse(isInstalled("A", "1.0.0"));
	}

	/*
	 * - add content.jar
	 * - add artifacts.jar
	 * - add indexs + plugins/features/binary dir (bug 252752)
	 */
	public void test_p2Repo() throws IOException {
		assertInitialized();
		assertDoesNotExistInBundlesInfo("zzz");
		assertFalse(isInstalled("zFeature", "1.0.0"));

		File source = getTestData("1.0", "testData/reconciler/basicRepo.jar");
		add("dropins", source);

		reconcile();

		assertExistsInBundlesInfo("zzz");
		assertTrue(isInstalled("zzz", "1.0.0"));
		assertTrue(isInstalled("zFeature.feature.group", "1.0.0"));
		IInstallableUnit unit = getRemoteIU("zzz", "1.0.0");
		assertEquals("foo", unit.getProperty("test"));

		// cleanup
		remove("dropins", "basicRepo.jar");
		reconcile();
		assertDoesNotExistInBundlesInfo("zzz");
		assertFalse(isInstalled("zzz", "1.0.0"));
		assertFalse(isInstalled("zFeature.feature.group", "1.0.0"));
	}

	/*
	 * See bug 265121.
	 */
	public void testDisabledBundleInLink() throws IOException {
		assertInitialized();
		File link = getTestData("1.0", "testData/reconciler/link");
		File temp = getTempFolder();
		// add this to the "toRemove" set in case we fail, we still want it to be removed by the cleanup
		toRemove.add(temp);
		copy(link, temp);
		String linkFilename = getUniqueString();
		createLinkFile("1.2", linkFilename, temp.getAbsolutePath());

		reconcile();

		assertDoesNotExistInBundlesInfo("bbb");
		assertFalse(isInstalled("bbb", "1.0.0"));
		assertExistsInBundlesInfo("ccc");
		assertTrue(isInstalled("ccc", "1.0.0"));

		// cleanup
		removeLinkFile("4.0", linkFilename);
		reconcile();
		assertDoesNotExistInBundlesInfo("bbb");
		assertFalse(isInstalled("bbb", "1.0.0"));
		assertDoesNotExistInBundlesInfo("ccc");
		assertFalse(isInstalled("ccc", "1.0.0"));
	}
}
