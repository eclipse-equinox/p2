/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.directorywatcher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class RepositoryListenerTest extends AbstractProvisioningTest {

	public RepositoryListenerTest(String name) {
		super(name);
	}

	private void removeContents(File source, File target) throws Exception {
		if (source.exists() && source.isDirectory() && target.exists() && target.isDirectory()) {
			File[] files = source.listFiles();
			for (int i = 0; i < files.length; i++)
				delete(new File(target, files[i].getName()));
		}
	}

	private IInstallableUnit[] getInstallableUnits(RepositoryListener listener) {
		return (IInstallableUnit[]) listener.getMetadataRepository().query(InstallableUnitQuery.ANY, new Collector(), null).toArray(IInstallableUnit.class);
	}

	public void testDirectoryWatcherListener() {
		URL base = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher1");
		File baseFolder = null;
		try {
			baseFolder = new File(FileLocator.toFileURL(base).getPath());
		} catch (IOException e) {
			fail("0.99", e);
		}

		URL base2 = TestActivator.getContext().getBundle().getEntry("/testData/directorywatcher2");
		File baseFolder2 = null;
		try {
			baseFolder2 = new File(FileLocator.toFileURL(base2).getPath());
		} catch (IOException e) {
			fail("0.100", e);
		}

		String tempDir = System.getProperty("java.io.tmpdir");
		File folder = new File(tempDir, "testWatcher");
		delete(folder);
		folder.mkdir();

		Hashtable props = new Hashtable();
		props.put(DirectoryWatcher.DIR, folder.getAbsolutePath());
		props.put(DirectoryWatcher.POLL, "500");

		RepositoryListener listener = new RepositoryListener(TestActivator.getContext(), "test" + folder.getAbsolutePath().hashCode());

		assertEquals("1.0", 0, getInstallableUnits(listener).length);
		assertEquals("1.1", 0, listener.getArtifactRepository().getArtifactKeys().length);

		DirectoryWatcher watcher = new DirectoryWatcher(props, TestActivator.getContext());
		watcher.addListener(listener);
		watcher.start();

		assertEquals("2.0", 0, getInstallableUnits(listener).length);
		assertEquals("2.1", 0, listener.getArtifactRepository().getArtifactKeys().length);

		try {
			copy(baseFolder, folder);
		} catch (IOException e) {
			fail("2.2", e);
		}
		watcher.poll();
		watcher.stop();

		IFileArtifactRepository repo = (IFileArtifactRepository) listener.getArtifactRepository();
		IArtifactKey[] keys = repo.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			assertTrue("2.3", repo.getArtifactFile(keys[i]).toString().startsWith(folder.getAbsolutePath().toString()));
		}

		assertEquals("3.0", 2, getInstallableUnits(listener).length);
		assertEquals("3.1", 2, listener.getArtifactRepository().getArtifactKeys().length);

		watcher = new DirectoryWatcher(props, TestActivator.getContext());
		watcher.addListener(listener);
		watcher.start();

		assertEquals("4.0", 2, getInstallableUnits(listener).length);
		assertEquals("4.1", 2, listener.getArtifactRepository().getArtifactKeys().length);

		try {
			copy(baseFolder2, folder);
		} catch (IOException e) {
			fail("4.99", e);
		}
		watcher.poll();

		assertEquals("5.0", 5, getInstallableUnits(listener).length);
		assertEquals("5.1", 3, listener.getArtifactRepository().getArtifactKeys().length);

		watcher.stop();

		watcher = new DirectoryWatcher(props, TestActivator.getContext());
		watcher.addListener(listener);
		watcher.start();

		try {
			removeContents(baseFolder, folder);
		} catch (Exception e) {
			fail("5.99", e);
		}
		watcher.poll();

		assertEquals("6.0", 1, getInstallableUnits(listener).length);
		assertEquals("6.1", 1, listener.getArtifactRepository().getArtifactKeys().length);

		watcher.stop();

		delete(folder);
	}
}
