/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.gc;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.garbagecollector.CoreGarbageCollector;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for the sweep (clean) phase of the garbage collection
 */
public class GCCleanTest extends AbstractProvisioningTest {
	@SuppressWarnings("removal")
	private IArtifactRepository createRepository(File location) throws ProvisionException {
		URI repositoryURI = location.toURI();

		IArtifactRepository repo = getArtifactRepositoryManager().createRepository(repositoryURI, "test", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, new HashMap<>());

		ArtifactDescriptor d1 = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "a", Version.create("1.0.0")));
		ArtifactDescriptor d2 = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "a", Version.create("2.0.0")));
		ArtifactDescriptor d3 = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "a", Version.create("2.0.0")));
		d3.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
		IProgressMonitor monitor = new NullProgressMonitor();
		repo.addDescriptor(d1, monitor);
		repo.addDescriptor(d2, monitor);
		repo.addDescriptor(d3, monitor);
		return repo;
	}

	public void testRemoveAll() throws ProvisionException {
		File folder = getTestFolder("GCCleanTest.testRemoveAll");
		IArtifactRepository repository = createRepository(folder);

		CoreGarbageCollector gc = new CoreGarbageCollector();

		gc.clean(new IArtifactKey[0], repository);

		assertEquals("1.0", 0, repository.query(ArtifactKeyQuery.ALL_KEYS, null).toSet().size());

	}
}
