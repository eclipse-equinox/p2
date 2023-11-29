/******************************************************************************* 
* Copyright (c) 2009, 2017 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.analyzer;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.tools.analyzer.IUAnalyzer;

/**
 * This service checks that each IU has a proper version number
 *  1. No 0.0.0
 *  2. No x.y.z.qualifier (each qualifier has been replaced)
 */
@SuppressWarnings("nls")
public class VersionAnalyzer extends IUAnalyzer {

	@Override
	public void analyzeIU(IInstallableUnit iu) {
		if (iu.getVersion().equals(Version.emptyVersion)) {
			error(iu, "[ERROR] IU: " + iu.getId() + " has not replaced its qualifiier");
			return;
		}
		if (iu.getVersion().isOSGiCompatible()) {
			String qualifier = toOSGiVersion(iu.getVersion()).getQualifier();
			if (qualifier != null && qualifier.equals("qualifier")) {
				error(iu, "[ERROR] IU: " + iu.getId() + " has not replaced its qualifiier");
				return;
			}
		}
	}

	private static org.osgi.framework.Version toOSGiVersion(Version version) {
		if (version == null)
			return null;
		if (version == Version.emptyVersion)
			return org.osgi.framework.Version.emptyVersion;
		if (version == Version.MAX_VERSION)
			return new org.osgi.framework.Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

		return new org.osgi.framework.Version(version.toString());
	}

	@Override
	public void preAnalysis(IMetadataRepository repo) {
		// Do nothing
	}

}
