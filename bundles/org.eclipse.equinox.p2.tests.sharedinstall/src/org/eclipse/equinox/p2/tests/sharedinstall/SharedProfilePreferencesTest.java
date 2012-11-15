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
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class SharedProfilePreferencesTest extends AbstractProvisioningTest {
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
		
		File baseInstall = getTestData("test shared install", "testData/sharedPreferences/baseInstall");
		File userHome = getTestData("test shared install", "testData/sharedPreferences/userHome");
		System.setProperty("osgi.sharedConfiguration.area", new File(baseInstall, "configuration").toURI().toString());
		System.setProperty("osgi.configuration.area", new File(userHome, "configuration").toURI().toString());
		System.setProperty("eclipse.p2.profile", "epp.package.java");
		System.setProperty("eclipse.p2.data.area", "@config.dir/../p2");
		
		p2Core.start();
	}
	
	public void testCountReposInSharedInstallPreferences(){
		IPreferencesService prefService = (IPreferencesService) ServiceHelper.getService(TestActivator.getContext(), IPreferencesService.class.getName());
		assertNotNull(prefService);
		try {
			URI defaultLocation = URIUtil.makeAbsolute(URIUtil.fromString(TestActivator.getContext().getProperty("osgi.configuration.area") + "../p2/"), new URI("."));
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

	public void testCountRepoInSharedInstallThroughRepoManagerAPI() {
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] repos = repoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		assertEquals(3, repos.length);
	}
	
	
}
