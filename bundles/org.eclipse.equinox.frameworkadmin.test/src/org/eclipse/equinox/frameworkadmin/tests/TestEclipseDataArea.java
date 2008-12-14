/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

public class TestEclipseDataArea extends FwkAdminAndSimpleConfiguratorTest {
	Manipulator m = null;

	public TestEclipseDataArea(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		m = createMinimalConfiguration(TestEclipseDataArea.class.getName());
	}

	public void testp2DataArea() throws IOException {
		m.getConfigData().setProperty("eclipse.p2.data.area", "@config.dir/../p2");
		m.save(false);
		assertContent(getConfigIni(), "@config.dir/../p2");
		m.load();
		m.save(false);
		assertContent(getConfigIni(), "@config.dir/../p2");

		m.getConfigData().setProperty("eclipse.p2.data.area", new File(getConfigurationFolder(), "p2").getAbsoluteFile().toURI().toString());
		m.save(false);
		assertContent(getConfigIni(), "@config.dir/p2");
		m.load();
		m.save(false);
		assertContent(getConfigIni(), "@config.dir/p2");

		m.getConfigData().setProperty("eclipse.p2.data.area", new File(getConfigurationFolder(), "../p2").getAbsoluteFile().toURI().toString());
		m.save(false);
		assertContent(getConfigIni(), "@config.dir/../p2");
		m.load();
		m.save(false);
		assertContent(getConfigIni(), "@config.dir/../p2");

		m.getConfigData().setProperty("eclipse.p2.data.area", "file:/d:/tmp/fo%20o/bar/p2");
		m.save(false);
		assertContent(getConfigIni(), "/tmp/fo o/bar/p2");
		assertNotContent(getConfigIni(), "@config.dir");
		m.load();
		m.save(false);
		assertContent(getConfigIni(), "/tmp/fo o/bar/p2");
		assertNotContent(getConfigIni(), "@config.dir");

	}
}
