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
import java.net.URISyntaxException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class SharedProfilePreferencesTest extends AbstractProvisioningTest {
	protected void setUp() throws Exception {
		//We don't call super.setUp() on purpose

		Bundle p2Core = Platform.getBundle("org.eclipse.equinox.p2.core");
		p2Core.stop();

		//We have to do all this dance to copy the files because if we store them at the actual path, then the path is too long 
		File baseInstallToCopy = getTestData("baseInstall", "testData/sharedPrefs/test1/baseInstall");
		File baseInstall = getTempFolder();
		File baseInstallToCopyTo = new File(baseInstall, "p2/org.eclipse.equinox.p2.engine/profileRegistry");
		baseInstallToCopy.mkdirs();
		copy("copy base install", baseInstallToCopy, baseInstallToCopyTo);

		File userHomeToCopy = getTestData("useHome", "testData/sharedPrefs/test1/userHome");
		File userHome = getTempFolder();
		File userHomeToCopyTo = new File(userHome, "p2/org.eclipse.equinox.p2.engine/");
		userHomeToCopyTo.mkdirs();
		copy("copy user home data", userHomeToCopy, userHomeToCopyTo);

		System.setProperty("osgi.sharedConfiguration.area", new File(baseInstall, "configuration").toURI().toString());
		System.setProperty("osgi.configuration.area", new File(userHome, "configuration").toURI().toString() + '/');
		System.setProperty("eclipse.p2.profile", "epp.package.java");
		System.setProperty("eclipse.p2.data.area", "@config.dir/../p2");
		IPreferencesService prefService = ServiceHelper.getService(TestActivator.getContext(), IPreferencesService.class);
		prefService.getRootNode().node("/profile/").removeNode();
		p2Core.start();
	}

	public void testCountReposInSharedInstallPreferences() {
		IPreferencesService prefService = ServiceHelper.getService(TestActivator.getContext(), IPreferencesService.class);
		assertNotNull(prefService);
		try {
			URI defaultLocation = adjustTrailingSlash(URIUtil.makeAbsolute(URIUtil.fromString(TestActivator.getContext().getProperty("osgi.configuration.area") + "/../p2/"), new URI(".")), true);
			String locationString = EncodingUtils.encodeSlashes(defaultLocation.toString());
			Preferences node = prefService.getRootNode().node("/profile/shared/" + locationString + "/_SELF_/org.eclipse.equinox.p2.metadata.repository/repositories"); //$NON-NLS-1$
			String[] children = node.childrenNames();
			assertEquals(3, children.length);
		} catch (IllegalArgumentException e) {
			fail("Exception", e);
		} catch (URISyntaxException e) {
			fail("Exception", e);
		} catch (BackingStoreException e) {
			fail("Exception", e);
		}

	}

	private static URI adjustTrailingSlash(URI url, boolean trailingSlash) throws URISyntaxException {
		String file = url.toString();
		if (trailingSlash == (file.endsWith("/"))) //$NON-NLS-1$
			return url;
		file = trailingSlash ? file + "/" : file.substring(0, file.length() - 1); //$NON-NLS-1$
		return new URI(file);
	}

	public void testCountRepoInSharedInstallThroughRepoManagerAPI() {
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] repos = repoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		assertEquals(3, repos.length);
	}

}
