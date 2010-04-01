/*******************************************************************************
 *  Copyright (c) 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import java.net.URI;
import java.util.Set;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * Tests for the ability to install an IU that has an installer plan by
 * using the operation API.
 */
public class InstallerPlanTest extends AbstractProvisioningUITest {
	public void testInstallerPlan() throws ProvisionException {
		URI uri = getTestData("InstallHandler", "testData/installPlan").toURI();
		Set<IInstallableUnit> ius = getMetadataRepositoryManager().loadRepository(uri, getMonitor()).query(QueryUtil.createIUQuery("A"), getMonitor()).toSet();
		assertTrue("One IU", ius.size() == 1);
		InstallOperation op = new InstallOperation(getSession(), ius);
		op.setProfileId(TESTPROFILE);
		ProvisioningContext pc = new ProvisioningContext(getAgent());
		pc.setArtifactRepositories(new URI[] {uri});
		pc.setMetadataRepositories(new URI[] {uri});
		op.setProvisioningContext(pc);
		assertTrue("Should resolve", op.resolveModal(getMonitor()).isOK());
		assertTrue("Should install", op.getProvisioningJob(null).runModal(getMonitor()).isOK());
		assertFalse("Action1 should have been installed", getProfile(TESTPROFILE).query(QueryUtil.createIUQuery("Action1"), getMonitor()).isEmpty());
	}
}
