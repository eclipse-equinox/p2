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
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class JarURLRepositoryTest extends TestCase {

	private ServiceReference managerRef;
	private IMetadataRepositoryManager manager;
	private File testRepoJar;

	public JarURLRepositoryTest(String name) {
		super(name);
	}

	public JarURLRepositoryTest() {
		this("");
	}

	protected void setUp() throws Exception {
		managerRef = TestActivator.getContext().getServiceReference(IMetadataRepositoryManager.class.getName());
		manager = (IMetadataRepositoryManager) TestActivator.getContext().getService(managerRef);

		String tempDir = System.getProperty("java.io.tmpdir");
		File testRepo = new File(tempDir, "testRepo");
		FileUtils.deleteAll(testRepo);
		testRepo.mkdir();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");
		IMetadataRepository repo = manager.createRepository(testRepo.toURL(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		InstallableUnitDescription descriptor = new MetadataFactory.InstallableUnitDescription();
		descriptor.setId("testIuId");
		descriptor.setVersion(new Version("3.2.1"));
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(descriptor);
		repo.addInstallableUnits(new IInstallableUnit[] {iu});

		testRepoJar = new File(testRepo, "content.jar");
		assertTrue(testRepoJar.exists());
		testRepoJar.deleteOnExit();
	}

	protected void tearDown() throws Exception {
		manager = null;
		FileUtils.deleteAll(testRepoJar.getParentFile());
		TestActivator.getContext().ungetService(managerRef);
	}

	public void testJarURLRepository() throws ProvisionException {
		URL jarRepoURL = null;
		try {
			jarRepoURL = new URL("jar:" + testRepoJar.toURL().toString() + "!/");
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}
		IMetadataRepository repo = manager.loadRepository(jarRepoURL, null);
		assertTrue(!repo.query(InstallableUnitQuery.ANY, new Collector(), null).isEmpty());
		manager.removeRepository(jarRepoURL);
	}
}
