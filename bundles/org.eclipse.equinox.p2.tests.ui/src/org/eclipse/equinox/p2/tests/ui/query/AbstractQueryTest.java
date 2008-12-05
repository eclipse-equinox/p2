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
package org.eclipse.equinox.p2.tests.ui.query;

import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Abstract class to set up the mock query provider
 */
public abstract class AbstractQueryTest extends AbstractProvisioningTest {
	protected void setUp() throws Exception {
		super.setUp();
		// use test query provider
		// This is really not how the default policy should be used in practice,
		// but we need to reset it for the tests.
		Policy.getDefault().setQueryProvider(new MockQueryProvider(getMockQuery()));
		// some of the test repos are set up as system repos so we need to
		// query all repos, not just non-system repos
		// TODO consider evolving these tests to distinguish between system
		// and non-system
		IUViewQueryContext queryContext = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_BY_REPO);
		queryContext.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
		queryContext.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
		Policy.getDefault().setQueryContext(queryContext);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		Policy.getDefault().setQueryProvider(null);
		Policy.getDefault().setQueryContext(null);
	}

	protected Query getMockQuery() {
		return new Query() {
			public boolean isMatch(Object candidate) {
				return true;
			}
		};

	}
}
