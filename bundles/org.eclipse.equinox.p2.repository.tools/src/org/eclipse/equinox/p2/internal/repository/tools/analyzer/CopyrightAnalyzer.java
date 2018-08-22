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
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.tools.analyzer.IUAnalyzer;

/**
 * This service checks that all Group IUs have a copyright.
 */
public class CopyrightAnalyzer extends IUAnalyzer {

	@Override
	public void analyzeIU(IInstallableUnit iu) {
		if (Boolean.parseBoolean(iu.getProperty(InstallableUnitDescription.PROP_TYPE_GROUP))) {
			if (iu.getCopyright() == null || iu.getCopyright().getBody().length() == 0) {
				// If there is no copyright at all, this is an error
				error(iu, "[ERROR] " + iu.getId() + " has no copyright");
				return;
			}
			if (iu.getCopyright() != null && iu.getCopyright().getBody().startsWith("%")) {
				// If there is a copyright, but it starts with %, then check the default
				// language for a copyright
				String copyrightProperty = iu.getCopyright().getBody().substring(1);
				if (iu.getProperty("df_LT." + copyrightProperty) == null)
					error(iu, "[ERROR] " + iu.getId() + " has no copyright");
			}
		}
	}

	@Override
	public void preAnalysis(IMetadataRepository repository) {
		// do nothing
	}

}
