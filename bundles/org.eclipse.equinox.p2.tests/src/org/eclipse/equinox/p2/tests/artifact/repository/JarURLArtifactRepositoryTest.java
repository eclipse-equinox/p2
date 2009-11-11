/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.net.*;
import junit.framework.TestCase;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

public class JarURLArtifactRepositoryTest extends TestCase {

	private ServiceReference managerRef;
	private IArtifactRepositoryManager manager;

	public JarURLArtifactRepositoryTest(String name) {
		super(name);
	}

	public JarURLArtifactRepositoryTest() {
		super("");
	}

	protected void setUp() throws Exception {
		managerRef = TestActivator.getContext().getServiceReference(IArtifactRepositoryManager.SERVICE_NAME);
		manager = (IArtifactRepositoryManager) TestActivator.getContext().getService(managerRef);
	}

	protected void tearDown() throws Exception {
		manager = null;
		TestActivator.getContext().ungetService(managerRef);
	}

	public void testJarURLRepository() throws ProvisionException, URISyntaxException {
		URL engineJar = TestActivator.getContext().getBundle().getEntry("/testData/enginerepo.jar");
		URI jarRepoLocation = null;
		try {
			jarRepoLocation = URIUtil.toURI(new URL("jar:" + engineJar.toString() + "!/testData/enginerepo/artifacts.xml"));
		} catch (URISyntaxException e) {
			fail(e.getMessage());
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}
		IArtifactRepository repo = manager.loadRepository(jarRepoLocation, null);
		assertTrue(repo.contains(new ArtifactKey("osgi.bundle", "testdata", Version.create("1.0.0.1"))));
	}
}
