/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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
import java.io.IOException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdminRuntimeException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

public class TestVMArg  extends FwkAdminAndSimpleConfiguratorTest {

	private Manipulator m;

	public TestVMArg(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws  Exception {
		super.setUp();
		m = createMinimalConfiguration(TestEclipseDataArea.class.getName());
	}
	
	public void testVMInsideInstall() throws FrameworkAdminRuntimeException, IOException {
		//Test VM path in the install folder
		File jreLocation = new File(m.getLauncherData().getLauncher().getParentFile(), "jre");
		m.getLauncherData().setJvm(jreLocation);
		m.save(false);
		assertNotContent(getLauncherConfigFile(), jreLocation.getAbsolutePath());
		assertContent(getLauncherConfigFile(), "jre");
		assertContent(m.getLauncherData().getLauncherConfigLocation(), "-vm");
		assertContent(m.getLauncherData().getLauncherConfigLocation(), "jre");
		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "file:");
		m.load();
		assertEquals(jreLocation, m.getLauncherData().getJvm());

		m.getLauncherData().setJvm(null);
		m.save(false);
		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "-vm");
		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "jre");
	}
	
//	public void testVMInsideInstall_MacOS() throws Exception {
//		m = createMinimalConfiguration(TestEclipseDataArea.class.getName(), Constants.OS_MACOSX);
//		final String expectedRelativePath = "../../../jre";
//
//		File jreLocation = new File(m.getLauncherData().getLauncher().getParentFile(), expectedRelativePath);
//		m.getLauncherData().setJvm(jreLocation);
//		m.save(false);
//
//		File launcherConfigFile = getLauncherConfigFile();
//		assertNotContent("No absolute JRE path must be present in " + launcherConfigFile, launcherConfigFile, jreLocation.getAbsolutePath());
//		assertContent("Relative JRE path must be present in " + launcherConfigFile, launcherConfigFile, expectedRelativePath);
//	}

//	public void testVMInsideInstall_MacOS_BundledLayout() throws Exception {
//		m = createMinimalConfiguration(TestEclipseDataArea.class.getName(), EclipseLauncherParser.MACOSX_BUNDLED);
//		// note the difference the traditional layout: one segment less
//		final String expectedRelativePath = "../../jre";
//
//		File jreLocation = new File(m.getLauncherData().getLauncher().getParentFile(), expectedRelativePath);
//		m.getLauncherData().setJvm(jreLocation);
//		m.save(false);
//
//		File launcherConfigFile = getLauncherConfigFile();
//		assertNotContent("No absolute JRE path must be present in " + launcherConfigFile, launcherConfigFile, jreLocation.getAbsolutePath());
//		assertContent("Relative JRE path must be present in " + launcherConfigFile, launcherConfigFile, expectedRelativePath);
//	}

	public void testVMOutsideInstall() throws FrameworkAdminRuntimeException, IOException {
		//Test VM path in the install folder
		File jreLocation = new File(m.getLauncherData().getLauncher().getParentFile(), "../../jre").getCanonicalFile();
		m.getLauncherData().setJvm(jreLocation);
		m.save(false);
		assertContent(getLauncherConfigFile(), jreLocation.getAbsolutePath().replace('\\','/'));
		assertContent(m.getLauncherData().getLauncherConfigLocation(), "-vm");
		assertContent(m.getLauncherData().getLauncherConfigLocation(), "jre");
		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "file:");
		m.load();
		assertEquals(jreLocation, m.getLauncherData().getJvm());
	}

	public void test269502() throws FrameworkAdminRuntimeException, IOException {
		//Test VM path in the install folder
		String winPath = "c:/ibm5sr3/bin";
		String linuxPath = "/Users/Pascal/ibm5sr3/bin";
		String chosenPath = Platform.getOS().equals("win32") ? winPath : linuxPath; 
		File jreLocation =  new File(chosenPath);
		m.getLauncherData().setJvm(jreLocation);
		m.save(false);
		assertContent(getLauncherConfigFile(), chosenPath);
		assertContent(m.getLauncherData().getLauncherConfigLocation(), "-vm");
		assertContent(m.getLauncherData().getLauncherConfigLocation(), chosenPath);
		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "file:");
		m.load();
		assertEquals(jreLocation, m.getLauncherData().getJvm());
	}

//	public void test269502_MacOS() throws Exception {
//		m = createMinimalConfiguration(TestEclipseDataArea.class.getName(), Constants.OS_MACOSX);
//
//		//Test VM path in the install folder
//		String chosenPath = "/Users/Pascal/ibm5sr3/bin";
//		File jreLocation =  new File(chosenPath);
//		m.getLauncherData().setJvm(jreLocation);
//		m.save(false);
//		assertContent(getLauncherConfigFile(), chosenPath);
//		assertContent(m.getLauncherData().getLauncherConfigLocation(), "-vm");
//		assertContent(m.getLauncherData().getLauncherConfigLocation(), chosenPath);
//		assertNotContent(m.getLauncherData().getLauncherConfigLocation(), "file:");
//		m.load();
//		assertEquals(jreLocation, m.getLauncherData().getJvm());
//	}

	/**
	 * But 282303: 
	 * Have -vm ../jre as program arguments.
	 * See them vanish during the save operation of the manipulator 
	 * 
	 * @throws FrameworkAdminRuntimeException
	 * @throws IOException
	 */
	public void test282303() throws FrameworkAdminRuntimeException, IOException {
		assertNotContent(getLauncherConfigFile(), "-vm");
		assertNotContent(getLauncherConfigFile(), "../mylocation");
		assertNotContent(getLauncherConfigFile(), "-otherarg");
		m.getLauncherData().addProgramArg("-vm");
		m.getLauncherData().addProgramArg("../mylocation");
		m.getLauncherData().addProgramArg("-otherarg");
		m.getLauncherData().setJvm(new File("../mylocation"));
		m.save(false);
		m.load();
		String[] args = m.getLauncherData().getProgramArgs();
		boolean found = false;
		for(int i = 0; i < args.length; i++) {
			if("-vm".equals(args[i]) && i != args.length - 1) {
				if("../mylocation".equals(args[++i])) {
					found = true;
					break;
				}
			}
		}
		assertTrue("Can't find vm argument.", found);
	}
}
