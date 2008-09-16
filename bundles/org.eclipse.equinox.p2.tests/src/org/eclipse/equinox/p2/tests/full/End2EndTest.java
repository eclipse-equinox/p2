/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.tests.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class End2EndTest extends AbstractProvisioningTest {

	private IMetadataRepositoryManager metadataRepoManager;

	private IArtifactRepositoryManager artifactRepoManager;
	private IDirector director;

	private ServiceTracker fwAdminTracker;

	protected void setUp() throws Exception {
		ServiceReference sr = TestActivator.context.getServiceReference(IDirector.class.getName());
		if (sr == null)
			throw new RuntimeException("Director service not available");
		director = createDirector();
		//		planner = createPlanner();
		ServiceReference sr2 = TestActivator.context.getServiceReference(IMetadataRepositoryManager.class.getName());
		metadataRepoManager = (IMetadataRepositoryManager) TestActivator.context.getService(sr2);
		if (metadataRepoManager == null)
			throw new RuntimeException("Metadata repository manager could not be loaded");

		ServiceReference sr3 = TestActivator.context.getServiceReference(IArtifactRepositoryManager.class.getName());
		artifactRepoManager = (IArtifactRepositoryManager) TestActivator.context.getService(sr3);
		if (artifactRepoManager == null)
			throw new RuntimeException("Artifact repo manager could not be loaded");
	}

	protected IProfile createProfile(String profileId, String installFolder) {
		ServiceReference profileRegSr = TestActivator.context.getServiceReference(IProfileRegistry.class.getName());
		IProfileRegistry profileRegistry = (IProfileRegistry) TestActivator.context.getService(profileRegSr);
		if (profileRegistry == null) {
			throw new RuntimeException("Profile registry service not available");
		}

		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, installFolder + '/' + profileId);
		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(TestActivator.getContext(), EnvironmentInfo.class.getName());
		if (info != null)
			properties.put(IProfile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch());

		return createProfile(profileId, null, properties);
	}

	public void testInstallSDK() {
		File installFolder = TestActivator.getContext().getDataFile(End2EndTest.class.getName());
		IProfile profile2 = createProfile("End2EndProfile", installFolder.getAbsolutePath());

		//Add repository of the release
		try {
			metadataRepoManager.loadRepository(new URL("http://download.eclipse.org/eclipse/updates/3.4"), new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("Exception loading the repository.", e);
		} catch (MalformedURLException e) {
			//Ignore
		}

		final String sdkID = "org.eclipse.sdk.ide";
		final Version sdkVersion = new Version("3.4.0.I20080617-2000");

		//First we install the sdk
		ProfileChangeRequest request = new ProfileChangeRequest(profile2);
		request.addInstallableUnits(new IInstallableUnit[] {getIU(sdkID, sdkVersion)});
		IStatus s = director.provision(request, null, new NullProgressMonitor());
		if (!s.isOK())
			fail("Installation of the " + sdkID + " " + sdkVersion + " failed.");

		validateInstallContentFor34(new File(installFolder, "End2EndProfile"));

		//Uninstall the SDK
		request = new ProfileChangeRequest(profile2);
		request.removeInstallableUnits(new IInstallableUnit[] {getIU(sdkID, sdkVersion)});
		s = director.provision(request, null, new NullProgressMonitor());
		if (!s.isOK())
			fail("The uninstallation of the " + sdkID + " " + sdkVersion + " failed.");

		assertEquals(false, getInstallableUnits(profile2).hasNext()); //the profile should be empty since we uninstalled everything
		assertTrue(profile2.query(new InstallableUnitQuery(sdkID, VersionRange.emptyRange), new Collector(), null).isEmpty());
	}

	public IInstallableUnit getIU(String id, Version v) {
		Iterator it = metadataRepoManager.query(new InstallableUnitQuery(id, v), new Collector(), null).iterator();
		if (it.hasNext())
			return (IInstallableUnit) it.next();
		return null;
	}

	private void validateInstallContentFor34(File installFolder) {
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
		assertContains("Can't find program arg", programArgs, "--launcher.library");
		assertContains("Can't find program arg", programArgs, "-showsplash");
		assertContains("Can't find program arg", programArgs, "org.eclipse.platform");

		assertTrue(manipulator.getConfigData().getBundles().length > 130);
	}

	private void assertContains(String message, String[] source, String searched) {
		for (int i = 0; i < source.length; i++) {
			if (source[i].equals(searched))
				return;
		}
		fail(message + " " + searched);
	}

	private FrameworkAdmin getEquinoxFrameworkAdmin() {
		final String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + "=" + FrameworkAdmin.class.getName() + ")";
		final String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)";
		final String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)";
		final String filterFwAdmin = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ")";

		String FWK_ADMIN_EQ = "org.eclipse.equinox.frameworkadmin.equinox";
		Bundle b = Platform.getBundle(FWK_ADMIN_EQ);
		if (b == null)
			fail("Bundle: " + FWK_ADMIN_EQ + " is required for this test");
		try {
			b.start();
		} catch (BundleException e) {
			fail("Can't start framework admin");
		}
		if (fwAdminTracker == null) {
			Filter filter;
			try {
				filter = Activator.getContext().createFilter(filterFwAdmin);
				fwAdminTracker = new ServiceTracker(Activator.getContext(), filter, null);
				fwAdminTracker.open();
			} catch (InvalidSyntaxException e) {
				// never happens
				e.printStackTrace();
			}
		}
		return (FrameworkAdmin) fwAdminTracker.getService();
	}

	private static String getLauncherName(String name, String os) {
		if (os == null) {
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
			if (info != null)
				os = info.getOS();
		}

		if (os.equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32)) {
			IPath path = new Path(name);
			if ("exe".equals(path.getFileExtension())) //$NON-NLS-1$
				return name;
			return name + ".exe"; //$NON-NLS-1$
		}
		if (os.equals(org.eclipse.osgi.service.environment.Constants.OS_MACOSX)) {
			IPath path = new Path(name);
			if ("app".equals(path.getFileExtension())) //$NON-NLS-1$
				return name;
			StringBuffer buffer = new StringBuffer();
			buffer.append(name.substring(0, 1).toUpperCase());
			buffer.append(name.substring(1));
			buffer.append(".app/Contents/MacOS/"); //$NON-NLS-1$
			buffer.append(name.toLowerCase());
			return buffer.toString();
		}
		return name;
	}
}
