/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug329279 extends AbstractProvisioningTest {

	private IProfile profile;
	private IPlanner planner;

	public void testInstallSourceFeature() throws ProvisionException, OperationCanceledException {
		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(getTestData("Repo for 329279", "testData/bug329279/repo/").toURI(), new NullProgressMonitor());
		IInstallableUnit sdk = repo.query(QueryUtil.createIUQuery("org.eclipse.sdk.ide"), new NullProgressMonitor()).iterator().next();
		assertNotNull(sdk);
		assertFalse(repo.query(QueryUtil.createIUQuery("org.eclipse.equinox.p2.user.ui.source.feature.group"), new NullProgressMonitor()).isEmpty());

		IProfileChangeRequest req = planner.createChangeRequest(profile);
		req.add(sdk);

		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, new NullProgressMonitor());
		assertFalse(plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.equinox.p2.user.ui.source.feature.group"), new NullProgressMonitor()).isEmpty());
	}
}
