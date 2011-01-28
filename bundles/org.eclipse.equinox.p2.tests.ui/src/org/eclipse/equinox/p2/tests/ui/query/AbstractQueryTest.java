/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

//import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.MatchQuery;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Abstract class to set up the mock query provider
 */
public abstract class AbstractQueryTest extends AbstractProvisioningTest {
	protected void setUp() throws Exception {
		super.setUp();
		// use test query provider
		// This is really not how the default policy should be used in practice,
		// but we need to reset it for the tests.
		//		ProvUI.setQueryProvider(new MockQueryProvider(getMockQuery(), ProvisioningUI.getDefaultUI()));
		// some of the test repos are set up as system repos so we need to
		// query all repos, not just non-system repos
		// TODO consider evolving these tests to distinguish between system
		// and non-system
		RepositoryTracker manipulator = ProvisioningUI.getDefaultUI().getRepositoryTracker();
		manipulator.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
		manipulator.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		RepositoryTracker manipulator = ProvisioningUI.getDefaultUI().getRepositoryTracker();
		manipulator.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		manipulator.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		//		ProvUI.setQueryProvider(null);
	}

	protected IQuery getMockQuery() {
		return new MatchQuery() {
			public boolean isMatch(Object candidate) {
				return true;
			}
		};

	}
}
