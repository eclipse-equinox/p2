package org.eclipse.equinox.p2.tests.importexport;

import junit.framework.*;

/**
 * Performs all automated importexport tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(ImportExportTests.class);
		//		suite.addTestSuite(ImportExportRemoteTests.class);
		return suite;
	}

}
