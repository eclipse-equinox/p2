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
package org.eclipse.equinox.p2.tests.generator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.Generator;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.IGeneratorInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.tests.*;
import org.osgi.framework.Bundle;

/**
 * Tests running the metadata generator against Eclipse 3.3 features.
 */
public class EclipseSDK33Test extends AbstractProvisioningTest {

	public static Test suite() {
		return new TestSuite(EclipseSDK33Test.class);
	}

	public EclipseSDK33Test() {
		super("");
	}

	public EclipseSDK33Test(String name) {
		super(name);
	}

	/**
	 * TODO This test is currently failing on the build machine for an unknown reason.
	 */
	public void testGeneration() {
		if (DISABLED)
			return;
		IGeneratorInfo generatorInfo = createGeneratorInfo();
		Generator generator = new Generator(generatorInfo);
		generator.generate();

		TestMetadataRepository repo = (TestMetadataRepository) generatorInfo.getMetadataRepository();
		IInstallableUnit unit = repo.find("org.eclipse.cvs.source.feature.group", "1.0.0.v20070606-7C79_79EI99g_Y9e");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.rcp.feature.group", "3.3.0.v20070607-8y8eE8NEbsN3X_fjWS8HPNG");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.jdt.feature.group", "3.3.0.v20070606-0010-7o7jCHEFpPoqQYvnXqejeR");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.cvs.feature.group", "1.0.0.v20070606-7C79_79EI99g_Y9e");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.pde.source.feature.group", "3.3.0.v20070607-7N7M-DUUEF6Ez0H46IcCC");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.sdk.feature.group", "3.3.0.v20070607-7M7J-BIolz-OcxWxvWAPSfLPqevO");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.platform.feature.group", "3.3.0.v20070612-_19UEkLEzwsdF9jSqQ-G");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.platform.source.feature.group", "3.3.0.v20070612-_19UEkLEzwsdF9jSqQ-G");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.jdt.source.feature.group", "3.3.0.v20070606-0010-7o7jCHEFpPoqQYvnXqejeR");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.pde.feature.group", "3.3.0.v20070607-7N7M-DUUEF6Ez0H46IcCC");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.rcp.source.feature.group", "3.3.0.v20070607-8y8eE8NEbsN3X_fjWS8HPNG");
		assertNotNull(unit);
		assertGroup(unit);

		IArtifactRepository artifactRepo = generatorInfo.getArtifactRepository();
		assertTrue(getArtifactKeyCount(artifactRepo) == 11);
	}

	/**
	 * Asserts that the given IU represents a group.
	 */
	private void assertGroup(IInstallableUnit unit) {
		assertEquals("IU is not a group", Boolean.TRUE.toString(), QueryUtil.isGroup(unit));
	}

	private IGeneratorInfo createGeneratorInfo() {
		Bundle myBundle = TestActivator.getContext().getBundle();
		URL root = FileLocator.find(myBundle, new Path("testData/generator/eclipse3.3"), null);
		File rootFile = null;
		try {
			root = FileLocator.toFileURL(root);
			rootFile = new File(root.getPath());
		} catch (IOException e) {
			fail("4.99", e);
		}
		TestGeneratorInfo info = new TestGeneratorInfo(getAgent(), rootFile);
		return info;
	}

}
