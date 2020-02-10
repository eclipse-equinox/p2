/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith - bug fixing and new functionality
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This is the master test suite for all automated provisioning tests. It runs
 * every test that is suitable for running in an automated fashion as part of a
 * build. Some tests may be excluded if they require special setup (such as
 * generating metadata).
 *
 * PLEASE ADD SUITES IN THE PACKAGE NAME ORDER.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ org.eclipse.equinox.p2.tests.publisher.AllTests.class,
		org.eclipse.equinox.frameworkadmin.tests.AllTests.class,
		org.eclipse.equinox.p2.tests.ant.AllTests.class,
		org.eclipse.equinox.p2.tests.artifact.processors.AllTests.class,
		org.eclipse.equinox.p2.tests.artifact.repository.AllTests.class,
		org.eclipse.equinox.p2.tests.artifact.repository.processing.AllTests.class,
		org.eclipse.equinox.p2.tests.core.AllTests.class, org.eclipse.equinox.p2.tests.director.AllTests.class,
		org.eclipse.equinox.p2.tests.directorywatcher.AllTests.class,
		org.eclipse.equinox.p2.tests.engine.AllTests.class,
		org.eclipse.equinox.p2.tests.extensionlocation.AllTests.class, org.eclipse.equinox.p2.tests.gc.AllTests.class,
		org.eclipse.equinox.p2.tests.generator.AllTests.class, org.eclipse.equinox.p2.tests.installer.AllTests.class,
		org.eclipse.equinox.p2.tests.jarprocessor.AllTests.class,
		org.eclipse.equinox.p2.tests.metadata.AllTests.class,
		org.eclipse.equinox.p2.tests.metadata.expression.AllTests.class,
		org.eclipse.equinox.p2.tests.metadata.repository.AllTests.class,
		org.eclipse.equinox.p2.tests.mirror.AllTests.class, org.eclipse.equinox.p2.tests.omniVersion.AllTests.class,
		org.eclipse.equinox.p2.tests.planner.AllTests.class, org.eclipse.equinox.p2.tests.ql.AllTests.class,
		org.eclipse.equinox.p2.tests.repository.AllTests.class,
		org.eclipse.equinox.p2.tests.sat4j.smoke.AllTests.class,
		org.eclipse.equinox.p2.tests.simpleconfigurator.SimpleConfiguratorTests.class,
		org.eclipse.equinox.p2.tests.simpleconfigurator.manipulator.AllTests.class,
		org.eclipse.equinox.p2.tests.touchpoint.eclipse.AllTests.class,
		org.eclipse.equinox.p2.tests.touchpoint.natives.AllTests.class,
		org.eclipse.equinox.p2.tests.updatechecker.AllTests.class,
		org.eclipse.equinox.p2.tests.updatesite.AllTests.class,
		org.eclipse.equinox.p2.tests.reconciler.dropins.AllTests.class,
		org.eclipse.equinox.p2.tests.sharedinstall.AllTests.class, org.eclipse.equinox.p2.tests.full.AllTests.class })
public class AutomatedTests {
	// SuiteClasses
}
