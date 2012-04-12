/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.repository;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.equinox.p2.tests.*;

public class RepositoryExtensionPointTest extends AbstractProvisioningTest {
	/**
	 * A non-URL URI, e.g. for identifying a repository in a registry.
	 */
	static URI repositoryKey = URI.create("testregistry:repo");
	static IMetadataRepository metadataRepositoryInstance = new TestMetadataRepository(getAgent());
	static IArtifactRepository artifactRepositoryInstance = new TestArtifactRepository(getAgent(), repositoryKey);

	public static class TestMetadataRepositoryRegistry extends MetadataRepositoryFactory {

		@Override
		public IMetadataRepository create(URI location, String name, String type, Map<String, String> properties) throws ProvisionException {
			// none of the codes defined in ProvisionException really fit
			int errorCode = 0;
			Status status = new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, errorCode, "Creating repositories of type " + type + " is not supported", null);
			throw new ProvisionException(status);
		}

		@Override
		public IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) {
			if (repositoryKey.equals(location)) {
				return metadataRepositoryInstance;
			}
			return null;
		}
	}

	public static class TestArtifactRepositoryRegistry extends ArtifactRepositoryFactory {

		@Override
		public IArtifactRepository create(URI location, String name, String type, Map<String, String> properties) throws ProvisionException {
			// none of the codes defined in ProvisionException really fit
			int errorCode = 0;
			Status status = new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, errorCode, "Creating repositories of type " + type + " is not supported", null);
			throw new ProvisionException(status);
		}

		@Override
		public IArtifactRepository load(URI location, int flags, IProgressMonitor monitor) {
			if (repositoryKey.equals(location)) {
				return artifactRepositoryInstance;
			}
			return null;
		}
	}

	public void testLoadMetadataRepositoryFromURIWithCustomScheme() throws Exception {
		IMetadataRepository repo = loadMetadataRepository(repositoryKey);

		assertThat(repo, sameInstance(metadataRepositoryInstance));
	}

	public void testLoadArtifactRepositoryFromURIWithCustomScheme() throws Exception {
		IArtifactRepository repo = loadArtifactRepository(repositoryKey);

		assertThat(repo, sameInstance(artifactRepositoryInstance));
	}
}
