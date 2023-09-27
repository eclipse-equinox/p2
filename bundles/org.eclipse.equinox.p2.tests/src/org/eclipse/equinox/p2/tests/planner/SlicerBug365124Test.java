/*******************************************************************************
 *  Copyright (c) 2011, 2017 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SlicerBug365124Test extends AbstractProvisioningTest {
	private IMetadataRepository repo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		File repoFile = getTestData("Repo for slicer test", "testData/slicerBug365124Test");
		repo = getMetadataRepositoryManager().loadRepository(repoFile.toURI(), new NullProgressMonitor());
	}

	private Map<String, String> getProperties() {
		Map<String, String> result = new HashMap<>();
		result.put("org.eclipse.equinox.p2.installFolder", "/Users/equinox/Downloads/eclipse");
		result.put("osgi.nl", "en_US");
		result.put("osgi.ws", "cocoa");
		result.put("org.eclipse.equinox.p2.cache", "/Users/equinox/Downloads/eclipse");
		result.put("org.eclipse.equinox.p2.cache.extensions", "file:/Users/equinox/Downloads/eclipse/.eclipseextension|file:/Users/equinox/Downloads/eclipse/configuration/org.eclipse.osgi/bundles/84/data/listener_1925729951/");
		result.put("osgi.os", "macosx");
		result.put("osgi.arch", "x86_64");
		result.put("org.eclipse.update.install.features", "true");
		result.put("eclipse.touchpoint.launcherName", "eclipse");
		result.put("org.eclipse.equinox.p2.roaming", "true");
		result.put("org.eclipse.equinox.p2.environments", "osgi.nl=en_US,osgi.ws=cocoa,osgi.arch=x86_64,osgi.os=macosx");
		return result;
	}

	public void testSlice() {
		Slicer slicer = new Slicer(getProfile(IProfileRegistry.SELF), getProperties(), true);
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		slicer.slice(c.toUnmodifiableSet(), new NullProgressMonitor());
		assertNotOK("1.0", slicer.getStatus());
	}

}
