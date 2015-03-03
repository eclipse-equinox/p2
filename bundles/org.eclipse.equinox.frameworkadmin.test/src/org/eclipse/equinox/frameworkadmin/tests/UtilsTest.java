/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.net.URI;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.osgi.service.environment.Constants;

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
		File fooWithSpaces = new File(plugins, "alotof/s p a c e s/org.foo_1.2.3.abc");
		foo1.mkdirs();
		foo2.mkdirs();
		foo_64.mkdirs();
		fooWithSpaces.mkdirs();

		Manipulator manipulator = getFrameworkManipulator(new File(installFolder, "configuration"), new File(installFolder, "eclipse"));

		
		assertEquals(FileUtils.getEclipseRealLocation(manipulator, "org.foo"), foo2.toURI());
		assertEquals(FileUtils.getEclipseRealLocation(manipulator, "org.foo_1.2.3.abc"), foo1.toURI());
		assertEquals(FileUtils.getEclipseRealLocation(manipulator, "org.foo.x86_64"), foo_64.toURI());
			
		assertEquals(FileUtils.getEclipseRealLocation(manipulator, plugins.toURI().toString() + "alotof/s%20p%20a%20c%20e%20s/org.foo_1.2.3.abc/"), fooWithSpaces.toURI());

		File other = new File(installFolder, "other/org.foo_1.2.4");
		other.mkdirs();
		manipulator.getConfigData().setProperty("osgi.syspath", other.getParentFile().getAbsolutePath());
		assertEquals(FileUtils.getEclipseRealLocation(manipulator, "org.foo"), other.toURI());
	}
	
	public void testMacRealLocation() throws Exception {
		File installFolder = Activator.getContext().getDataFile("280007/Eclipse.app/Contents/Eclipse/");

		File plugins = new File(installFolder, "plugins");
		File foo = new File(plugins, "org.foo_1.2.3.abc");
		foo.mkdirs();

		Manipulator manipulator = getFrameworkManipulator(new File(installFolder, "configuration"), new File(installFolder, "../MacOS/eclipse"));
		manipulator.getLauncherData().setOS(Constants.OS_MACOSX);
		URI res = FileUtils.getEclipseRealLocation(manipulator, "org.foo");
		assertEquals(res, foo.toURI());
	}
}
