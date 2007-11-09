/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.optimizers.pack200;

import java.io.*;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.optimizers.jardelta.Optimizer;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

public class JarDeltaRepositoryTest extends TestCase {
	private ServiceTracker managerTracker;
	private File workDir;

	public JarDeltaRepositoryTest(String name) {
		super(name);
	}

	public JarDeltaRepositoryTest() {
		super("");
	}

	protected void setUp() throws Exception {
		managerTracker = new ServiceTracker(TestActivator.getContext(), IArtifactRepositoryManager.class.getName(), null);
		managerTracker.open();
	}

	protected void tearDown() throws Exception {
		managerTracker.close();
		if (workDir != null)
			FileUtils.deleteAll(workDir);
	}

	public void testJarURLRepository() {
		URL repositoryJar = TestActivator.getContext().getBundle().getEntry("/testData/enginerepo.zip");
		URL repositoryURL = extractRepositoryJAR(repositoryJar);
		assertNotNull("Could not extract repository", repositoryURL);
		IArtifactRepository repository = ((IArtifactRepositoryManager) managerTracker.getService()).loadRepository(repositoryURL, null);
		IArtifactKey key = new ArtifactKey("eclipse", "plugin", "testdata", new Version("1.0.0.2"));
		IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
		assertTrue("Artifact Descriptor for engine missing", descriptors.length == 1);

		new Optimizer(repository, 1, 1).run();
		descriptors = repository.getArtifactDescriptors(key);
		assertTrue("Optimization was a no-op", descriptors.length == 2);

		IArtifactDescriptor canonical = null;
		IArtifactDescriptor optimized = null;
		for (int i = 0; i < descriptors.length; i++) {
			if (descriptors[i].getProcessingSteps().length == 0)
				canonical = descriptors[i];
			else
				optimized = descriptors[i];
		}

		assertTrue("Optmized descriptor not found", optimized != null);
		assertTrue("Canonical descriptor not found", canonical != null);
		long optimizedSize = Long.parseLong(optimized.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
		long canonicalSize = Long.parseLong(canonical.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
		assertTrue("Optimzed not smaller than canonical", optimizedSize < canonicalSize);

		File canonicalFile = fetchArtifact("canonical", canonical);
		File optimizedFile = fetchArtifact("optimized", optimized);
		compareFiles(canonicalFile, optimizedFile);
	}

	private URL extractRepositoryJAR(URL source) {
		String filter = "(protocol=" + source.getProtocol() + ")";
		URLConverter converter = (URLConverter) ServiceHelper.getService(TestActivator.getContext(), URLConverter.class.getName(), filter);
		try {
			if (converter == null)
				return null;
			URL repoURL = converter.toFileURL(source);
			if (!repoURL.toExternalForm().endsWith(".jar") && !repoURL.toExternalForm().endsWith(".zip"))
				return repoURL;
			// else the repo is a JAR or zip and we should extract the contents to a work dir.
			File repoLocation = getWorkDir();
			FileUtils.unzipFile(new File(repoURL.getPath()), repoLocation);
			return repoLocation.toURL();
		} catch (IOException e) {
			return null;
		}
	}

	private void compareFiles(File canonicalFile, File optimizedFile) {
		assertTrue("Canonical file does not exist", canonicalFile.exists());
		assertTrue("Optimized file does not exist", optimizedFile.exists());
		assertEquals(canonicalFile.length(), optimizedFile.length());
		// TODO compare the actual content
	}

	private File fetchArtifact(String name, IArtifactDescriptor descriptor) {
		try {
			File result = new File(getWorkDir(), name);
			OutputStream destination = new BufferedOutputStream(new FileOutputStream(result));
			try {
				descriptor.getRepository().getArtifact(descriptor, destination, new NullProgressMonitor());
				return result;
			} finally {
				if (destination != null)
					destination.close();
			}
		} catch (IOException e) {
			fail("Could not fetch artifact " + descriptor);
		}
		return null;
	}

	private File getWorkDir() throws IOException {
		if (workDir != null)
			return workDir;
		workDir = File.createTempFile("work", "");
		if (!workDir.delete())
			throw new IOException("Could not delete file for creating temporary working dir.");
		if (!workDir.mkdirs())
			throw new IOException("Could not create temporary working dir.");
		return workDir;
	}

}
