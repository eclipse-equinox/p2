/*******************************************************************************
 * Copyright (c) 2012, 2015 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pascal Rapicault (Ericsson) - Initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import java.io.File;
import java.net.URI;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Bundle;

public class SharedProfilePreferencesTestWithDataInUser extends AbstractProvisioningTest {
	protected void setUp() throws Exception {
		//We don't call super.setUp() on purpose

		Bundle p2Core = Platform.getBundle("org.eclipse.equinox.p2.core");
		p2Core.stop();

		//We have to do all this dance to copy the files because if we store them at the actual path, then the path is too long
		File baseInstallToCopy = getTestData("baseInstall", "testData/sharedPrefs/test2/baseInstall");
		File baseInstall = getTempFolder();
		File baseInstallToCopyTo = new File(baseInstall, "p2/org.eclipse.equinox.p2.engine/profileRegistry");
		baseInstallToCopy.mkdirs();
		copy("copy base install", baseInstallToCopy, baseInstallToCopyTo);

		File userHomeToCopy = getTestData("useHome", "testData/sharedPrefs/test2/userHome");
		File userHome = getTempFolder();
		File userHomeToCopyTo = new File(userHome, "p2/org.eclipse.equinox.p2.engine/profileRegistry");
		userHomeToCopyTo.mkdirs();
		copy("copy user home data", userHomeToCopy, userHomeToCopyTo);

		System.setProperty("osgi.sharedConfiguration.area", new File(baseInstall, "configuration").toURI().toString());
		System.setProperty("osgi.configuration.area", new File(userHome, "configuration").toURI().toString() + '/');
		System.setProperty("eclipse.p2.profile", "epp.package.java");
		System.setProperty("eclipse.p2.data.area", "@config.dir/../p2");
		IPreferencesService prefService = ServiceHelper.getService(Activator.getContext(), IPreferencesService.class);
		prefService.getRootNode().node("/profile/").removeNode();
		p2Core.start();

		//Make sure that things are properly setup
		IProvisioningAgent currentAgent = getAgent();
		assertEquals(currentAgent, ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgent.class));
		currentAgent.getService(IProvisioningAgent.SHARED_BASE_AGENT);
		currentAgent.getService(IProvisioningAgent.SHARED_CURRENT_AGENT);
	}

	public void testCountRepoInSharedInstallThroughRepoManagerAPI() {
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] repos = repoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		assertEquals(4, repos.length);
	}

}
