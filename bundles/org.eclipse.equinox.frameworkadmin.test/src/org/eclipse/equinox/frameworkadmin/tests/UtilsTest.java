/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

public class UtilsTest extends AbstractFwkAdminTest {

	/**
	 * @param name
	 */
	public UtilsTest(String name) {
		super(name);
	}

	public void test_getEclipseRealLocation() throws Exception {
		File installFolder = Activator.getContext().getDataFile("212361");

		File plugins = new File(installFolder, "plugins");
		File foo1 = new File(plugins, "org.foo_1.2.3.abc");
		File foo2 = new File(plugins, "org.foo_1.2.4.xyz");
		File foo_64 = new File(plugins, "org.foo.x86_64_1.2.3");
		foo1.mkdirs();
		foo2.mkdirs();
		foo_64.mkdirs();

		Manipulator manipulator = getFrameworkManipulator(new File(installFolder, "configuration"), new File(installFolder, "eclipse"));

		assertEquals(FileUtils.getEclipseRealLocation(manipulator, "org.foo"), foo2.toURI());
		assertEquals(FileUtils.getEclipseRealLocation(manipulator, "org.foo_1.2.3.abc"), foo1.toURL());
		assertEquals(FileUtils.getEclipseRealLocation(manipulator, "org.foo.x86_64"), foo_64.toURL());

		File other = new File(installFolder, "other/org.foo_1.2.4");
		other.mkdirs();
		manipulator.getConfigData().setFwDependentProp("osgi.syspath", other.getParentFile().getAbsolutePath());
		assertEquals(FileUtils.getEclipseRealLocation(manipulator, "org.foo"), other.toURL());
	}
}
