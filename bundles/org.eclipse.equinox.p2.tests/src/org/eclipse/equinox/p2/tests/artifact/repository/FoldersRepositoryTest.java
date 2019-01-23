/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.Iterator;
import junit.framework.TestCase;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class FoldersRepositoryTest extends TestCase {

	private IArtifactRepositoryManager manager;
	private File testRepo;

	public FoldersRepositoryTest(String name) {
		super(name);
	}

	public FoldersRepositoryTest() {
		super("");
	}

	@Override
	protected void setUp() throws Exception {
		IProvisioningAgent agent = ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgent.class);
		manager = agent.getService(IArtifactRepositoryManager.class);
	}

	@Override
	protected void tearDown() throws Exception {
		manager = null;
		if (testRepo != null)
			AbstractProvisioningTest.delete(testRepo);
	}

	public void testFolderRepository() throws Exception {
		String tempDir = System.getProperty("java.io.tmpdir");
		testRepo = new File(tempDir, "testRepo");
		AbstractProvisioningTest.delete(testRepo);
		testRepo.mkdir();

		manager.removeRepository(testRepo.toURI());
		IArtifactRepository repo = manager.createRepository(testRepo.toURI(), "testRepo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		File pluginsFolder = new File(testRepo, "plugins");
		pluginsFolder.mkdir();

		URL sourceBase = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File sourceFolder = new File(FileLocator.toFileURL(sourceBase).getPath());

		AbstractProvisioningTest.copy("0.99", sourceFolder, pluginsFolder);
		FileFilter filter = pathname -> !pathname.getName().equals("CVS");
		File[] fileList = pluginsFolder.listFiles(filter);
		assertEquals(2, fileList.length);
		IProgressMonitor monitor = new NullProgressMonitor();
		for (int i = 0; i < fileList.length; i++) {
			File file = fileList[i];
			String fileName = file.getName();
			if (fileName.endsWith(".jar"))
				fileName = fileName.substring(0, fileName.length() - 4);
			String identifier = fileName.substring(0, fileName.indexOf('_'));
			String version = fileName.substring(fileName.indexOf('_') + 1);

			ArtifactKey key = new ArtifactKey("osgi.bundle", identifier, Version.create(version));
			ArtifactDescriptor descriptor = new ArtifactDescriptor(key);
			if (file.isDirectory())
				descriptor.setProperty("artifact.folder", "true");

			repo.addDescriptor(descriptor, monitor);
		}
		IQueryResult<IArtifactKey> keys = repo.query(ArtifactKeyQuery.ALL_KEYS, null);
		assertEquals(2, AbstractProvisioningTest.queryResultSize(keys));
		for (Iterator<IArtifactKey> iterator = keys.iterator(); iterator.hasNext();) {
			IArtifactKey key = iterator.next();
			repo.removeDescriptor(key, monitor);
		}

		keys = repo.query(ArtifactKeyQuery.ALL_KEYS, null);
		assertTrue(keys.isEmpty());
		assertEquals(0, pluginsFolder.listFiles(filter).length);

		manager.removeRepository(repo.getLocation());
		AbstractProvisioningTest.delete(testRepo);
	}
}
