/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PermissiveSlicerTest extends AbstractProvisioningTest {
	private IMetadataRepository repo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		File repoFile = getTestData("Repo for permissive slicer test", "testData/permissiveSlicer");
		repo = getMetadataRepositoryManager().loadRepository(repoFile.toURI(), new NullProgressMonitor());
	}

	public void testSliceRCPOut() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, Collections.emptyMap(), true, false, true, false, false);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()));
		assertEquals(66, queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
		assertEquals(1, queryResultSize(result.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	//Test with and without optional pieces
	public void testSliceRCPWithOptionalPieces() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, Collections.emptyMap(), false, false, true, false, false);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()));
		assertEquals(64, queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testSliceRCPWithIgnoringGreed() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, Collections.emptyMap(), false, true, true, false, false);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()));
		assertEquals(64, queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testSliceRCPWithFilter() {
		Map<String, String> p = new HashMap<>();
		p.put("osgi.os", "win32");
		p.put("osgi.ws", "win32");
		p.put("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, false, false, false);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()));
		assertEquals(0, queryResultSize(result.query(QueryUtil.createIUQuery("org.eclipse.swt.motif.linux.x86"), new NullProgressMonitor())));
		assertEquals(34, queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testStrictDependency() {
		Map<String, String> p = new HashMap<>();
		p.put("osgi.os", "win32");
		p.put("osgi.ws", "win32");
		p.put("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, false, false, true, false);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()));
		assertEquals(0, queryResultSize(result.query(QueryUtil.createIUQuery("org.eclipse.ecf"), new NullProgressMonitor())));
		assertEquals(29, queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testExtractPlatformIndependentPieces() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, Collections.emptyMap(), true, false, false, false, false);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iu), new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()));
		assertEquals(32, queryResultSize(result.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
		assertEquals(1, queryResultSize(result.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testMetaRequirements() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		PermissiveSlicer slicer = new PermissiveSlicer(createTestMetdataRepository(new IInstallableUnit[] {a, act1}), Collections.emptyMap(), true, false, false, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(a), new NullProgressMonitor());
		assertEquals(1, queryResultSize(result.query(QueryUtil.createIUQuery("Action1"), null)));
	}

	public void testValidateIU() {
		Map<String, String> p = new HashMap<>();
		p.put("osgi.os", "win32");
		p.put("osgi.ws", "win32");
		p.put("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, false, false, true, false);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.swt.cocoa.macosx"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();
		assertNull(slicer.slice(List.of(iu), new NullProgressMonitor()));
		assertNotOK(slicer.getStatus());
	}

	public void testMissingNecessaryPiece() {
		IRequirement[] req = createRequiredCapabilities("B", "B", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit iuA = createIU("A", DEFAULT_VERSION, null, req, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, true);
		PermissiveSlicer slicer = new PermissiveSlicer(createTestMetdataRepository(new IInstallableUnit[] {iuA}), Collections.emptyMap(), true, false, false, false, false);
		IQueryable<IInstallableUnit> result = slicer.slice(List.of(iuA), new NullProgressMonitor());
		assertNotNull(result);
		assertNotOK(slicer.getStatus());
	}

	public void testExtractOnlyPlatformSpecificForOnePlatform() {
		Map<String, String> p = new HashMap<>();
		p.put("osgi.os", "win32");
		p.put("osgi.ws", "win32");
		p.put("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, false, false, false, true);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();
		IQueryResult<IInstallableUnit> resultCollector = slicer.slice(List.of(iu), new NullProgressMonitor()).query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertEquals(3, queryResultSize(resultCollector));
	}

	public void testExtractOnlyPlatformSpecific() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, Collections.emptyMap(), true, false, true, false, true);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = c.iterator().next();
		IQueryResult<IInstallableUnit> resultCollector = slicer.slice(List.of(iu), new NullProgressMonitor())
				.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertEquals(35, queryResultSize(resultCollector));
	}

}
