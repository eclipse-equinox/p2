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

import org.eclipse.equinox.internal.simpleconfigurator.Activator;

public class BundlesTxtTestExtended extends BundlesTxtTest {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		// enable extended mode
		Activator.EXTENDED = true;
		System.setProperty("p2.fragments", "true");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Activator.EXTENDED = false;
		System.setProperty("p2.fragments", "false");
	}
}
