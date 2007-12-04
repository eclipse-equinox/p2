/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.director.IPlanner;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class End2EndTest extends AbstractProvisioningTest {

	private IMetadataRepositoryManager repoManager;
	private IDirector director;
	private IPlanner planner;

	protected void setUp() throws Exception {
		ServiceReference sr = TestActivator.context.getServiceReference(IDirector.class.getName());
		if (sr == null)
			throw new RuntimeException("Director service not available");
		director = createDirector();
		planner = createPlanner();
		ServiceReference sr2 = TestActivator.context.getServiceReference(IMetadataRepositoryManager.class.getName());
		repoManager = (IMetadataRepositoryManager) TestActivator.context.getService(sr2);
		if (repoManager == null)
			throw new RuntimeException("Repository manager could not be loaded");
	}

	protected Profile createProfile(String profileId) {
		String installFolder = System.getProperty(Profile.PROP_INSTALL_FOLDER);
		ServiceReference profileRegSr = TestActivator.context.getServiceReference(IProfileRegistry.class.getName());
		IProfileRegistry profileRegistry = (IProfileRegistry) TestActivator.context.getService(profileRegSr);
		if (profileRegistry == null) {
			throw new RuntimeException("Profile registry service not available");
		}

		String newFlavor = System.getProperty("eclipse.p2.configurationFlavor");
		boolean doUninstall = (Boolean.TRUE.equals(Boolean.valueOf(System.getProperty("eclipse.p2.doUninstall"))));

		Profile profile1 = profileRegistry.getProfile(profileId);
		if (profile1 == null) {
			if (doUninstall) {
				throw new RuntimeException("Uninstalling from a nonexistent profile");
			}
			profile1 = new Profile(profileId); //Typically a profile would receive a name.
			profile1.setValue(Profile.PROP_INSTALL_FOLDER, installFolder + '/' + profileId);
			profile1.setValue(Profile.PROP_FLAVOR, newFlavor);
			// TODO: should we add the profile to the registry here? instead of after test?
		} else {
			String currentFlavor = profile1.getValue(Profile.PROP_FLAVOR);
			if (currentFlavor != null && !currentFlavor.endsWith(newFlavor)) {
				throw new RuntimeException("Install flavor not consistent with profile flavor");
			} else if (currentFlavor == null) {
				profile1.setValue(Profile.PROP_FLAVOR, newFlavor);
			}
		}

		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(TestActivator.getContext(), EnvironmentInfo.class.getName());
		if (info != null)
			profile1.setValue(Profile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch());

		return profile1;
	}

	public void testInstallSDK() {
		Profile profile2 = createProfile("profile2");
		//First we install the sdk
		IStatus s = director.install(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, profile2, new NullProgressMonitor());
		if (!s.isOK())
			fail("Installation failed");
		IInstallableUnit firstSnapshot = getIU("profile2"); //This should represent the empty profile
		assertNotNull(firstSnapshot);
		assertNotNull(firstSnapshot.getProperty("profileIU"));

		//Uninstall the SDK
		s = director.uninstall(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, profile2, new NullProgressMonitor());
		if (!s.isOK())
			fail("The uninstallation has failed and it was not expected");

		assertEquals(false, profile2.getInstallableUnits().hasNext()); //the profile should be empty since we uninstalled everything
		IInstallableUnit[] snapshots = getIUs("profile2");
		assertTrue("snap" + snapshots.length, snapshots.length >= 2);//TODO Normally here it should be 2!!!
		assertTrue(profile2.query(new InstallableUnitQuery("sdk", VersionRange.emptyRange), new Collector(), null).isEmpty());

		// Now test the rollback to a previous state, in this case we reinstall the SDK
		s = director.become(snapshots[0].equals(firstSnapshot) ? snapshots[1] : snapshots[0], profile2, new NullProgressMonitor());
		if (!s.isOK())
			fail("The become operation failed");

		assertNotNull(getIU("sdk"));

		//Test replace
		s = director.replace(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, planner.updatesFor(getIU("sdk", new Version("3.3.0"))), profile2, new NullProgressMonitor());
		assertOK(s);
		assertProfileContainsAll("", profile2, new IInstallableUnit[] {getIU("sdk", new Version("3.4.0"))});
		assertNotIUs(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, profile2.getInstallableUnits());

		//Remove everything from the profile by becoming an empty profile
		s = director.become(firstSnapshot, profile2, new NullProgressMonitor());
		assertOK(s);
		//		assertEmptyProfile(profile2);
	}

	public IInstallableUnit[] getIUs(String id) {
		return (IInstallableUnit[]) repoManager.query(new InstallableUnitQuery(id, VersionRange.emptyRange), new Collector(), null).toArray(IInstallableUnit.class);
	}

	public IInstallableUnit getIU(String id) {
		Iterator it = repoManager.query(new InstallableUnitQuery(id, VersionRange.emptyRange), new Collector(), null).iterator();
		if (it.hasNext())
			return (IInstallableUnit) it.next();
		return null;
	}

	public IInstallableUnit getIU(String id, Version v) {
		Iterator it = repoManager.query(new InstallableUnitQuery(id, v), new Collector(), null).iterator();
		if (it.hasNext())
			return (IInstallableUnit) it.next();
		return null;
	}
}
