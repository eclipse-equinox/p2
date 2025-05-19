/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.discovery.tests.core.mock;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.Policy;
import org.eclipse.equinox.internal.p2.discovery.compatibility.BundleDiscoveryStrategy;

/**
 * a discovery strategy for bundles where the policy can be arbitrarily set
 *
 * @author David Green
 */
public class MockBundleDiscoveryStrategy extends BundleDiscoveryStrategy {
	private Policy policy = Policy.defaultPolicy();

	@Override
	protected AbstractCatalogSource computeDiscoverySource(IContributor contributor) {
		AbstractCatalogSource discoverySource = super.computeDiscoverySource(contributor);
		discoverySource.setPolicy(policy);
		return discoverySource;
	}

	public Policy getPolicy() {
		return policy;
	}

	public void setPolicy(Policy policy) {
		this.policy = policy;
	}
}
