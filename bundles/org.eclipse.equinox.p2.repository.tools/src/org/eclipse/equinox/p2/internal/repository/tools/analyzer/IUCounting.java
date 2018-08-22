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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.tools.analyzer.IIUAnalyzer;

/**
 * This service just counts the total number of IUs
 */
public class IUCounting implements IIUAnalyzer {

	int totalIUs = 0;
	int totalGroups = 0;
	int totalFragments = 0;
	int totalCategories = 0;

	private boolean hasProperty(IInstallableUnit iu, String property) {
		return Boolean.parseBoolean(iu.getProperty(property));
	}

	@Override
	public void analyzeIU(IInstallableUnit iu) {
		totalIUs++;
		if (hasProperty(iu, InstallableUnitDescription.PROP_TYPE_FRAGMENT))
			totalFragments++;
		if (hasProperty(iu, InstallableUnitDescription.PROP_TYPE_GROUP))
			totalGroups++;
		if (hasProperty(iu, InstallableUnitDescription.PROP_TYPE_CATEGORY))
			totalCategories++;
	}

	@Override
	public IStatus postAnalysis() {
		System.out.println("Total IUs: " + totalIUs);
		System.out.println("  Total Groups: " + totalGroups);
		System.out.println("  Total Fragments: " + totalFragments);
		System.out.println("  Total Categories: " + totalCategories);
		return null;
	}

	@Override
	public void preAnalysis(IMetadataRepository repo) {
		totalIUs = 0;
		totalGroups = 0;
		totalFragments = 0;
		totalCategories = 0;
	}

}
