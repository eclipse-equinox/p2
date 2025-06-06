/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
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
 *     Red Hat, Inc. - fragment support
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.File;
import org.eclipse.equinox.internal.simpleconfigurator.Activator;
import org.eclipse.equinox.p2.tests.sharedinstall.AbstractSharedInstallTest;

public class NonExclusiveModeExtendedConfigured extends NonExclusiveModeExtended {

	private File testData;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		testData = getTempFolder();
		copy("preparing testData", getTestData("simpleconfigurator extensions", "testData/simpleConfiguratorExtendedTest/extensions"), testData);
		Activator.EXTENSIONS = testData.toString();
		AbstractSharedInstallTest.setReadOnly(testData, true);
	}

	@Override
	protected void tearDown() throws Exception {
		Activator.EXTENSIONS = null;
		AbstractSharedInstallTest.setReadOnly(testData, false);
		testData.delete();
		super.tearDown();
	}
}
