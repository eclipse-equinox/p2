/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.*;

/**
 * This publisher action can handle build.properties files and transform these
 * into InstallableUnits that can be used to provision a system that is used for
 * <b>building</b>, so this action will usually used in the context of Tycho,
 * Oomph or similar.
 */
public class BuildPropertiesAction extends AbstractPublisherAction {

	private static final String KEY_ADDITIONAL_BUNDLES = "additional.bundles"; //$NON-NLS-1$
	private static final String SEPERATOR = ","; //$NON-NLS-1$

	private final File buildPropertiesFile;

	public BuildPropertiesAction(File buildPropertiesFile) {
		this.buildPropertiesFile = buildPropertiesFile;
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		if (buildPropertiesFile.exists()) {
			Properties properties = new Properties();
			try (FileInputStream stream = new FileInputStream(buildPropertiesFile)) {
				properties.load(stream);
			} catch (IOException e) {
				return Status.error("Can't read build.properties file: " + buildPropertiesFile.getAbsolutePath(), e); //$NON-NLS-1$
			}
			String property = properties.getProperty(KEY_ADDITIONAL_BUNDLES);
			if (property != null) {
				List<IRequirement> requirements = Arrays.stream(property.split(SEPERATOR)).map(String::strip)
						.filter(Predicate.not(String::isBlank))
						.map(bsn -> MetadataFactory.createRequirement(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE, bsn,
								VersionRange.emptyRange, null, true, true))
						.toList();
				if (!requirements.isEmpty()) {
					InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
					result.setId("additional-bundle-requirements-" + UUID.randomUUID()); //$NON-NLS-1$
					result.setVersion(Version.createOSGi(0, 0, 0, String.valueOf(System.currentTimeMillis())));
					result.addRequirements(requirements);
					results.addIU(MetadataFactory.createInstallableUnit(result), IPublisherResult.NON_ROOT);
				}
			}
		}
		return Status.OK_STATUS;
	}

}
