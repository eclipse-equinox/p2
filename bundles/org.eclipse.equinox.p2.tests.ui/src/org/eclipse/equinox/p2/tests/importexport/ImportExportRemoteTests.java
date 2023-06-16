/*******************************************************************************
 * Copyright (c) 2011, 2017 WindRiver Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.importexport;

import java.io.*;
import java.net.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.importexport.IUDetail;
import org.eclipse.equinox.internal.p2.importexport.P2ImportExport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.metadata.repository.ServerBasedTestCase;
import org.junit.Ignore;
import org.osgi.util.tracker.ServiceTracker;

@Ignore
public class ImportExportRemoteTests extends ServerBasedTestCase {

	private P2ImportExport importexportService;

	private URI getTestRepository() throws URISyntaxException {
		return new URI(getBaseURL() + "/importexport/");
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		ServiceTracker<P2ImportExport, P2ImportExport> tracker = new ServiceTracker<>(TestActivator.getContext(),
				P2ImportExport.class, null);
		tracker.open();
		importexportService = tracker.getService();
		assertNotNull("Fail to get ImportExport service", importexportService);
		tracker.close();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		importexportService = null;
		IMetadataRepositoryManager repoMan = getAgent().getService(IMetadataRepositoryManager.class);
		URI[] urls = repoMan.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (URI url : urls) {
			repoMan.removeRepository(url);
		}
	}

	public void testExportFeaturesFromRemoteRepository()
			throws URISyntaxException, IOException, ProvisionException, OperationCanceledException {
		File testFile = File.createTempFile("test", "p2f");
		URI uri = getTestRepository();
		try {
			IMetadataRepositoryManager metaManager = getAgent().getService(IMetadataRepositoryManager.class);
			IMetadataRepository repo = metaManager.loadRepository(uri, null);
			assertNotNull("Fail to load remote repo", repo);
			IInstallableUnit iu = AbstractProvisioningTest.createIU("A", Version.create("1.0.0"));
			try (OutputStream output = new FileOutputStream(testFile)) {
				IStatus status = importexportService.exportP2F(output, new IInstallableUnit[] { iu }, false, null);
				assertTrue("Not expected return result.", status.isOK());
			}
			try (InputStream input = new FileInputStream(testFile)) {
				List<IUDetail> ius = importexportService.importP2F(input);
				assertEquals("Exported the number of features is not expected.", 1, ius.size());
				assertEquals("Exported feature is not expected.", iu, ius.get(0).getIU());
				assertEquals("Exported the number of referred repositories is not expected.", 1,
						ius.get(0).getReferencedRepositories().size());
				assertEquals("Exported referred repository is not expected.", uri,
						ius.get(0).getReferencedRepositories().get(0));
			}
		} finally {
			testFile.delete();
		}
	}

	protected File getTestData(String message, String entry) {
		if (entry == null)
			fail(message + " entry is null.");
		URL base = TestActivator.getContext().getBundle().getEntry(entry);
		if (base == null)
			fail(message + " entry not found in bundle: " + entry);
		try {
			String osPath = IPath.fromOSString(FileLocator.toFileURL(base).getPath()).toOSString();
			File result = new File(osPath);
			if (!result.getCanonicalPath().equals(result.getPath()))
				fail(message + " result path: " + result.getPath() + " does not match canonical path: "
						+ result.getCanonicalFile().getPath());
			return result;
		} catch (IOException e) {
			fail(message);
		}
		// avoid compile error... should never reach this code
		return null;
	}

	public void testExportFeaturesFromBothRemoteRepositoryAndLocalRepository()
			throws URISyntaxException, IOException, ProvisionException, OperationCanceledException {
		File testFile = File.createTempFile("test", "p2f");
		URI uri = getTestRepository();
		try {
			IMetadataRepositoryManager metaManager = getAgent().getService(IMetadataRepositoryManager.class);
			File localRepoFile = getTestData("Error load data", "testData/importexport/repo1");
			IMetadataRepository localRepo = metaManager.loadRepository(localRepoFile.toURI(), null);
			assertNotNull("Fail to load local repo", localRepo);
			IMetadataRepository repo = metaManager.loadRepository(uri, null);
			assertNotNull("Fail to load remote repo", repo);
			IInstallableUnit iu = AbstractProvisioningTest.createIU("A", Version.create("1.0.0"));
			try (OutputStream output = new FileOutputStream(testFile)) {
				IStatus status = importexportService.exportP2F(output, new IInstallableUnit[] { iu }, false, null);
				assertTrue("Not expected return result.", status.isOK());
			}
			try (InputStream input = new FileInputStream(testFile)) {
				List<IUDetail> ius = importexportService.importP2F(input);
				assertEquals("Exported the number of features is not expected.", 1, ius.size());
				assertEquals("Exported feature is not expected.", iu, ius.get(0).getIU());
				assertEquals("Exported the number of referred repositories is not expected.", 1,
						ius.get(0).getReferencedRepositories().size());
				assertEquals("Exported referred repository is not expected.", uri,
						ius.get(0).getReferencedRepositories().get(0));
			}
		} finally {
			testFile.delete();
		}
	}
}
