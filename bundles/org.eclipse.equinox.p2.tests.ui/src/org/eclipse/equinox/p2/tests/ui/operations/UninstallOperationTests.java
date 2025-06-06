/*******************************************************************************
 *  Copyright (c) 2010, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import java.util.HashSet;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * Tests for the ability to install an IU that has an installer plan by
 * using the operation API.
 */
public class UninstallOperationTests extends AbstractProvisioningUITest {
	public void testUninstallContactsNoRepositories() {

		HashSet<IInstallableUnit> ius = new HashSet<>();
		ius.add(top1);
		UninstallOperation op = new UninstallOperation(getSession(), ius);
		// We set the provisioning context to the same one we use for install
		ProvisioningContext pc = new ProvisioningContext(getAgent());
		pc.setArtifactRepositories(testRepoLocation);
		pc.setMetadataRepositories(testRepoLocation);
		op.setProvisioningContext(pc);
		op.setProfileId(TESTPROFILE);
		assertTrue("Should resolve", op.resolveModal(getMonitor()).isOK());

		// The provisioning context actually used should have nothing (because the first pass is to try with no repos)
		pc = op.getProvisioningPlan().getContext();
		IQueryable<IInstallableUnit> queryable = pc.getMetadata(getMonitor());
		assertTrue("metadata queryable should be empty", queryable.query(QueryUtil.ALL_UNITS, getMonitor()).isEmpty());
		IQueryable<IArtifactRepository> artifactQueryable = pc.getArtifactRepositories(getMonitor());
		IQuery<IArtifactRepository> all = new ExpressionMatchQuery<>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		assertTrue("artifact queryable should be empty", artifactQueryable.query(all, getMonitor()).isEmpty());
	}
}
