/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.net.URI;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
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

		IQueryResult query = metadataRepositoryManager.query(QueryUtil.createLatestIUQuery(), null);
		assertEquals("1.0", 1, queryResultSize(query));
		assertEquals("1.1", Version.createOSGi(2, 1, 0), ((IInstallableUnit) query.iterator().next()).getVersion());
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

		IQueryResult query = metadataRepositoryManager.query(QueryUtil.createLatestIUQuery(), null);
		assertEquals("1.0", 1, queryResultSize(query));
		assertEquals("1.1", Version.createOSGi(3, 0, 0), ((IInstallableUnit) query.iterator().next()).getVersion());
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

		IQueryResult queryResult = metadataRepositoryManager.query(QueryUtil.createLatestIUQuery(), null);
		assertEquals("1.0", 1, queryResultSize(queryResult));
		assertEquals("1.1", Version.createOSGi(3, 0, 0), ((IInstallableUnit) queryResult.iterator().next()).getVersion());
	}
}
