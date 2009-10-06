/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * 
 */
public class ProvisioningUtilTest extends AbstractProvisioningUITest {
	public void testArtifactRepos() throws ProvisionException {
		URI[] artifactRepos = ProvisioningUtil.getArtifactRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < artifactRepos.length; i++)
			assertTrue(artifactRepos[i].toString() + " should be enabled", ProvisioningUtil.getArtifactRepositoryEnablement(artifactRepos[i]));

		artifactRepos = ProvisioningUtil.getArtifactRepositories(IRepositoryManager.REPOSITORIES_DISABLED);
		for (int i = 0; i < artifactRepos.length; i++)
			assertFalse(artifactRepos[i].toString() + " should be disabled", ProvisioningUtil.getArtifactRepositoryEnablement(artifactRepos[i]));

		artifactRepos = ProvisioningUtil.getArtifactRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		for (int i = 0; i < artifactRepos.length; i++)
			assertEquals("Expected non system repo " + artifactRepos[i].toString(), ProvisioningUtil.getArtifactRepositoryProperty(artifactRepos[i], IRepository.PROP_SYSTEM), Boolean.toString(false));

		artifactRepos = ProvisioningUtil.getArtifactRepositories(IRepositoryManager.REPOSITORIES_LOCAL);
		// TODO need to understand why this is failing
		// ProvisioningUtil.refreshArtifactRepositories(artifactRepos, getMonitor());

	}

	public void testMetadataRepos() throws ProvisionException {
		URI[] metadataRepos = ProvisioningUtil.getMetadataRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < metadataRepos.length; i++)
			assertTrue(metadataRepos[i].toString() + " should be enabled", ProvisioningUtil.getMetadataRepositoryEnablement(metadataRepos[i]));

		metadataRepos = ProvisioningUtil.getMetadataRepositories(IRepositoryManager.REPOSITORIES_DISABLED);
		for (int i = 0; i < metadataRepos.length; i++)
			assertFalse(metadataRepos[i].toString() + " should be disabled", ProvisioningUtil.getMetadataRepositoryEnablement(metadataRepos[i]));

		metadataRepos = ProvisioningUtil.getMetadataRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		for (int i = 0; i < metadataRepos.length; i++)
			assertEquals("Expected non system repo " + metadataRepos[i].toString(), ProvisioningUtil.getMetadataRepositoryProperty(metadataRepos[i], IRepository.PROP_SYSTEM), Boolean.toString(false));

		metadataRepos = ProvisioningUtil.getMetadataRepositories(IRepositoryManager.REPOSITORIES_LOCAL);
		ProvisioningUtil.refreshMetadataRepositories(metadataRepos, getMonitor());

	}
}
