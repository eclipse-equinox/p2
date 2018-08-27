/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import junit.framework.*;

public class AllMetaReqTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllMetaReqTests.class.getName());
		suite.addTestSuite(AgentPlanTestInExternalInstance.class);
		suite.addTestSuite(AgentPlanTestInExternalInstanceForCohostedMode.class);
		suite.addTestSuite(AgentPlanTestInRunningInstance.class);
		return suite;
	}

}
