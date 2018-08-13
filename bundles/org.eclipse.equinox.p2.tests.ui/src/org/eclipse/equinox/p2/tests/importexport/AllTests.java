package org.eclipse.equinox.p2.tests.importexport;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all automated importexport tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ImportExportTests.class, ImportExportRemoteTests.class })
public class AllTests {
	// test suite
}
