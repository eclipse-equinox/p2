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

import java.util.HashSet;
import java.util.Set;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.tools.analyzer.IUAnalyzer;

/**
 * This service checks that each IU is unique in a given repository.
 */
public class UniqueIUAnalyzer extends IUAnalyzer {

	Set<String> versionedNames = null;

	@Override
	public void analyzeIU(IInstallableUnit iu) {
		// Create a unique name / version pair and cache it
		String uniqueID = iu.getId() + ":" + iu.getVersion().toString();
		if (versionedNames.contains(uniqueID)) {
			error(iu, "[ERROR]" + iu.getId() + " with version: " + iu.getVersion() + " already exists in the repository");
			return;
		}
		versionedNames.add(uniqueID);
	}

	@Override
	public void preAnalysis(IMetadataRepository repo) {
		versionedNames = new HashSet<>();
	}
}
