/*******************************************************************************
 * Copyright (c) 2008, 2018 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   SAP AG - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher;

import org.eclipse.equinox.p2.tests.publisher.actions.ANYConfigCUsActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.AbstractPublisherActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.AccumulateConfigDataActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.AdviceFileParserTest;
import org.eclipse.equinox.p2.tests.publisher.actions.BundlesActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.CategoryPublisherTest;
import org.eclipse.equinox.p2.tests.publisher.actions.ConfigCUsActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.ContextRepositoryTest;
import org.eclipse.equinox.p2.tests.publisher.actions.DefaultCUsActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.EquinoxExecutableActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.EquinoxLauncherCUActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.FeaturesActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.JREActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.LocalUpdateSiteActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.LocalizationTests;
import org.eclipse.equinox.p2.tests.publisher.actions.ProductActionCapturingTest;
import org.eclipse.equinox.p2.tests.publisher.actions.ProductActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.ProductActionTestMac;
import org.eclipse.equinox.p2.tests.publisher.actions.ProductActionWithJRELocationTest;
import org.eclipse.equinox.p2.tests.publisher.actions.ProductContentTypeTest;
import org.eclipse.equinox.p2.tests.publisher.actions.ProductFileAdviceTest;
import org.eclipse.equinox.p2.tests.publisher.actions.ProductFileTest;
import org.eclipse.equinox.p2.tests.publisher.actions.RootFilesActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.RootIUActionTest;
import org.eclipse.equinox.p2.tests.publisher.actions.VersionAdviceTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses({ AbstractPublisherActionTest.class, AccumulateConfigDataActionTest.class,
		AdviceFileParserTest.class, ANYConfigCUsActionTest.class, BundlesActionTest.class, CategoryPublisherTest.class,
		ConfigCUsActionTest.class, ContextRepositoryTest.class, DefaultCUsActionTest.class,
		EquinoxExecutableActionTest.class, EquinoxLauncherCUActionTest.class, FeaturesActionTest.class,
		JREActionTest.class, LocalizationTests.class, LocalUpdateSiteActionTest.class, ChecksumGenerationTest.class,
		ProductActionTest.class, ProductActionCapturingTest.class,
		ProductActionTestMac.class, ProductActionWithJRELocationTest.class, ProductContentTypeTest.class,
		ProductFileAdviceTest.class, ProductFileTest.class, RootFilesActionTest.class, RootIUActionTest.class,
		GeneralPublisherTests.class, VersionAdviceTest.class })
public class AllTests {
// test suite
}