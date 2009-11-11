/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

public class FoldersRepositoryTest extends TestCase {

	private ServiceReference managerRef;
	private IArtifactRepositoryManager manager;
	private File testRepo;

	public FoldersRepositoryTest(String name) {
		super(name);
	}

	public FoldersRepositoryTest() {
		super("");
	}

	protected void setUp() throws Exception {
		managerRef = TestActivator.getContext().getServiceReference(IArtifactRepositoryManager.SERVICE_NAME);
		manager = (IArtifactRepositoryManager) TestActivator.getContext().getService(managerRef);
	}

	protected void tearDown() throws Exception {
		manager = null;
		TestActivator.getContext().ungetService(managerRef);
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
		FileFilter filter = new FileFilter() {

			public boolean accept(File pathname) {
				return !pathname.getName().equals("CVS");
			}
		};
		File[] fileList = pluginsFolder.listFiles(filter);
		assertEquals(2, fileList.length);
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

			repo.addDescriptor(descriptor);
		}
		IArtifactKey[] keys = repo.getArtifactKeys();
		assertEquals(2, keys.length);
		for (int i = 0; i < keys.length; i++) {
			repo.removeDescriptor(keys[i]);
		}
		assertEquals(0, repo.getArtifactKeys().length);
		assertEquals(0, pluginsFolder.listFiles(filter).length);

		manager.removeRepository(repo.getLocation());
		AbstractProvisioningTest.delete(testRepo);
	}
}
