package org.eclipse.equinox.p2.tests.sharedinstall;

/*******************************************************************************
 * Copyright (c) 2012 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pascal Rapicault (Ericsson) - Initial API and implementation
 *******************************************************************************/
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import org.eclipse.core.runtime.*;
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
	protected File getTestData(String message, String entry) {
		if (entry == null)
			fail(message + " entry is null.");
		URL base = Platform.getBundle("org.eclipse.equinox.p2.tests.sharedinstall").getEntry(entry);
		if (base == null)
			fail(message + " entry not found in bundle: " + entry);
		try {
			String osPath = new Path(FileLocator.toFileURL(base).getPath()).toOSString();
			File result = new File(osPath);
			if (!result.getCanonicalPath().equals(result.getPath()))
				fail(message + " result path: " + result.getPath() + " does not match canonical path: " + result.getCanonicalFile().getPath());
			return result;
		} catch (IOException e) {
			fail(message, e);
		}
		// avoid compile error... should never reach this code
		return null;
	}

	protected void setUp() throws Exception {
		//We don't call super.setUp() on purpose

		Bundle p2Core = Platform.getBundle("org.eclipse.equinox.p2.core");
		p2Core.stop();

		File baseInstall = getTestData("test shared install", "data/test1/base");
		File userHome = getTestData("test shared install", "data/test1/user");
		System.setProperty("osgi.sharedConfiguration.area", new File(baseInstall, "configuration").toURI().toString());
		System.setProperty("osgi.configuration.area", new File(userHome, "configuration").toURI().toString());
		System.setProperty("eclipse.p2.profile", "epp.package.java");
		System.setProperty("eclipse.p2.data.area", "@config.dir//../p2");
		IPreferencesService prefService = (IPreferencesService) ServiceHelper.getService(Activator.getContext(), IPreferencesService.class.getName());
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
