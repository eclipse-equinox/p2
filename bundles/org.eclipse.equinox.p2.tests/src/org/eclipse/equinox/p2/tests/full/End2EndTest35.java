/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.p2.metadata.VersionedId;

/**
 * Test installing 3.5.
 */
public class End2EndTest35 extends AbstractEnd2EndTest {

	protected void validateInstallContent(File installFolder) {
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();
		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(new File(installFolder, "configuration"));
		launcherData.setLauncher(new File(installFolder, getLauncherName("eclipse", Platform.getOS())));
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			fail("Error loading the configuration", e);
		} catch (FrameworkAdminRuntimeException e) {
			fail("Error loading the configuration", e);
		} catch (IOException e) {
			fail("Error loading the configuration", e);
		}

		assertContains("Can't find VM arg", manipulator.getLauncherData().getJvmArgs(), "-Xms40m");
		assertContains("Can't find VM arg", manipulator.getLauncherData().getJvmArgs(), "-Xmx256m");

		String[] programArgs = manipulator.getLauncherData().getProgramArgs();
		assertContains("Can't find program arg", programArgs, "-startup");
		assertContains("Can't find program arg", programArgs, "-showsplash");
		assertContains("Can't find program arg", programArgs, "org.eclipse.platform");

		assertTrue(manipulator.getConfigData().getBundles().length > 50);

		assertTrue(new File(installFolder, "plugins").exists());
		assertTrue(new File(installFolder, "features").exists());

	}

	protected URI getRepositoryLocation() {
		return URI.create("http://download.eclipse.org/eclipse/updates/3.5");
	}

	protected VersionedId getPlatform() {
		return new VersionedId("org.eclipse.platform.ide", "3.5.0.I20090611-1540");
	}

	protected VersionedId getPlatformSource() {
		return new VersionedId("org.eclipse.platform.source.feature.group", "3.5.0.v20090611a-9gEeG1HFtQcmRThO4O3aR_fqSMvJR2sJ");
	}

}
