/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.tests.full;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.prov.core.helpers.ServiceHelper;
import org.eclipse.equinox.prov.director.IDirector;
import org.eclipse.equinox.prov.director.Oracle;
import org.eclipse.equinox.prov.engine.IProfileRegistry;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.prov.tests.AbstractProvisioningTest;
import org.eclipse.equinox.prov.tests.TestActivator;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class End2EndTest extends AbstractProvisioningTest {

	private IMetadataRepository[] repos;
	private IDirector director;

	protected void setUp() throws Exception {
		ServiceReference sr = TestActivator.context.getServiceReference(IDirector.class.getName());
		if (sr == null) {
			throw new RuntimeException("Director service not available");
		}
		director = createDirector();
		ServiceReference sr2 = TestActivator.context.getServiceReference(IMetadataRepositoryManager.class.getName());
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) TestActivator.context.getService(sr2);
		if (mgr == null) {
			throw new RuntimeException("Repository manager could not be loaded");
		}
		repos = mgr.getKnownRepositories();
	}

	private Profile createProfile(String profileId) {
		String installFolder = System.getProperty(Profile.PROP_INSTALL_FOLDER);
		ServiceReference profileRegSr = TestActivator.context.getServiceReference(IProfileRegistry.class.getName());
		IProfileRegistry profileRegistry = (IProfileRegistry) TestActivator.context.getService(profileRegSr);
		if (profileRegistry == null) {
			throw new RuntimeException("Profile registry service not available");
		}

		String newFlavor = System.getProperty("eclipse.prov.configurationFlavor");
		boolean doUninstall = (Boolean.TRUE.equals(Boolean.valueOf(System.getProperty("eclipse.prov.doUninstall"))));

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

	private Collection extractIUs(Profile p) {
		Collection result = new HashSet();
		Iterator it = p.getInstallableUnits();
		while (it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}

	public void testInstallSDKWithEntryPoint() {
		Profile profile1 = createProfile("profile1");
		//First we install the sdk
		IStatus s = director.install(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, profile1, "entryPoint1", new NullProgressMonitor());
		assertOK(s);
		Collection iusInstalled = extractIUs(profile1);
		IInstallableUnit firstSnapshot = getIU("profile1"); //This should represent the empty profile
		assertNotNull(firstSnapshot);
		assertNotNull(firstSnapshot.getProperty("profileIU"));
		IInstallableUnit firstEntryPoint = null;
		try {
			firstEntryPoint = getIU(IInstallableUnitConstants.ENTRYPOINT_IU_KEY, "true")[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			fail("We expect to find an entry point");
		}

		//The uninstallation should not work since there is an entyr point
		s = director.uninstall(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, profile1, new NullProgressMonitor());
		assertNotOK(s);

		//The uninstallation of the entry point should lead to an empty profile
		s = director.uninstall(new IInstallableUnit[] {firstEntryPoint}, profile1, new NullProgressMonitor());
		assertOK(s);

		assertEmptyProfile(profile1);
		IInstallableUnit[] snapshots = getIUs("profile1");
		assertEquals(2, snapshots.length);
		assertEquals(false, profile1.getIterator("sdk", VersionRange.emptyRange, null, false).hasNext());

		// Now test the rollback to a previous state, in this case we reinstall the SDK
		s = director.become(snapshots[0].equals(firstSnapshot) ? snapshots[1] : snapshots[0], profile1, new NullProgressMonitor());
		if (!s.isOK())
			fail("The become operation failed");

		assertNotNull(getIU("sdk"));
		assertEquals(firstEntryPoint, getIU(firstEntryPoint.getId()));
		Collection afterRollback = extractIUs(profile1);

		//Verify that the rollback brought back everything we had
		Collection iusInstalledCopy = new HashSet(iusInstalled);
		iusInstalled.removeAll(afterRollback);
		afterRollback.removeAll(iusInstalledCopy);
		assertEquals(0, iusInstalled.size());
		assertEquals(0, afterRollback.size());

		//Now update for the SDK itself
		Collection snapshotsBeforeUpdate = Arrays.asList(getIUs("profile1"));
		assertEquals(1, new Oracle().hasUpdate(getIU("sdk", new Version("3.3.0"))).size());
		s = director.replace(new IInstallableUnit[] {firstEntryPoint}, new IInstallableUnit[] {(IInstallableUnit) new Oracle().hasUpdate(getIU("sdk", new Version("3.3.0"))).iterator().next()}, profile1, new NullProgressMonitor());
		assertOK(s);
		assertProfileContainsAll("", profile1, new IInstallableUnit[] {getIU("sdk", new Version("3.4.0"))});
		Collection snapsshotsAfterUpdate = new ArrayList(Arrays.asList(getIUs("profile1")));
		snapsshotsAfterUpdate.removeAll(snapshotsBeforeUpdate);
		IInstallableUnit former = (IInstallableUnit) snapsshotsAfterUpdate.iterator().next();

		//Now come back to a 3.3 install
		s = director.become(former, profile1, new NullProgressMonitor());
		assertOK(s);

		//Test replace the sdk 3.3 entry point with 3.4
		assertEquals(1, new Oracle().hasUpdate(firstEntryPoint).size());
		s = director.replace(new IInstallableUnit[] {firstEntryPoint}, new IInstallableUnit[] {(IInstallableUnit) new Oracle().hasUpdate(firstEntryPoint).iterator().next()}, profile1, new NullProgressMonitor());
		assertOK(s);
		assertProfileContainsAll("", profile1, new IInstallableUnit[] {getIU("sdk", new Version("3.4.0"))});
		assertNotIUs(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, profile1.getInstallableUnits());

		//Remove everything from the profile by becoming an empty profile
		s = director.become(firstSnapshot, profile1, new NullProgressMonitor());
		assertOK(s);
		//		assertEmptyProfile(profile1);
	}

	public void testInstallSDK() {
		Profile profile2 = createProfile("profile2");
		//First we install the sdk
		IStatus s = director.install(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, profile2, null, new NullProgressMonitor());
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
		assertEquals(false, profile2.getIterator("sdk", VersionRange.emptyRange, null, false).hasNext());

		// Now test the rollback to a previous state, in this case we reinstall the SDK
		s = director.become(snapshots[0].equals(firstSnapshot) ? snapshots[1] : snapshots[0], profile2, new NullProgressMonitor());
		if (!s.isOK())
			fail("The become operation failed");

		assertNotNull(getIU("sdk"));

		//Test replace
		s = director.replace(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, new IInstallableUnit[] {getIU("sdk", new Version("3.4.0"))}, profile2, new NullProgressMonitor());
		assertOK(s);
		assertProfileContainsAll("", profile2, new IInstallableUnit[] {getIU("sdk", new Version("3.4.0"))});
		assertNotIUs(new IInstallableUnit[] {getIU("sdk", new Version("3.3.0"))}, profile2.getInstallableUnits());

		//Remove everything from the profile by becoming an empty profile
		s = director.become(firstSnapshot, profile2, new NullProgressMonitor());
		assertOK(s);
		//		assertEmptyProfile(profile2);
	}

	public IInstallableUnit[] getIU(String property, String value) {
		Collection result = new ArrayList();
		for (int i = 0; i < repos.length; i++) {
			IInstallableUnit[] ius = repos[i].getInstallableUnits(null);
			for (int j = 0; j < ius.length; j++) {
				String v = ius[j].getProperty(property);
				if (v != null && v.equals(value))
					result.add(ius[j]);
			}
		}
		return (IInstallableUnit[]) result.toArray(new IInstallableUnit[result.size()]);
	}

	public IInstallableUnit[] getIUs(String id) {
		Collection result = new ArrayList();
		for (int i = 0; i < repos.length; i++) {
			Iterator it = repos[i].getIterator(id, VersionRange.emptyRange, null, false);
			while (it.hasNext()) {
				result.add(it.next());
			}
		}
		return (IInstallableUnit[]) result.toArray(new IInstallableUnit[result.size()]);
	}

	public IInstallableUnit getIU(String id) {
		for (int i = 0; i < repos.length; i++) {
			Iterator it = repos[i].getIterator(id, VersionRange.emptyRange, null, false);
			while (it.hasNext()) {
				return (IInstallableUnit) it.next();
			}
		}
		return null;
	}

	public IInstallableUnit getIU(String id, Version v) {
		for (int i = 0; i < repos.length; i++) {
			Iterator it = repos[i].getIterator(id, new VersionRange("[" + v.toString() + "," + v.toString() + "]"), null, false);
			while (it.hasNext()) {
				return (IInstallableUnit) it.next();
			}
		}
		return null;
	}
}
