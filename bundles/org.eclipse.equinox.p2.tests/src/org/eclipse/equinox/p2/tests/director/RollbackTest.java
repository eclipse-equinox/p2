/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class RollbackTest extends AbstractProvisioningTest {

	private IInstallableUnit a1;
	private Profile profile;
	private IDirector director;

	protected void setUp() throws Exception {
		a1 = createIU("A", DEFAULT_VERSION, true);
		profile = createProfile("TestProfile." + getName());
		director = createDirector();
	}

	public void test() {
		System.out.println(director.install(new IInstallableUnit[] {a1}, profile, new NullProgressMonitor()));
		printProfile(profile);
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		IMetadataRepository repo = null;
		repo = repoMan.loadRepository(getRollbackRepository(), null);
		IInstallableUnit[] ius = repo.getInstallableUnits(null);
		for (int i = 0; i < ius.length; i++)
			System.out.println(ius[i]);
		director.become(ius[0], profile, new NullProgressMonitor());
		printProfile(profile);
	}

	private URL getRollbackRepository() {
		try {
			URL location = ((AgentLocation) ServiceHelper.getService(DirectorActivator.context, AgentLocation.class.getName())).getDataArea(DirectorActivator.PI_DIRECTOR);
			return new URL(location, "rollback");
		} catch (MalformedURLException e) {
			fail("4.99", e);
			return null;
		}
	}
}
