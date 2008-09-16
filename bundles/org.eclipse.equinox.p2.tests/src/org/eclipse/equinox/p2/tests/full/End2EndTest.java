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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
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
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class End2EndTest extends AbstractProvisioningTest {

	private IMetadataRepositoryManager metadataRepoManager;

	private IArtifactRepositoryManager artifactRepoManager;
	private IDirector director;

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
			return;
		} catch (MalformedURLException e) {
			return;
		}

		final String sdkID = "org.eclipse.sdk.ide";
		final Version sdkVersion = new Version("3.4.0.I20080617-2000");

		//First we install the sdk
		ProfileChangeRequest request = new ProfileChangeRequest(profile2);
		request.addInstallableUnits(new IInstallableUnit[] {getIU(sdkID, sdkVersion)});
		IStatus s = director.provision(request, null, new NullProgressMonitor());
		if (!s.isOK())
			fail("Installation of the " + sdkID + " " + sdkVersion + " failed.");

		//Uninstall the SDK
		request = new ProfileChangeRequest(profile2);
		request.removeInstallableUnits(new IInstallableUnit[] {getIU(sdkID, sdkVersion)});
		s = director.provision(request, null, new NullProgressMonitor());
		if (!s.isOK())
			fail("The uninstallation of the " + sdkID + " " + sdkVersion + " failed.");

		assertEquals(false, getInstallableUnits(profile2).hasNext()); //the profile should be empty since we uninstalled everything
		assertTrue(profile2.query(new InstallableUnitQuery(sdkID, VersionRange.emptyRange), new Collector(), null).isEmpty());
	}

	public IInstallableUnit[] getIUs(String id) {
		return (IInstallableUnit[]) metadataRepoManager.query(new InstallableUnitQuery(id, VersionRange.emptyRange), new Collector(), null).toArray(IInstallableUnit.class);
	}

	public IInstallableUnit getIU(String id) {
		Iterator it = metadataRepoManager.query(new InstallableUnitQuery(id, VersionRange.emptyRange), new Collector(), null).iterator();
		if (it.hasNext())
			return (IInstallableUnit) it.next();
		return null;
	}

	public IInstallableUnit getIU(String id, Version v) {
		Iterator it = metadataRepoManager.query(new InstallableUnitQuery(id, v), new Collector(), null).iterator();
		if (it.hasNext())
			return (IInstallableUnit) it.next();
		return null;
	}
}
