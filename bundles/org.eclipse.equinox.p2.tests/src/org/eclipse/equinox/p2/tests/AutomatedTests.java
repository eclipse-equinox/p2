/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith - bug fixing and new functionality
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import junit.framework.*;

/**
 * This is the master test suite for all automated provisioning tests. It runs every test
 * that is suitable for running in an automated fashion as part of a build. Some
 * tests may be excluded if they require special setup (such as generating metadata).
 * 
 * PLEASE ADD SUITES IN THE PACKAGE NAME ORDER.
 */
public class AutomatedTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(AutomatedTests.class.getName());
		suite.addTest(org.eclipse.equinox.p2.tests.publisher.AllTests.suite());
		suite.addTest(org.eclipse.equinox.frameworkadmin.tests.AllTests.suite());

		suite.addTest(org.eclipse.equinox.p2.tests.ant.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.artifact.processors.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.artifact.repository.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.artifact.repository.processing.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.core.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.director.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.directorywatcher.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.engine.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.extensionlocation.AllTests.suite());
		//org.eclipse.equinox.p2.tests.full - Off sequence
		suite.addTest(org.eclipse.equinox.p2.tests.gc.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.generator.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.installer.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.jarprocessor.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.metadata.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.metadata.expression.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.metadata.repository.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.mirror.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.omniVersion.AllTests.suite());
		//org.eclipse.equinox.p2.tests.perf -- Executed as part of the 
		suite.addTest(org.eclipse.equinox.p2.tests.planner.AllTests.suite());
		//org.eclipse.equinox.p2.tests.publisher.actions -- The tests found in this package are invoked from the publisher package 
		suite.addTest(org.eclipse.equinox.p2.tests.ql.AllTests.suite());
		//org.eclipse.equinox.p2.tests.reconciler.dropins -- Off sequence
		suite.addTest(org.eclipse.equinox.p2.tests.repository.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.sat4j.smoke.AllTests.suite());
		//org.eclipse.equinox.p2.tests.sharedinstall -- Off sequence
		suite.addTest(org.eclipse.equinox.p2.tests.simpleconfigurator.SimpleConfiguratorTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.simpleconfigurator.manipulator.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.touchpoint.eclipse.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.touchpoint.natives.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.updatechecker.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.updatesite.AllTests.suite());

		//Off sequence
		suite.addTest(org.eclipse.equinox.p2.tests.reconciler.dropins.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.sharedinstall.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.full.AllTests.suite());
		return suite;
	}
}
