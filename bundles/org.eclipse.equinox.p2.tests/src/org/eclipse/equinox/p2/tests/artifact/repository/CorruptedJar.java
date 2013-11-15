/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.engine.DownloadManager;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class CorruptedJar extends AbstractProvisioningTest {
	private static final String testDataLocation = "testData/artifactRepo/corruptedJarRepo";
	IArtifactRepository source = null;
	IArtifactRepository target = null;

	@Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		IArtifactRepositoryManager mgr = getArtifactRepositoryManager();
		try {
			source = mgr.loadRepository((getTestData("CorruptedJar repo", testDataLocation).toURI()), null);
		} catch (Exception e) {
			fail("1.0", e);
		}
		try {
			target = mgr.createRepository(getTestFolder("CorruptedJarTarget").toURI(), "CorruptedJar target repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (Exception e) {
			fail("2.0", e);
		}
	}

	public void testDownloadCorruptedJar() {
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		ctx.setArtifactRepositories(new URI[] {getTestData("CorruptedJar repo", testDataLocation).toURI()});
		DownloadManager mgr = new DownloadManager(ctx, getAgent());
		IArtifactKey key = source.query(ArtifactKeyQuery.ALL_KEYS, null).iterator().next();
		mgr.add(new MirrorRequest(key, target, null, null, getTransport()));
		IStatus s = mgr.start(new NullProgressMonitor());
		assertNotOK(s);
	}
}
