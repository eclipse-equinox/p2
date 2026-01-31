/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.updatesite;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Performs all automated director tests.
 */
@Suite
@SelectClasses({ CategoryXMLActionTest.class, CategoryIUXMLActionTest.class, DoesNotCacheStaleData.class,
		SiteXMLActionTest.class, UpdateSiteTest.class, LocalUpdatesiteTest.class, })
public class AllTests {
//test suite
}
