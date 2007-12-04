/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import java.util.Iterator;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.ServiceReference;

/**
 * This test installs or uninstalls the IU defined by the property "eclipse.p2.autoInstall".
 * Metadata for the IUs to install must be generated separately before running this test.
 */
public class DirectorTest extends TestCase {

	public void testInstallIU() {
		ServiceReference sr = TestActivator.context.getServiceReference(IDirector.class.getName());
		if (sr == null) {
			throw new RuntimeException("Director service not available");
		}
		IDirector director = (IDirector) TestActivator.context.getService(sr);
		if (director == null) {
			throw new RuntimeException("Director could not be loaded");
		}

		ServiceReference sr2 = TestActivator.context.getServiceReference(IMetadataRepositoryManager.class.getName());
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) TestActivator.context.getService(sr2);
		if (mgr == null) {
			throw new RuntimeException("Repository manager could not be loaded");
		}

		String autoInstall = System.getProperty("eclipse.p2.autoInstall");
		Collector allJobs = mgr.query(new InstallableUnitQuery(autoInstall, VersionRange.emptyRange), new Collector(), null);

		String installFolder = System.getProperty(Profile.PROP_INSTALL_FOLDER);
		ServiceReference profileRegSr = TestActivator.context.getServiceReference(IProfileRegistry.class.getName());
		IProfileRegistry profileRegistry = (IProfileRegistry) TestActivator.context.getService(profileRegSr);
		if (profileRegistry == null) {
			throw new RuntimeException("Profile registry service not available");
		}

		String profileId = installFolder;
		String newFlavor = System.getProperty("eclipse.p2.configurationFlavor");
		boolean doUninstall = (Boolean.TRUE.equals(Boolean.valueOf(System.getProperty("eclipse.p2.doUninstall"))));

		Profile p = profileRegistry.getProfile(profileId);
		if (p == null) {
			if (doUninstall) {
				throw new RuntimeException("Uninstalling from a nonexistent profile");
			}
			p = new Profile(installFolder); //Typically a profile would receive a name.
			p.setValue(Profile.PROP_INSTALL_FOLDER, installFolder);
			p.setValue(Profile.PROP_FLAVOR, newFlavor);
			// TODO: should we add the profile to the registry here? instead of after test?
		} else {
			String currentFlavor = p.getValue(Profile.PROP_FLAVOR);
			if (currentFlavor != null && !currentFlavor.endsWith(newFlavor)) {
				throw new RuntimeException("Install flavor not consistent with profile flavor");
			} else if (currentFlavor == null) {
				p.setValue(Profile.PROP_FLAVOR, newFlavor);
			}
		}

		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(TestActivator.getContext(), EnvironmentInfo.class.getName());
		if (info != null)
			p.setValue(Profile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch());

		IInstallableUnit[] allRoots = new IInstallableUnit[1];
		IStatus operationStatus = null;
		if (!allJobs.isEmpty()) {
			allRoots[0] = (IInstallableUnit) allJobs.iterator().next();
			if (!doUninstall) {
				operationStatus = director.install(allRoots, p, new NullProgressMonitor());
			} else {
				operationStatus = director.uninstall(allRoots, p, new NullProgressMonitor());
			}
		} else {
			operationStatus = new Status(IStatus.INFO, "org.eclipse.equinox.p2.director.test", "The installable unit '" + System.getProperty("eclipse.p2.autoInstall") + "' has not been found");
		}

		if (operationStatus.isOK()) {
			System.out.println((!doUninstall ? "installation" : "uninstallation") + " complete");
			if (profileRegistry.getProfile(p.getProfileId()) == null) {
				profileRegistry.addProfile(p);
			} else {
				// TODO: should delete the profile if it is 'empty'
				// if (p.isEmpty()) {
				//     profileRegistry.removeProfile(p);
				// }
			}
		} else {
			System.out.println((!doUninstall ? "installation" : "uninstallation") + " failed. " + operationStatus);
			LogHelper.log(operationStatus);
		}
		if (!operationStatus.isOK())
			fail("The installation has failed");

		IInstallableUnit[] result = (IInstallableUnit[]) p.query(new InstallableUnitQuery(allRoots[0].getId(), VersionRange.emptyRange), new Collector(), null).toArray(IInstallableUnit.class);
		assertEquals(result.length, (!doUninstall ? 1 : 0));
		result = (IInstallableUnit[]) p.query(new InstallableUnitQuery("toolingdefault", VersionRange.emptyRange), new Collector(), null).toArray(IInstallableUnit.class);

		ensureFragmentAssociationIsNotPersisted(mgr);
	}

	private void ensureFragmentAssociationIsNotPersisted(IMetadataRepositoryManager mgr) {
		//Test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=177661
		AgentLocation location = (AgentLocation) ServiceHelper.getService(TestActivator.getContext(), AgentLocation.class.getName());
		mgr.removeRepository(location.getMetadataRepositoryURL());
		IMetadataRepository repo = null;
		repo = mgr.loadRepository(location.getMetadataRepositoryURL(), null);
		Iterator it = repo.query(new InstallableUnitQuery("org.eclipse.equinox.simpleconfigurator", VersionRange.emptyRange), new Collector(), null).iterator();
		if (!it.hasNext())
			return;
		IInstallableUnit sc = (IInstallableUnit) it.next();
		if (sc.isResolved())
			fail("The repository should not store resolved installable units");
	}
}
