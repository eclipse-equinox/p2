/*******************************************************************************
* Copyright (c) 2009, 2013 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   Red Hat Inc. - 383795 (bundle element), 406902 (nested categories)
******************************************************************************/
package org.eclipse.equinox.p2.tests.updatesite;

import java.io.File;
import java.net.URI;
import java.util.Set;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.updatesite.CategoryXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.MergeResultsAction;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.*;

/**
 *
 */
public class CategoryXMLActionTest extends AbstractProvisioningTest {

	private TestMetadataRepository metadataRepository;
	private IPublisherResult actionResult;
	private URI siteLocation;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		actionResult = new PublisherResult();
		metadataRepository = new TestMetadataRepository(getAgent(), new IInstallableUnit[0]);
	}

	public void testCategoryCreation() throws Exception {
		PublisherInfo info = new PublisherInfo();
		info.setMetadataRepository(metadataRepository);
		siteLocation = TestData.getFile("updatesite", "CategoryXMLActionTest/category.xml").toURI();
		FeaturesAction featuresAction = new FeaturesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		featuresAction.perform(info, actionResult, new NullProgressMonitor());

		CategoryXMLAction action = new CategoryXMLAction(siteLocation, null);
		action.perform(info, actionResult, getMonitor());

		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(result));
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "Test Category Label", iu.getProperty(IInstallableUnit.PROP_NAME));
	}

	public void testCategoryCreationMultiFeature() throws Exception {
		PublisherInfo info = new PublisherInfo();

		info.setMetadataRepository(metadataRepository);
		siteLocation = TestData.getFile("updatesite", "CategoryXMLActionTest/category01.xml").toURI();
		FeaturesAction featuresAction = new FeaturesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		featuresAction.perform(info, actionResult, new NullProgressMonitor());

		CategoryXMLAction action = new CategoryXMLAction(siteLocation, null);
		action.perform(info, actionResult, getMonitor());

		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(result));
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "Test Category Label", iu.getProperty(IInstallableUnit.PROP_NAME));

		IQuery<IInstallableUnit> memberQuery = QueryUtil.createIUCategoryMemberQuery(iu);
		IQueryResult<IInstallableUnit> categoryMembers = actionResult.query(memberQuery, new NullProgressMonitor());
		assertEquals("2.0", 3, categoryMembers.toUnmodifiableSet().size());
	}

	public void testCategoryCreationMultiFeatureQualifier() throws Exception {
		PublisherInfo info = new PublisherInfo();

		info.setMetadataRepository(metadataRepository);
		siteLocation = TestData.getFile("updatesite", "CategoryXMLActionTest/category02.xml").toURI();
		FeaturesAction featuresAction = new FeaturesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		featuresAction.perform(info, actionResult, new NullProgressMonitor());

		CategoryXMLAction action = new CategoryXMLAction(siteLocation, null);
		action.perform(info, actionResult, getMonitor());

		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(result));
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "Test Category Label", iu.getProperty(IInstallableUnit.PROP_NAME));

		IQuery<IInstallableUnit> memberQuery = QueryUtil.createIUCategoryMemberQuery(iu);
		IQueryResult<IInstallableUnit> categoryMembers = actionResult.query(memberQuery, new NullProgressMonitor());
		assertEquals("2.0", 2, categoryMembers.toUnmodifiableSet().size());
	}

	public void testBundlesInCategory() throws Exception {
		PublisherInfo info = new PublisherInfo();

		info.setMetadataRepository(metadataRepository);
		siteLocation = TestData.getFile("updatesite", "CategoryXMLActionTest/categoryWithBundle.xml").toURI();
		FeaturesAction featuresAction = new FeaturesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		BundlesAction bundlesAction = new BundlesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		MergeResultsAction publishAction = new MergeResultsAction(new IPublisherAction[] {featuresAction, bundlesAction}, IPublisherResult.MERGE_ALL_NON_ROOT);
		publishAction.perform(info, actionResult, new NullProgressMonitor());

		CategoryXMLAction action = new CategoryXMLAction(siteLocation, null);
		action.perform(info, actionResult, getMonitor());

		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(result));
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "Test Category Label", iu.getProperty(IInstallableUnit.PROP_NAME));

		IQuery<IInstallableUnit> memberQuery = QueryUtil.createIUCategoryMemberQuery(iu);
		IQueryResult<IInstallableUnit> categoryMembers = actionResult.query(memberQuery, new NullProgressMonitor());
		Set<IInstallableUnit> categoryMembersSet = categoryMembers.toUnmodifiableSet();
		assertEquals("2.0", 1, categoryMembersSet.size());
		assertEquals("2.1", "test.bundle", categoryMembersSet.iterator().next().getId());
	}

	public void testUncategorizedBundlesInCategory() throws Exception {
		PublisherInfo info = new PublisherInfo();

		info.setMetadataRepository(metadataRepository);
		siteLocation = TestData.getFile("updatesite", "CategoryXMLActionTest/categoryUncategorizedBundle.xml").toURI();
		FeaturesAction featuresAction = new FeaturesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		BundlesAction bundlesAction = new BundlesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		MergeResultsAction publishAction = new MergeResultsAction(new IPublisherAction[] {featuresAction, bundlesAction}, IPublisherResult.MERGE_ALL_NON_ROOT);
		publishAction.perform(info, actionResult, new NullProgressMonitor());

		CategoryXMLAction action = new CategoryXMLAction(siteLocation, null);
		action.perform(info, actionResult, getMonitor());

		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 1, queryResultSize(result));
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "Uncategorized", iu.getProperty(IInstallableUnit.PROP_NAME));

		IQuery<IInstallableUnit> memberQuery = QueryUtil.createIUCategoryMemberQuery(iu);
		IQueryResult<IInstallableUnit> categoryMembers = actionResult.query(memberQuery, new NullProgressMonitor());
		Set<IInstallableUnit> categoryMembersSet = categoryMembers.toUnmodifiableSet();
		assertEquals("2.0", 1, categoryMembersSet.size());
		assertEquals("2.1", "test.bundle", categoryMembersSet.iterator().next().getId());
	}

	public void testNestedInCategory() throws Exception {
		PublisherInfo info = new PublisherInfo();

		info.setMetadataRepository(metadataRepository);
		siteLocation = TestData.getFile("updatesite", "CategoryXMLActionTest/categoryNested.xml").toURI();
		FeaturesAction featuresAction = new FeaturesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		BundlesAction bundlesAction = new BundlesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		MergeResultsAction publishAction = new MergeResultsAction(new IPublisherAction[] {featuresAction, bundlesAction}, IPublisherResult.MERGE_ALL_NON_ROOT);
		publishAction.perform(info, actionResult, new NullProgressMonitor());

		CategoryXMLAction action = new CategoryXMLAction(siteLocation, null);
		action.perform(info, actionResult, getMonitor());

		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 2, queryResultSize(result));
		IInstallableUnit rootCategoryIu = null;
		for (Object item : result) {
			if (((IInstallableUnit) item).getId().endsWith("Root Category")) {
				rootCategoryIu = (IInstallableUnit) item;
			}
		}
		assertNotNull("1.1", rootCategoryIu);

		IInstallableUnit nestedCategory = assertContainsAndGetIU(rootCategoryIu, "Nested Category");

		IQuery<IInstallableUnit> nestedCategoryMemberQuery = QueryUtil.createIUCategoryMemberQuery(nestedCategory);
		IQueryResult<IInstallableUnit> nestedCategoryMembers = actionResult.query(nestedCategoryMemberQuery, new NullProgressMonitor());
		Set<IInstallableUnit> nestedCategoryMembersSet = nestedCategoryMembers.toUnmodifiableSet();
		assertEquals("3.0", 1, nestedCategoryMembersSet.size());
		assertEquals("3.1", "test.bundle", nestedCategoryMembersSet.iterator().next().getId());
	}

	public void testMultiDepthNestedInCategory() throws Exception {
		PublisherInfo info = new PublisherInfo();

		info.setMetadataRepository(metadataRepository);
		siteLocation = TestData.getFile("updatesite", "CategoryXMLActionTest/3-depth-category.xml").toURI();
		FeaturesAction featuresAction = new FeaturesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		BundlesAction bundlesAction = new BundlesAction(new File[] {TestData.getFile("updatesite", "CategoryXMLActionTest")});
		MergeResultsAction publishAction = new MergeResultsAction(new IPublisherAction[] {featuresAction, bundlesAction}, IPublisherResult.MERGE_ALL_NON_ROOT);
		publishAction.perform(info, actionResult, new NullProgressMonitor());

		CategoryXMLAction action = new CategoryXMLAction(siteLocation, null);
		action.perform(info, actionResult, getMonitor());

		IQueryResult result = actionResult.query(QueryUtil.createIUCategoryQuery(), new NullProgressMonitor());
		assertEquals("1.0", 4, queryResultSize(result));
		IInstallableUnit rootCategoryIu = null;
		for (Object item : result) {
			if (((IInstallableUnit) item).getId().endsWith("Root Category")) {
				rootCategoryIu = (IInstallableUnit) item;
			}
		}
		assertNotNull("1.1", rootCategoryIu);

		IInstallableUnit nestedCategoryIu = assertContainsAndGetIU(rootCategoryIu, "Nested Category");
		IInstallableUnit nestedNestedCategoryIu = assertContainsAndGetIU(nestedCategoryIu, "Nested Nested Category");
		IInstallableUnit nestedNestedNestedCategoryIu = assertContainsAndGetIU(nestedNestedCategoryIu, "Nested Nested Nested Category");

		IQuery<IInstallableUnit> nestedNestedNestedCategoryMemberQuery = QueryUtil.createIUCategoryMemberQuery(nestedNestedNestedCategoryIu);
		IQueryResult<IInstallableUnit> nestedNestedNestedCategoryMembers = actionResult.query(nestedNestedNestedCategoryMemberQuery, new NullProgressMonitor());
		Set<IInstallableUnit> nestedCategoryMembersSet = nestedNestedNestedCategoryMembers.toUnmodifiableSet();
		assertEquals("3.0", 1, nestedCategoryMembersSet.size());
		assertEquals("3.1", "test.feature.feature.group", nestedCategoryMembersSet.iterator().next().getId());
	}

	private IInstallableUnit assertContainsAndGetIU(IInstallableUnit parentCategoryIu, String iuId) {
		IQuery<IInstallableUnit> rootCategoryMembersQuery = QueryUtil.createIUCategoryMemberQuery(parentCategoryIu);
		IQueryResult<IInstallableUnit> rootCategoryMembers = actionResult.query(rootCategoryMembersQuery, new NullProgressMonitor());
		Set<IInstallableUnit> rootCategoryMembersSet = rootCategoryMembers.toUnmodifiableSet();
		assertEquals("Unexpected multiple items under category", 1, rootCategoryMembersSet.size());
		IInstallableUnit nestedCategoryIu = rootCategoryMembersSet.iterator().next();
		assertTrue("Could not find IU '" + iuId + "'", nestedCategoryIu.getId().endsWith(iuId));
		return nestedCategoryIu;
	}
}
