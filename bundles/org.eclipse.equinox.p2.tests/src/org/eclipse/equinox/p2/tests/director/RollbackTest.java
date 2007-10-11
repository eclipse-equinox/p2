/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.net.URL;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Version;

public class RollbackTest extends AbstractProvisioningTest {

	private InstallableUnit a1;
	private Profile profile;
	private IDirector director;

	protected void setUp() throws Exception {
		a1 = new InstallableUnit();
		a1.setId("A");
		a1.setVersion(new Version(1, 0, 0));
		a1.setSingleton(true);

		profile = new Profile("TestProfile." + getName());
		director = createDirector();
	}

	public void test() {
		System.out.println(director.install(new IInstallableUnit[] {a1}, profile, new NullProgressMonitor()));
		printProfile(profile);
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		IMetadataRepository repo = null;
		URL location = ((AgentLocation) ServiceHelper.getService(DirectorActivator.context, AgentLocation.class.getName())).getTouchpointDataArea("director");
		repo = repoMan.getRepository(location);
		IInstallableUnit[] ius = repo.getInstallableUnits(null);
		for (int i = 0; i < ius.length; i++)
			System.out.println(ius[i]);
		director.become(ius[0], profile, new NullProgressMonitor());
		printProfile(profile);
	}
}
