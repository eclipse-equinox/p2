/*******************************************************************************
 *  Copyright (c) 2010, 2017 Sonatype, Inc and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PersistNegation extends AbstractProvisioningTest {

	public void testPersistNegation() throws ProvisionException, OperationCanceledException {
		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("NegateRWT");
		iud1.setVersion(Version.create("1.0.0"));

		IRequirement req1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.rap.rwt", new VersionRange("[1.0.0, 2.0.0)"), null, 0, 0, false, null);
		Collection<IRequirement> requirements = new ArrayList<>();
		requirements.add(req1);
		iud1.addRequirements(requirements);
		Collection<IProvidedCapability> capabilities = new ArrayList<>();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "NegateRWT", Version.create("1.0.0")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);
		File tmpFolder = getTempFolder();
		IMetadataRepository repo = getMetadataRepositoryManager().createRepository(tmpFolder.toURI(), "NegationRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		ArrayList<IInstallableUnit> iu = new ArrayList<>();
		iu.add(iu1);
		repo.addInstallableUnits(iu);

		assertContents(new File(tmpFolder, "content.xml"), new String[] {"max='0'"});
		assertContents(new File(tmpFolder, "content.xml"), new String[] {"generation='2'"});

	}
}
