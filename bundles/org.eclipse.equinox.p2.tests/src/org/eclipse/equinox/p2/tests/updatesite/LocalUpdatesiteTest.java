/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.updatesite;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSitePublisherApplication;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;

public class LocalUpdatesiteTest extends AbstractProvisioningTest {
	protected File repoLocation;

	protected void setUp() throws Exception {
		super.setUp();
		String tempDir = System.getProperty("java.io.tmpdir");
		repoLocation = new File(tempDir, "LocalMetadataRepositoryTest");
		AbstractProvisioningTest.delete(repoLocation);
		repoLocation.mkdir();
	}

	protected void tearDown() throws Exception {
		getMetadataRepositoryManager().removeRepository(repoLocation.toURI());
		delete(repoLocation);
		super.tearDown();
	}

	public void testCategoryQualifier() throws IOException, ProvisionException {
		File siteSource = TestData.getFile("updatesite", "SiteXMLActionTest");
		UpdateSitePublisherApplication application = new UpdateSitePublisherApplication();
		try {
			application.run(new String[] {"-metadataRepository", repoLocation.toURI().toString(), "-source", siteSource.toString(), "-categoryQualifier", "fooQualifier"});
		} catch (Exception e) {
			fail("0.99");
		}
		IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repoLocation.toURI(), new NullProgressMonitor());
		IQueryResult results = repository.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(results));
		Iterator iter = results.iterator();
		while (iter.hasNext()) {
			IInstallableUnit unit = (IInstallableUnit) iter.next();
			assertTrue("2.0", unit.getId().startsWith("fooQualifier"));
			assertEquals("3.0", "Test Category Label", unit.getProperty(IInstallableUnit.PROP_NAME));
		}
	}

	public void testBundleInCategory() throws Exception {
		File siteSource = TestData.getFile("updatesite", "SiteXMLActionTest");
		UpdateSitePublisherApplication application = new UpdateSitePublisherApplication();
		try {
			application.run(new String[] {"-metadataRepository", repoLocation.toURI().toString(), "-source", siteSource.toString(), "-categoryQualifier", "fooQualifier"});
		} catch (Exception e) {
			fail("0.99");
		}
		IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repoLocation.toURI(), new NullProgressMonitor());

		IQueryResult results = repository.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		Iterator iter = results.iterator();

		IInstallableUnit unit = (IInstallableUnit) iter.next();
		IQuery<IInstallableUnit> memberQuery = QueryUtil.createIUCategoryMemberQuery(unit);
		IQueryResult<IInstallableUnit> categoryMembers = repository.query(memberQuery, new NullProgressMonitor());
		Set<String> membersId = new HashSet<String>();
		for (IInstallableUnit iu : categoryMembers.toUnmodifiableSet()) {
			membersId.add(iu.getId());
		}
		assertEquals("1.0", 2, membersId.size());
		assertTrue("2.0", membersId.contains("test.bundle"));
	}
}
