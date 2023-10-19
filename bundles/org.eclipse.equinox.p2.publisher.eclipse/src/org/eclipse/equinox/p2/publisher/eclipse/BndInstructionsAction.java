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

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.*;

/**
 * This publisher action can handle bnd instruction files and transform these
 * into InstallableUnits that can be used to provision a system that is used for
 * <b>building</b>, so this action will usually used in the context of Tycho,
 * Oomph or similar.
 */
public class BndInstructionsAction extends AbstractPublisherAction {

	private static final List<String> DEPENDENCY_INSTRUCTIONS = List.of(Constants.BUILDPATH, Constants.TESTPATH);

	private final File bndFile;

	public BndInstructionsAction(File bndFile) {
		this.bndFile = bndFile;
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		try (Processor processor = new Processor()) {
			processor.setProperties(bndFile);
			List<IRequirement> requirements = DEPENDENCY_INSTRUCTIONS.stream()
					.flatMap(instr -> processor.getMergedParameters(instr).stream().mapToObj((key, attr) -> {
						return MetadataFactory.createRequirement(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE, key,
								VersionRange.emptyRange, null, false, true);
					})).toList();
			if (!requirements.isEmpty()) {
				InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
				result.setId("bnd-bundle-requirements-" + UUID.randomUUID()); //$NON-NLS-1$
				result.setVersion(Version.createOSGi(0, 0, 0, String.valueOf(System.currentTimeMillis())));
				result.addRequirements(requirements);
				results.addIU(MetadataFactory.createInstallableUnit(result), IPublisherResult.NON_ROOT);
			}
		} catch (IOException e) {
			return Status.error(bndFile.getAbsolutePath() + " can not be processed", e); //$NON-NLS-1$
		}
		return Status.OK_STATUS;
	}

}
