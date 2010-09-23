/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

public class RepoValidator extends AbstractProvisioningTest {
	public void testValidate() throws ProvisionException, URISyntaxException {
		URI repoLoc = new URI("http://fullmoon.ottawa.ibm.com/eclipse/updates/3.5-I-builds/");
		ServiceReference sr = TestActivator.context.getServiceReference(IPlanner.SERVICE_NAME);
		if (sr == null) {
			throw new RuntimeException("Planner service not available");
		}
		IPlanner planner = (IPlanner) TestActivator.context.getService(sr);
		if (planner == null) {
			throw new RuntimeException("Planner could not be loaded");
		}

		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (mgr == null) {
			throw new RuntimeException("Repository manager could not be loaded");
		}
		IMetadataRepository validatedRepo = mgr.loadRepository(repoLoc, null);

		Map properties = new HashMap();
		properties.put("osgi.os", "win32");
		properties.put("osgi.ws", "win32");
		properties.put("osgi.arch", "x86");
		IProfile p = createProfile("repoValidator", properties);

		IQuery q;

		q = QueryUtil.createIUQuery("org.eclipse.rcp.feature.group");

		//		q = InstallableUnitQuery.ANY;
		IQueryResult iusToTest = validatedRepo.query(q, null);

		ProvisioningContext pc = new ProvisioningContext(getAgent());
		pc.setMetadataRepositories(new URI[] {repoLoc});
		for (Iterator iterator = iusToTest.iterator(); iterator.hasNext();) {
			try {
				IInstallableUnit isInstallable = (IInstallableUnit) iterator.next();
				ProfileChangeRequest req = new ProfileChangeRequest(p);
				req.setProfileProperty("eclipse.p2.install.features", "true");
				req.addInstallableUnits(new IInstallableUnit[] {isInstallable});
				//				System.out.println("Working on: " + isInstallable);
				IStatus s = planner.getProvisioningPlan(req, pc, null).getStatus();
				if (!s.isOK()) {
					System.err.println("Can't resolve: " + isInstallable);
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				//ignore
			}
		}
	}
}
