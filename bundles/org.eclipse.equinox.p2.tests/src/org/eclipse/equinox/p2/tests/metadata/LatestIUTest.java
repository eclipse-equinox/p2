/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.net.URI;
import java.util.Collection;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.LatestIUVersionQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;

/**
 *
 */
public class LatestIUTest extends AbstractProvisioningTest {

	public void testLatestIUSingleRepo1() throws Exception {
		URI location;
		try {
			location = TestData.getFile("metadataRepo", "multipleversions1").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(location);

		Collector query = metadataRepositoryManager.query(new LatestIUVersionQuery(), new Collector(), null);
		Collection collection = query.toCollection();
		assertEquals("1.0", 1, collection.size());
		assertEquals("1.1", Version.createOSGi(2, 1, 0), ((IInstallableUnit) collection.iterator().next()).getVersion());
	}

	public void testLatestIUSingleRepo2() throws Exception {
		URI location;
		try {
			location = TestData.getFile("metadataRepo", "multipleversions2").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(location);

		Collector query = metadataRepositoryManager.query(new LatestIUVersionQuery(), new Collector(), null);
		Collection collection = query.toCollection();
		assertEquals("1.0", 1, collection.size());
		assertEquals("1.1", Version.createOSGi(3, 0, 0), ((IInstallableUnit) collection.iterator().next()).getVersion());
	}

	public void testLatestIUMultiRepo() throws Exception {
		URI location1;
		URI location2;
		try {
			location1 = TestData.getFile("metadataRepo", "multipleversions1").toURI();
			location2 = TestData.getFile("metadataRepo", "multipleversions2").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(location1);
		metadataRepositoryManager.addRepository(location2);

		Collector collector = metadataRepositoryManager.query(new LatestIUVersionQuery(), new Collector(), null);
		Collection collection = collector.toCollection();
		assertEquals("1.0", 1, collection.size());
		assertEquals("1.1", Version.createOSGi(3, 0, 0), ((IInstallableUnit) collection.iterator().next()).getVersion());
	}
}
