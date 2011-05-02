/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
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

	public void analyzeIU(IInstallableUnit iu) {
		// Create a unique name / version pair and cache it
		String uniqueID = iu.getId() + ":" + iu.getVersion().toString();
		if (versionedNames.contains(uniqueID)) {
			error(iu, "[ERROR]" + iu.getId() + " with version: " + iu.getVersion() + " already exists in the repository");
			return;
		}
		versionedNames.add(uniqueID);
	}

	public void preAnalysis(IMetadataRepository repo) {
		versionedNames = new HashSet<String>();
	}
}
