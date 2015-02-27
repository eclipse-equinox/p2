/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.ServiceReference;

/**
 * This test installs or uninstalls the IU defined by the property "eclipse.p2.autoInstall".
 * Metadata for the IUs to install must be generated separately before running this test.
 */
public class DirectorTest extends AbstractProvisioningTest {

	public void testInstallIU() {
		ServiceReference<IDirector> sr = TestActivator.context.getServiceReference(IDirector.class);
		if (sr == null) {
			throw new RuntimeException("Director service not available");
		}
		IDirector director = TestActivator.context.getService(sr);
		if (director == null) {
			throw new RuntimeException("Director could not be loaded");
		}

		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (mgr == null) {
			throw new RuntimeException("Repository manager could not be loaded");
		}

		String autoInstall = System.getProperty("eclipse.p2.autoInstall");
		IQueryResult allJobs = mgr.query(QueryUtil.createIUQuery(autoInstall, VersionRange.emptyRange), null);

		String installFolder = System.getProperty(IProfile.PROP_INSTALL_FOLDER);
		IProfileRegistry profileRegistry = getProfileRegistry();
		if (profileRegistry == null) {
			throw new RuntimeException("Profile registry service not available");
		}

		boolean doUninstall = (Boolean.TRUE.equals(Boolean.valueOf(System.getProperty("eclipse.p2.doUninstall"))));

		IProfile p = null;
		if (doUninstall) {
			p = profileRegistry.getProfile(installFolder);
			if (p == null)
				throw new RuntimeException("Uninstalling from a nonexistent profile");
		} else {
			Map properties = new HashMap();
			properties.put(IProfile.PROP_INSTALL_FOLDER, installFolder);
			EnvironmentInfo info = ServiceHelper.getService(TestActivator.getContext(), EnvironmentInfo.class);
			if (info != null)
				properties.put(IProfile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch());

			p = createProfile(installFolder, properties);
		}

		IInstallableUnit[] allRoots = new IInstallableUnit[1];
		IStatus operationStatus = null;
		if (!allJobs.isEmpty()) {
			allRoots[0] = (IInstallableUnit) allJobs.iterator().next();
			ProfileChangeRequest request = new ProfileChangeRequest(p);
			if (!doUninstall)
				request.addInstallableUnits(allRoots);
			else
				request.removeInstallableUnits(allRoots);
			operationStatus = director.provision(request, null, null);
		} else {
			operationStatus = new Status(IStatus.INFO, "org.eclipse.equinox.internal.provisional.p2.director.test", "The installable unit '" + System.getProperty("eclipse.p2.autoInstall") + "' has not been found");
		}

		if (!operationStatus.isOK())
			fail("The installation has failed");

		IInstallableUnit[] result = p.query(QueryUtil.createIUQuery(allRoots[0].getId(), VersionRange.emptyRange), null).toArray(IInstallableUnit.class);
		assertEquals(result.length, (!doUninstall ? 1 : 0));
		result = p.query(QueryUtil.createIUQuery("toolingdefault", VersionRange.emptyRange), null).toArray(IInstallableUnit.class);
	}
}
