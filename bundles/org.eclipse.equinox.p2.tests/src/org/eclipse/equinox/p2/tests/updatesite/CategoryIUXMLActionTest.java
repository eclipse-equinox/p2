/******************************************************************************* 
* Copyright (c) 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.updatesite;

import java.io.File;
import java.net.URI;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.updatesite.CategoryXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.*;

/**
 *
 */
public class CategoryIUXMLActionTest extends AbstractProvisioningTest {

	private TestMetadataRepository metadataRepository;
	private IPublisherResult actionResult;
	private URI siteLocation;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		actionResult = new PublisherResult();
		PublisherInfo info = new PublisherInfo();
		metadataRepository = new TestMetadataRepository(getAgent(), new IInstallableUnit[0]);
		info.setMetadataRepository(metadataRepository);
		siteLocation = TestData.getFile("updatesite", "CategoryXMLActionTest/" + getName() + ".xml").toURI();
		FeaturesAction featuresAction = new FeaturesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		featuresAction.perform(info, actionResult, new NullProgressMonitor());

		CategoryXMLAction action = new CategoryXMLAction(siteLocation, null);
		action.perform(info, actionResult, getMonitor());
	}

	public void testIUCategoryCreation01() throws Exception {
		doCategorySetTest();
	}

	public void testIUCategoryCreation02() throws Exception {
		doCategorySetTest();
	}

	public void testIUCategoryCreation03() throws Exception {
		doCategorySetTest();
	}

	public void testIUCategoryCreation04() throws Exception {
		doCategoryNotSetTest();
	}

	public void testIUCategoryCreation05() throws Exception {
		doCategoryNotSetTest();
	}

	public void testIUCategoryCreation06() throws Exception {
		doCategoryNotSetTest();
	}

	public void testIUCategoryCreation07() throws Exception {
		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(result));
	}

	private void doCategorySetTest() {
		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(result));
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "Test Category Label", iu.getProperty(IInstallableUnit.PROP_NAME));
	}

	private void doCategoryNotSetTest() {
		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 0, queryResultSize(result));
	}
}
