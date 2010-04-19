/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug306279 extends AbstractProvisioningTest {

	public void testGreedy() throws ProvisionException, OperationCanceledException, URISyntaxException {
		IMetadataRepository repo1 = getMetadataRepositoryManager().loadRepository(new URI("http://download.eclipse.org/releases/helios"), null);
		assertFalse(repo1.query(QueryUtil.createIUQuery("org.eclipse.rap.jface.databinding"), new NullProgressMonitor()).isEmpty());
		IMetadataRepository repo2 = getMetadataRepositoryManager().loadRepository(new URI("http://download.eclipse.org/rt/riena/updatesites/rienatoolbox-a"), null);

		IPlanner planner = getPlanner(getAgent());
		IProfile profile = createProfile(getName());
		IProfileChangeRequest request = new ProfileChangeRequest(profile);
		Set<IInstallableUnit> ius = repo2.query(QueryUtil.createIUQuery("org.eclipse.riena.toolbox.feature.feature.group"), new NullProgressMonitor()).toUnmodifiableSet();
		request.addAll(ius);
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		ctx.setMetadataRepositories(new URI[] {new URI("http://download.eclipse.org/releases/helios"), new URI("http://download.eclipse.org/rt/riena/updatesites/rienatoolbox-a")});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());

		assertOK("resolution failed", plan.getStatus());
		assertEquals(0, plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.rap.jface.databinding"), new NullProgressMonitor()).toUnmodifiableSet().size());
		System.out.println(plan);
	}
}
