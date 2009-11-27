/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.analyzer;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
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
			IRequiredCapability[] hosts = fragment.getHost();
			for (int i = 0; i < hosts.length; i++) {
				if (hosts[i].getNamespace().equals("osgi.bundle")) {
					Collector results = repository.query(new InstallableUnitQuery(hosts[i].getName(), hosts[i].getRange()), new Collector(), new NullProgressMonitor());
					if (results.size() == 0) {
						error(iu, "IU Fragment: " + iu.getId() + " cannot find host" + hosts[i].getName() + " : " + hosts[i].getRange());
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
