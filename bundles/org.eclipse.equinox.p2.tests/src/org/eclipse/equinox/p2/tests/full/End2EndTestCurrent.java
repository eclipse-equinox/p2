/*******************************************************************************
 *  Copyright (c) 2011 IBM Corporation and others.
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
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Test installing the latest available platform.
 */
public class End2EndTestCurrent extends AbstractEnd2EndTest {

	URI repositoryLocation;
	VersionedId platform, platformSource;

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
		assertContains("Can't find VM arg", manipulator.getLauncherData().getJvmArgs(), "-Xmx384m");

		String[] programArgs = manipulator.getLauncherData().getProgramArgs();
		assertContains("Can't find program arg", programArgs, "-startup");
		assertContains("Can't find program arg", programArgs, "-showsplash");
		assertContains("Can't find program arg", programArgs, "org.eclipse.platform");

		assertTrue(manipulator.getConfigData().getBundles().length > 50);

		assertTrue(new File(installFolder, "plugins").exists());
		assertTrue(new File(installFolder, "features").exists());

	}

	protected URI getRepositoryLocation() {
		if (repositoryLocation == null) {
			String repository = TestActivator.getContext().getProperty("org.eclipse.equinox.p2.tests.current.build.repo");
			assertNotNull("Need set the \'org.eclipse.equinox.p2.tests.current.build.repo\' property.", repository);
			repositoryLocation = URI.create(repository);
		}
		return repositoryLocation;
	}

	/*
	 * Look up the given IU id in the repo
	 */
	private IInstallableUnit getIU(String id) {
		IMetadataRepository repo = null;
		try {
			repo = loadMetadataRepository(getRepositoryLocation());
		} catch (ProvisionException e) {
			fail("Problem loading repository: " + getRepositoryLocation(), e);
		}
		IQueryResult result = repo.query(QueryUtil.createIUQuery(id), getMonitor());
		if (result.isEmpty())
			fail("Unable to load iu: \'" + id + "\' from repository: " + getRepositoryLocation());
		return (IInstallableUnit) result.iterator().next();
	}

	protected VersionedId getPlatform() {
		if (platform == null) {
			IInstallableUnit iu = getIU("org.eclipse.platform.ide");
			platform = new VersionedId(iu.getId(), iu.getVersion());
		}
		return platform;
	}

	protected VersionedId getPlatformSource() {
		if (platformSource == null) {
			IInstallableUnit iu = getIU("org.eclipse.platform.source.feature.group");
			platformSource = new VersionedId(iu.getId(), iu.getVersion());
		}
		return platformSource;
	}
}
