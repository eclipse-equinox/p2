/******************************************************************************* 
* Copyright (c) 2009, 2011 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM Corporation - Ongoing development
******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.analyzer;

import java.util.Collection;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.tools.analyzer.IUAnalyzer;

/**
 * This service checks that for each fragment the host can be resolved.
 * Currently this service only checks requirements with the namespace "osgi.bundle"
 */
public class HostCheckAnalyzer extends IUAnalyzer {

	private IMetadataRepository repository;

	public void analyzeIU(IInstallableUnit iu) {
		if (iu instanceof IInstallableUnitFragment) {
			IInstallableUnitFragment fragment = (IInstallableUnitFragment) iu;
			Collection<IRequirement> hosts = fragment.getHost();
			for (IRequirement req : hosts) {
				IMatchExpression<IInstallableUnit> hostMatch = req.getMatches();
				String namespace = RequiredCapability.extractNamespace(hostMatch);
				if ("osgi.bundle".equals(namespace)) { //$NON-NLS-1$
					String name = RequiredCapability.extractName(hostMatch);
					VersionRange range = RequiredCapability.extractRange(hostMatch);
					IQueryResult<IInstallableUnit> results = repository.query(QueryUtil.createIUQuery(name, range), new NullProgressMonitor());
					if (results.isEmpty()) {
						error(iu, "IU Fragment: " + iu.getId() + " cannot find host" + name + " : " + range);
						return;
					}
				}
			}
		}

	}

	public void preAnalysis(IMetadataRepository repository) {
		this.repository = repository;
	}

}
