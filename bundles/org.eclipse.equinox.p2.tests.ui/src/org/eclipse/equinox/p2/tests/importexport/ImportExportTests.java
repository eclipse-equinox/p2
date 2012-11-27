/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - Bug 387115 - Allow to export everything
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.importexport;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.p2.importexport.*;
import org.eclipse.equinox.internal.p2.importexport.internal.ImportExportImpl;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.util.tracker.ServiceTracker;

public class ImportExportTests extends AbstractProvisioningTest {

	private P2ImportExport importexportService;

	private List<IStatus> getChildren(IStatus s) {
		List<IStatus> rt = new ArrayList<IStatus>();
		if (s.isMultiStatus()) {
			for (IStatus child : s.getChildren()) {
				rt.addAll(getChildren(child));
			}
		}
		rt.add(s);
		return rt;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ServiceTracker<P2ImportExport, P2ImportExport> tracker = new ServiceTracker<P2ImportExport, P2ImportExport>(TestActivator.getContext(), P2ImportExport.class, null);
		tracker.open();
		importexportService = tracker.getService();
		assertNotNull("Fail to get ImportExport service", importexportService);
		tracker.close();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		importexportService = null;
	}

	public void testLoadP2f() throws IOException {
		File p2fFile = getTestData("Error load test file.", "testData/importexport/test.p2f");
		InputStream input = new FileInputStream(p2fFile);
		try {
			List<IUDetail> iuDetails = importexportService.importP2F(input);
			assertTrue("Should load two features from the p2f file.", iuDetails.size() == 2);
			int counter = 0;
			for (IUDetail iu : iuDetails) {
				if ("org.polarion.eclipse.team.svn.connector.feature.group".equals(iu.getIU().getId())) {
					counter++;
					assertTrue("Should have two referred repository.", iu.getReferencedRepositories().size() == 2);
				} else if ("org.polarion.eclipse.team.svn.connector.svnkit16.feature.group".equals(iu.getIU().getId())) {
					counter++;
					assertTrue("Should have one referred repository", iu.getReferencedRepositories().size() == 1);
				}
			}
			assertEquals("Load unexpected content.", 2, counter);
		} finally {
			input.close();
		}
	}

	public void testLoadUnknownP2f() throws IOException {
		File p2fFile = getTestData("Error load test file.", "testData/importexport/unknownformat.p2f");
		InputStream input = new FileInputStream(p2fFile);
		try {
			List<IUDetail> iuDetails = importexportService.importP2F(input);
			assertEquals("Should not load any detail.", 0, iuDetails.size());
		} finally {
			input.close();
		}
	}

	public void testIncompatibleP2f() throws IOException {
		File p2fFile = getTestData("Error load test file.", "testData/importexport/incompatible.p2f");
		InputStream input = new FileInputStream(p2fFile);
		try {
			importexportService.importP2F(input);
			assertTrue("Didn't complain the given file is not supported by current version.", false);
		} catch (VersionIncompatibleException e) {
			// expected
		} finally {
			input.close();
		}
	}

	public void testExportFeaturesInstalledFromLocal() throws ProvisionException, OperationCanceledException, IOException {
		File testFile = File.createTempFile("test", "p2f");
		try {
			IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
			File localRepoFile = getTestData("Error load data", "testData/importexport/repo1");
			IMetadataRepository repo = metaManager.loadRepository(localRepoFile.toURI(), null);
			assertNotNull("Fail to load local repo", repo);
			IInstallableUnit iu = createIU("A", Version.create("1.0.0"));
			OutputStream output = new FileOutputStream(testFile);
			IStatus status = importexportService.exportP2F(output, new IInstallableUnit[] {iu}, false, null);
			assertFalse("Not expected return result.", status.isOK());
			assertTrue("Should be a multiple status", status.isMultiStatus());
			boolean hasFeaturesIgnored = false;
			for (IStatus s : getChildren(status))
				if (s.getCode() == ImportExportImpl.IGNORE_LOCAL_REPOSITORY)
					hasFeaturesIgnored = true;
			assertTrue("Should have features ignored due to they're installed from local repository.", hasFeaturesIgnored);
			output.close();
		} finally {
			testFile.delete();
		}
	}

	public void testAllowExportFeaturesInstalledFromLocal() throws ProvisionException, OperationCanceledException, IOException {
		File testFile = File.createTempFile("test", "p2f");
		try {
			IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
			File localRepoFile = getTestData("Error load data", "testData/importexport/repo1");
			IMetadataRepository repo = metaManager.loadRepository(localRepoFile.toURI(), null);
			assertNotNull("Fail to load local repo", repo);
			IInstallableUnit iu = createIU("A", Version.create("1.0.0"));
			OutputStream output = new FileOutputStream(testFile);
			IStatus status = importexportService.exportP2F(output, new IInstallableUnit[] {iu}, true, null);
			assertTrue(status.isOK());
			output.close();
		} finally {
			testFile.delete();
		}
	}
}
