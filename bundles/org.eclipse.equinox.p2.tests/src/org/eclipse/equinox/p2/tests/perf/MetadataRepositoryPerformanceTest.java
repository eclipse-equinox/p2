/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.perf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Performance tests for metadata repositories
 */
public class MetadataRepositoryPerformanceTest extends ProvisioningPerformanceTest {
	private static final int REPEATS = 5;
	protected File repoLocation;
	IMetadataRepository repository;

	protected void setUp() throws Exception {
		super.setUp();
		String tempDir = System.getProperty("java.io.tmpdir");
		repoLocation = new File(tempDir, "MetadataRepositoryPerformanceTest");
		delete(repoLocation);
		repoLocation.mkdir();
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		repository = manager.createRepository(repoLocation.toURI(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
	}

	protected void tearDown() throws Exception {
		getMetadataRepositoryManager().removeRepository(repoLocation.toURI());
		delete(repoLocation);
		super.tearDown();
	}

	public void testQueryLocalRepository() {
		final int IU_COUNT = 3000;
		new PerformanceTestRunner() {
			IQuery[] queries = new IQuery[IU_COUNT];

			protected void setUp() {
				List<IInstallableUnit> ius = new ArrayList(IU_COUNT);
				for (int i = 0; i < IU_COUNT; i++) {
					IInstallableUnit iu = generateIU(i);
					queries[i] = QueryUtil.createIUQuery(iu.getId(), iu.getVersion());
					ius.add(iu);
				}
				repository.addInstallableUnits(ius);
			}

			protected void tearDown() {
			}

			protected void test() {
				for (int i = 0; i < queries.length; i++) {
					repository.query(queries[i], null);
				}
			}
		}.run(this, "Test query local metadata repo for " + IU_COUNT + " ius", REPEATS, 10);
	}
}
