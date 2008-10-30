/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui;

import java.io.File;
import java.net.URI;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Abstract class to set up the colocated UI test repo
 */
public abstract class AbstractProvisioningUITest extends AbstractProvisioningTest {

	protected static final String TEST_REPO_PATH = "testRepos/updateSite/";
	protected IMetadataRepositoryManager metaManager;
	protected IArtifactRepositoryManager artifactManager;
	protected URI testRepoLocation;

	protected void setUp() throws Exception {
		super.setUp();
		metaManager = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.context, IMetadataRepositoryManager.class.getName());
		artifactManager = (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.context, IArtifactRepositoryManager.class.getName());
		File site = new File(TestActivator.TEST_DATA_PATH, TEST_REPO_PATH);
		testRepoLocation = site.toURI();
		metaManager.addRepository(testRepoLocation);
		artifactManager.addRepository(testRepoLocation);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		metaManager.removeRepository(testRepoLocation);
		artifactManager.removeRepository(testRepoLocation);
	}

	protected boolean managerContains(IRepositoryManager manager, URI location) {
		URI[] locations = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].equals(location))
				return true;
		}
		return false;
	}
}
