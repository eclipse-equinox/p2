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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.tests.TestActivator;

public class JarURLArtifactRepositoryTest extends TestCase {

	private IArtifactRepositoryManager manager;

	public JarURLArtifactRepositoryTest(String name) {
		super(name);
	}

	public JarURLArtifactRepositoryTest() {
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
	}

	public void testJarURLRepository() throws ProvisionException {
		URL engineJar = TestActivator.getContext().getBundle().getEntry("/testData/enginerepo.jar");
		URI jarRepoLocation = null;
		try {
			jarRepoLocation = URIUtil.toURI(new URL("jar:" + engineJar.toString() + "!/testData/enginerepo/"));
		} catch (URISyntaxException e) {
			fail(e.getMessage());
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}
		IArtifactRepository repo = manager.loadRepository(jarRepoLocation, null);
		assertTrue(repo.contains(new ArtifactKey("osgi.bundle", "testdata", Version.create("1.0.0.1"))));
	}
}
