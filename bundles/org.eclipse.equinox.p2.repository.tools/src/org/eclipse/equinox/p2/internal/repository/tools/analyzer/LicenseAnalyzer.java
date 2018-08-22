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

import java.util.Collection;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.tools.analyzer.IUAnalyzer;

/**
 * This service checks that each IU has a license.
 */
public class LicenseAnalyzer extends IUAnalyzer {

	@Override
	public void analyzeIU(IInstallableUnit iu) {
		if (Boolean.parseBoolean(iu.getProperty(InstallableUnitDescription.PROP_TYPE_GROUP))) {
			Collection<ILicense> licenses = iu.getLicenses();
			if (iu.getLicenses() == null || licenses.size() == 0) {
				// If there is no license then this is an error
				error(iu, "[ERROR] " + iu.getId() + " has no license");
				return;
			} else if (licenses.iterator().next().getBody().length() == 0) {
				error(iu, "[ERROR] " + iu.getId() + " has no license");
				return;
			}
			for (ILicense license : licenses) {
				if (license.getBody().startsWith("%")) {
					String licenseProperty = license.getBody().substring(1);
					if (iu.getProperty("df_LT." + licenseProperty) == null) {
						error(iu, "[ERROR] " + iu.getId() + " has no license");
					}
				}
			}
		}
	}

	@Override
	public void preAnalysis(IMetadataRepository repository) {
		// Do nothing
	}

}
