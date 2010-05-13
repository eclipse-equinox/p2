/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PersistFragment extends AbstractProvisioningTest {

	public void testPersistFragmentIn35Repo() throws ProvisionException {
		//Pre 3.6, the host requirements were also persisted as part of the requirements 
		IInstallableUnitFragment fragment = createIUFragment(createEclipseIU("A"), "MyHost", Version.createOSGi(1, 0, 0));
		File repoFolder = getTempFolder();
		IMetadataRepository repo = createMetadataRepository(repoFolder.toURI(), null);
		Collection<IInstallableUnit> ius = new ArrayList<IInstallableUnit>();
		ius.add(fragment);
		repo.addInstallableUnits(ius);

		getMetadataRepositoryManager().removeRepository(repoFolder.toURI());

		IInstallableUnit iu = getMetadataRepositoryManager().loadRepository(repoFolder.toURI(), null).query(QueryUtil.createIUQuery("MyHost"), null).toArray(IInstallableUnit.class)[0];
		assertEquals(1, iu.getRequirements().size());

		assertEquals(fragment.getHost().iterator().next(), iu.getRequirements().iterator().next());
		assertNoContents(new File(repoFolder, "content.xml"), new String[] {"generation='2'"});
	}

	//Verify that in a 3.6 formatted IU, the host requirements are not persisted as part of the requirements 
	public void testPersistFragmentIn36Repo() throws ProvisionException {
		String orExpression = "providedCapabilities.exists(pc | pc.namespace == 'org.eclipse.equinox.p2.iu' && (pc.name == 'org.eclipse.mylyn34' || pc.name == 'org.eclipse.mylyn35'))";
		IExpression expr = ExpressionUtil.parse(orExpression);
		IMatchExpression matchExpression = ExpressionUtil.getFactory().matchExpression(expr);
		IRequirement orRequirement = MetadataFactory.createRequirement(matchExpression, null, 0, 1, true);

		IInstallableUnitFragment fragment = createIUFragment(createEclipseIU("A"), "MyHost", Version.createOSGi(1, 0, 0), new IRequirement[] {orRequirement}, null, null);
		File repoFolder = getTempFolder();
		IMetadataRepository repo = createMetadataRepository(repoFolder.toURI(), null);
		Collection<IInstallableUnit> ius = new ArrayList<IInstallableUnit>();
		ius.add(fragment);
		repo.addInstallableUnits(ius);

		getMetadataRepositoryManager().removeRepository(repoFolder.toURI());

		IInstallableUnit iu = getMetadataRepositoryManager().loadRepository(repoFolder.toURI(), null).query(QueryUtil.createIUQuery("MyHost"), null).toArray(IInstallableUnit.class)[0];
		assertEquals(1, iu.getRequirements().size());

		assertFalse(fragment.getHost().iterator().next().equals(iu.getRequirements().iterator().next()));
		assertContents(new File(repoFolder, "content.xml"), new String[] {"generation='2'"});
	}
}
