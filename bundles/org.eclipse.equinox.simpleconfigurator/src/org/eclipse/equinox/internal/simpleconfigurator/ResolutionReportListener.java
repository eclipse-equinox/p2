/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.*;

class ResolutionReportListener implements ResolverHookFactory, ResolverHook, ResolutionReport.Listener {

	private List<ResolutionReport> reports = new CopyOnWriteArrayList<>();

	@Override
	public ResolverHook begin(Collection<BundleRevision> triggers) {
		return this;
	}

	@Override
	public void handleResolutionReport(ResolutionReport report) {
		reports.add(report);
	}

	@Override
	public void filterResolvable(Collection<BundleRevision> candidates) {
		// nothing to do...
	}

	@Override
	public void filterSingletonCollisions(BundleCapability singleton,
			Collection<BundleCapability> collisionCandidates) {
		// nothing to do...
	}

	@Override
	public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
		// nothing to do...
	}

	@Override
	public void end() {
		// nothing to do...
	}

	public List<ResolutionReport> getReports() {
		return List.copyOf(reports);
	}
}