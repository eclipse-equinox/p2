/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.util.Properties;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
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
		PermissiveSlicer slicer = new PermissiveSlicer(repo, new Properties(), true, false, true, false);
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new Collector(), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size();
		assertEquals(66, result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size());
		assertEquals(1, result.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new Collector(), new NullProgressMonitor()).size());
		//		assertOK("1.0", slicer.getStatus());
	}

	//Test with and without optional pieces
	public void testSliceRCPWithOptionalPieces() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, new Properties(), false, false, true, false);
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new Collector(), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size();
		assertEquals(64, result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size());
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testSliceRCPWithIgnoringGreed() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, new Properties(), false, true, true, false);
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new Collector(), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size();
		assertEquals(64, result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size());
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testSliceRCPWithFilter() {
		Properties p = new Properties();
		p.setProperty("osgi.os", "win32");
		p.setProperty("osgi.ws", "win32");
		p.setProperty("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, false, false);
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new Collector(), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size();
		assertEquals(0, result.query(new InstallableUnitQuery("org.eclipse.swt.motif.linux.x86"), new Collector(), new NullProgressMonitor()).size());
		assertEquals(34, result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size());
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testStrictDependency() {
		Properties p = new Properties();
		p.setProperty("osgi.os", "win32");
		p.setProperty("osgi.ws", "win32");
		p.setProperty("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, false, false, true);
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new Collector(), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size();
		assertEquals(0, result.query(new InstallableUnitQuery("org.eclipse.ecf"), new Collector(), new NullProgressMonitor()).size());
		assertEquals(29, result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size());
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testExtractPlatformIndependentPieces() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, new Properties(), true, false, false, false);
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new Collector(), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size();
		assertEquals(32, result.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size());
		assertEquals(1, result.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new Collector(), new NullProgressMonitor()).size());
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testMissingOptionalDependency() {

	}

	public void testMissingNecessaryPiece() {
	}
}
