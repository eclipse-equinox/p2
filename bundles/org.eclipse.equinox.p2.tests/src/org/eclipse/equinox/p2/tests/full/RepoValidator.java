/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
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
import org.osgi.framework.BundleContext;

/*
 * Grabs some IUs (SDK, Platform) from the current repository and creates a plan to see if 
 * they can be installed.
 */
public class RepoValidator extends AbstractProvisioningTest {
	public void testValidate() throws ProvisionException {
		BundleContext context = TestActivator.getContext();
		String repositoryString = context.getProperty("org.eclipse.equinox.p2.tests.current.build.repo");
		assertNotNull("Need set the \'org.eclipse.equinox.p2.tests.current.build.repo\' property.", repositoryString);
		URI repositoryLocation = URI.create(repositoryString);

		IPlanner planner = getPlanner(getAgent());
		assertNotNull("Unable to aquire IPlanner service.", planner);

		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		assertNotNull("Unable to aquire IMetadataManager service.", manager);
		IMetadataRepository repository = manager.loadRepository(repositoryLocation, null);

		Map properties = new HashMap();
		properties.put("osgi.os", context.getProperty("osgi.os"));
		properties.put("osgi.ws", context.getProperty("osgi.ws"));
		properties.put("osgi.arch", context.getProperty("osgi.arch"));
		IProfile profile = createProfile("repoValidator", properties);

		// get the latest versions of the SDK and platform
		String[] ius = new String[] {"org.eclipse.sdk.ide", "org.eclipse.platform.ide"};
		for (String id : ius) {
			IQuery query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id));
			IQueryResult result = repository.query(query, getMonitor());
			assertFalse("SDK IU not found in repository: " + repositoryString, result.isEmpty());
			IInstallableUnit iu = (IInstallableUnit) result.iterator().next();

			ProvisioningContext provisioningContext = new ProvisioningContext(getAgent());
			provisioningContext.setMetadataRepositories(new URI[] {repositoryLocation});
			ProfileChangeRequest req = new ProfileChangeRequest(profile);
			req.setProfileProperty("eclipse.p2.install.features", "true");
			req.addInstallableUnits(new IInstallableUnit[] {iu});
			assertOK("Cannot resolve: " + iu, planner.getProvisioningPlan(req, provisioningContext, null).getStatus());
		}
	}
}
