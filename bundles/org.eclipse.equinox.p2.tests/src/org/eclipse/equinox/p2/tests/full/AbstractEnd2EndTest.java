/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Abstract parent class for the End-to-End test suites.
 */
public abstract class AbstractEnd2EndTest extends AbstractProvisioningTest {

	private IMetadataRepositoryManager metadataRepoManager;
	private IArtifactRepositoryManager artifactRepoManager;
	private IDirector director;
	private ServiceTracker fwAdminTracker;
	private IProvisioningAgent end2endAgent = null;

	protected void setUp() throws Exception {
		ServiceReference<IProvisioningAgentProvider> sr = TestActivator.context.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider agentFactory = TestActivator.context.getService(sr);
		end2endAgent = agentFactory.createAgent(getTempFolder().toURI());
		metadataRepoManager = (IMetadataRepositoryManager) end2endAgent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		artifactRepoManager = (IArtifactRepositoryManager) end2endAgent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		director = (IDirector) end2endAgent.getService(IDirector.SERVICE_NAME);
	}

	protected IProfile createProfile(String profileId, String installFolder) {
		IProfileRegistry profileRegistry = (IProfileRegistry) end2endAgent.getService(IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null) {
			throw new RuntimeException("Profile registry service not available");
		}

		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, installFolder);
		EnvironmentInfo info = ServiceHelper.getService(TestActivator.getContext(), EnvironmentInfo.class);
		if (info != null)
			properties.put(IProfile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch());
		properties.put("org.eclipse.update.install.features", "true");
		properties.put(IProfile.PROP_CACHE, installFolder);
		try {
			return profileRegistry.addProfile(profileId, properties);
		} catch (ProvisionException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public void testInstallSDK() {
		File installFolder = TestActivator.getContext().getDataFile(AbstractEnd2EndTest.class.getName());
		IProfile profile2 = createProfile("End2EndProfile", installFolder.getAbsolutePath());

		//Add repository of the release
		URI repositoryLocation = getRepositoryLocation();
		try {
			metadataRepoManager.addRepository(repositoryLocation);
			metadataRepoManager.setEnabled(repositoryLocation, true);
			metadataRepoManager.loadRepository(repositoryLocation, new NullProgressMonitor());
			artifactRepoManager.addRepository(repositoryLocation);
			artifactRepoManager.setEnabled(repositoryLocation, true);
		} catch (ProvisionException e) {
			fail("Exception loading the repository.", e);
		}

		installPlatform(profile2, installFolder);
		installBogusIU(profile2, installFolder);
		installPlatformSource(profile2, installFolder);
		attemptToUninstallRCP(profile2, installFolder);
		rollbackPlatformSource(profile2, installFolder);
		//		uninstallPlatform(profile2, installFolder);
	}

	private void attemptToUninstallRCP(IProfile profile2, File installFolder) {
		IQueryResult collect = profile2.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		assertEquals(1, queryResultSize(collect));
		ProfileChangeRequest request = new ProfileChangeRequest(profile2);
		request.removeInstallableUnits(new IInstallableUnit[] {(IInstallableUnit) collect.iterator().next()});
		IStatus s = director.provision(request, null, new NullProgressMonitor());
		assertOK("Can not uninstall RCP", s);
		assertEquals(1, queryResultSize(profile2.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor())));
	}

	protected void uninstallPlatform(IProfile profile2, File installFolder) {
		System.out.println("Uninstall the platform");
		IQueryResult collect = profile2.query(QueryUtil.createIUQuery(getPlatform().getId()), new NullProgressMonitor());
		assertEquals(1, queryResultSize(collect));
		//		Collector collect2 = profile2.query(new InstallableUnitQuery("org.eclipse.platform.source.feature.group"), new Collector(), new NullProgressMonitor());
		ProfileChangeRequest request = new ProfileChangeRequest(profile2);
		request.removeInstallableUnits(new IInstallableUnit[] {(IInstallableUnit) collect.iterator().next()});//, (IInstallableUnit) collect2.iterator().next()});
		IStatus s = director.provision(request, null, new NullProgressMonitor());
		assertOK("Can not uninstall platform", s);
	}

	private void rollbackPlatformSource(IProfile profile2, File installFolder) {
		IProfileRegistry profileRegistry = (IProfileRegistry) end2endAgent.getService(IProfileRegistry.SERVICE_NAME);
		long[] timestamps = profileRegistry.listProfileTimestamps(profile2.getProfileId());
		assertEquals(3, timestamps.length);

		IProfile revertProfile = profileRegistry.getProfile(profile2.getProfileId(), timestamps[1]);

		IStatus s = director.revert(profile2, revertProfile, new ProvisioningContext(getAgent()), new NullProgressMonitor());
		assertTrue(s.isOK());

		validateInstallContent(installFolder);
		assertFalse(new File(installFolder, "configuration/org.eclipse.equinox.source/source.info").exists());
	}

