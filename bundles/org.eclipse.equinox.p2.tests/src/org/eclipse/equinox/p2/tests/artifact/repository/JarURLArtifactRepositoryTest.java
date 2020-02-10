/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     Sonatype Inc - ongoing development
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JarURLArtifactRepositoryTest {

	private IArtifactRepositoryManager manager;

	@Before
	public void setUp() throws Exception {
		IProvisioningAgent agent = ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgent.class);
		manager = agent.getService(IArtifactRepositoryManager.class);
	}

	@After
	public void tearDown() throws Exception {
		manager = null;
	}

	@Test
	public void testJarURLRepository() throws ProvisionException, MalformedURLException, URISyntaxException {
		URL engineJar = TestActivator.getContext().getBundle().getEntry("/testData/enginerepo.jar");
		URI jarRepoLocation = URIUtil.toURI(new URL("jar:" + engineJar.toString() + "!/testData/enginerepo/"));
		IArtifactRepository repo = manager.loadRepository(jarRepoLocation, null);
		assertTrue(repo.contains(new ArtifactKey("osgi.bundle", "testdata", Version.create("1.0.0.1"))));
	}
}
