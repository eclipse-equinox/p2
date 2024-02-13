/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *     Ericsson AB (Pascal Rapicault) - Support to reuse bundles in place
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.AggregatedBundleRepository;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * @since 1.0
 */
public class UtilTest extends AbstractProvisioningTest {
	/*
	 * Constructor for the class.
	 */
	public UtilTest(String name) {
		super(name);
	}

	/*
	 * Run all the tests in this class.
	 */
	public static Test suite() {
		return new TestSuite(UtilTest.class);
	}

	public void testDefaultBundlePool() {
		IProfile profile = createProfile("test");
		IAgentLocation agentLocation = getAgent().getService(IAgentLocation.class);
		assertEquals(agentLocation.getDataArea("org.eclipse.equinox.p2.touchpoint.eclipse"), Util.getBundlePoolLocation(getAgent(), profile));
	}

	@SuppressWarnings("deprecation") // java.io.File.toURL()
	public void testExplicitBundlePool() throws MalformedURLException {
		Map<String, String> props = new HashMap<>();
		File cacheDir = new File(System.getProperty("java.io.tmpdir"), "cache");
		props.put(IProfile.PROP_CACHE, cacheDir.toString());
		IProfile profile = createProfile("test", props);
		assertEquals(cacheDir.toURL().toExternalForm(), Util.getBundlePoolLocation(getAgent(), profile).toString());
	}

	public void testCheckRunnableArtifactRepos() throws ProvisionException {
		File withFlag = getTestData("Get artifact repo in runnable format", "testData/utilTest/repoWithFlag");
		File withoutFlag = getTestData("Get artifact repo in runnable format", "testData/utilTest/repoWithoutFlag");
		IArtifactRepository repoWithFlag = getArtifactRepositoryManager().loadRepository(withFlag.toURI(), new NullProgressMonitor());
		IArtifactRepository repoWithoutFlag = getArtifactRepositoryManager().loadRepository(withoutFlag.toURI(), new NullProgressMonitor());

		assertNotNull(repoWithFlag.getProperty(IArtifactRepository.PROP_RUNNABLE));
		assertTrue(Boolean.TRUE.toString().equalsIgnoreCase(repoWithFlag.getProperty(IArtifactRepository.PROP_RUNNABLE)));
		assertNull(repoWithoutFlag.getProperty(IArtifactRepository.PROP_RUNNABLE));

		AggregatedBundleRepository repos = (AggregatedBundleRepository) Util.getAggregatedBundleRepository(getAgent(), null, 0);
		assertTrue(repos.testGetBundleRepositories().contains(repoWithFlag));
		assertTrue(!repos.testGetBundleRepositories().contains(repoWithoutFlag));

	}
}