	private void installPlatformSource(IProfile profile2, File installFolder) {
		VersionedId source = getPlatformSource();
		IInstallableUnit toInstall = getIU(source.getId(), source.getVersion());

		ProfileChangeRequest request = new ProfileChangeRequest(profile2);
		request.addInstallableUnits(new IInstallableUnit[] {toInstall});
		IStatus s = director.provision(request, null, new NullProgressMonitor());
		if (!s.isOK())
			fail("Installation of the " + source.getId() + " " + source.getVersion() + " failed.");

		assertProfileContainsAll("Platform source feature", profile2, new IInstallableUnit[] {toInstall});
		assertTrue(new File(installFolder, "configuration/org.eclipse.equinox.source").exists());
	}

	private void installBogusIU(IProfile profile, File installFolder) {
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		iud.setId("org.eclipse.equinox.p2.tests.bogusIU.end2end");
		iud.setVersion(Version.create("1.0.0"));
		iud.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.equinox.p2.tests.bogusIU.end2end", Version.create("1.0.0"))});
		Map data = new HashMap();
		data.put("install", "org.eclipse.equinox.p2.osgi.removeJvmArg(programArg:-XX:+UnlockDiagnosticVMOptions);");
		iud.addTouchpointData(MetadataFactory.createTouchpointData(data));
		IInstallableUnit bogusIU = MetadataFactory.createInstallableUnit(iud);
		iud.setTouchpointType(MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", Version.create("1.0.0")));
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {bogusIU});
		IStatus s = director.provision(request, null, new NullProgressMonitor());
		assertNotOK(s);
	}

	private void installPlatform(IProfile profile2, File installFolder) {
		VersionedId platform = getPlatform();
		//First we install the platform
		ProfileChangeRequest request = new ProfileChangeRequest(profile2);
		IInstallableUnit platformIU = getIU(platform.getId(), platform.getVersion());

		request.addInstallableUnits(new IInstallableUnit[] {platformIU});
		IStatus s = director.provision(request, null, new NullProgressMonitor());
		if (!s.isOK()) {
			LogHelper.log(s);
			fail("Installation of the " + platform.getId() + " " + platform.getVersion() + " failed. " + s.toString());
		}

		assertProfileContainsAll("Platform profile", profile2, new IInstallableUnit[] {platformIU});
		validateInstallContent(installFolder);
		assertFalse(new File(installFolder, "configuration/org.eclipse.equinox.source").exists());
	}

	/**
	 * Returns the IU corresponding to the given id and version. Fails if the IU could
	 * not be found. Never returns null.
	 */
	public IInstallableUnit getIU(String id, Version v) {
		final IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id, v);
		Iterator it = metadataRepoManager.query(query, null).iterator();
		if (it.hasNext())
			return (IInstallableUnit) it.next();
		//try the repository location directly - retry because eclipse.org can be flaky
		Exception failure = null;
		for (int i = 0; i < 3; i++) {
			try {
				IMetadataRepository repo = metadataRepoManager.loadRepository(getRepositoryLocation(), null);
				it = repo.query(query, null).iterator();
				if (it.hasNext())
					return (IInstallableUnit) it.next();
			} catch (ProvisionException e) {
				failure = e;
			}
		}
		if (failure == null)
			failure = new RuntimeException("IU not found");
		fail("Failed to obtain " + id + " version: " + v + " from: " + getRepositoryLocation(), failure);
		return null;//will never get here
	}

	abstract protected void validateInstallContent(File installFolder);

	abstract protected URI getRepositoryLocation();

	abstract protected VersionedId getPlatform();

	abstract protected VersionedId getPlatformSource();

	protected void assertContains(String message, String[] source, String searched) {
		for (int i = 0; i < source.length; i++) {
			if (source[i].equals(searched))
				return;
		}
		fail(message + " " + searched);
	}

	protected FrameworkAdmin getEquinoxFrameworkAdmin() {
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
				filter = TestActivator.getContext().createFilter(filterFwAdmin);
				fwAdminTracker = new ServiceTracker(TestActivator.getContext(), filter, null);
				fwAdminTracker.open();
			} catch (InvalidSyntaxException e) {
				// never happens
				e.printStackTrace();
			}
		}
		return (FrameworkAdmin) fwAdminTracker.getService();
	}

	protected static String getLauncherName(String name, String os) {
		if (os == null) {
			EnvironmentInfo info = ServiceHelper.getService(TestActivator.getContext(), EnvironmentInfo.class);
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
