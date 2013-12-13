/*******************************************************************************
 * Copyright (c) 2012,2013 Red Hat, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *      Red Hat, Inc. - initial API and implementation
 *      Ericsson AB - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import org.eclipse.equinox.internal.simpleconfigurator.Activator;

public class SimpleConfiguratorTestExtended extends SimpleConfiguratorTest {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		// enable extended mode
		Activator.EXTENDED = true;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Activator.EXTENDED = false;
	}

}
