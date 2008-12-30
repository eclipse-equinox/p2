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
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.net.*;
import junit.framework.TestCase;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

public class JarURLRepositoryTest extends TestCase {

	private ServiceReference managerRef;
	private IArtifactRepositoryManager manager;

	public JarURLRepositoryTest(String name) {
		super(name);
	}

	public JarURLRepositoryTest() {
		super("");
	}

	protected void setUp() throws Exception {
		managerRef = TestActivator.getContext().getServiceReference(IArtifactRepositoryManager.class.getName());
		manager = (IArtifactRepositoryManager) TestActivator.getContext().getService(managerRef);
	}

	protected void tearDown() throws Exception {
		manager = null;
		TestActivator.getContext().ungetService(managerRef);
	}

	public void testJarURLRepository() throws ProvisionException, URISyntaxException {
		URL engineJar = TestActivator.getContext().getBundle().getEntry("/testData/enginerepo.jar");
		URL jarRepoURL = null;
		try {
			jarRepoURL = new URL("jar:" + engineJar.toString() + "!/testData/enginerepo/artifacts.xml");
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}
		IArtifactRepository repo = manager.loadRepository(URIUtil.toURI(jarRepoURL), null);
		assertTrue(repo.contains(new ArtifactKey("osgi.bundle", "testdata", new Version("1.0.0.1"))));
	}
}
