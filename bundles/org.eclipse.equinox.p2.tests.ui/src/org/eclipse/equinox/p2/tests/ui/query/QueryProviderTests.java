/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import java.util.ArrayList;
import java.util.HashMap;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

public class QueryProviderTests extends AbstractProvisioningUITest {
	IInstallableUnit nestedCategory;
	IInstallableUnit a, b, c, nonGroup;
	static final String CAT = "Category";
	static final String NESTED = "NestedCategory";
	static final String A = "A";
	static final String B = "B";
	static final String C = "C";
	static final String D = "D";
	static final String E = "E";
	static final String NON_GROUP = "NonGroup"; // non-group, non-category, direct member of NESTED
	IMetadataRepository testRepo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		HashMap<String, String> categoryProperties = new HashMap<>();
		categoryProperties.put("org.eclipse.equinox.p2.type.category", "true");
		HashMap<String, String> groupProperties = new HashMap<>();
		groupProperties.put("org.eclipse.equinox.p2.type.group", "true");
		IInstallableUnit cat = createIU(CAT, Version.create("1.0.0"),
				createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, NESTED), categoryProperties, true);
		IRequirement[] nestedReqs = new IRequirement[] {
				MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, A, VersionRange.emptyRange, null, false, false),
				MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, NON_GROUP, VersionRange.emptyRange, null, false, false) };
		nestedCategory = createIU(NESTED, Version.create("1.0.0"), nestedReqs, categoryProperties, true);
		a = createIU(A, Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, B),
				groupProperties, true);
		b = createIU(B, Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, C),
				groupProperties, true);
		c = createIU(C, Version.create("1.0.0"), NO_REQUIRES, NO_PROPERTIES, true);
		nonGroup = createIU(NON_GROUP, Version.create("1.0.0"), NO_REQUIRES, NO_PROPERTIES, true);
		testRepo = createTestMetdataRepository(cat, nestedCategory, a, b, c, nonGroup);
	}

	public void testNestedCategories() {
		MetadataRepositoryElement element = new MetadataRepositoryElement(null, testRepo.getLocation(), true);
		Object[] children = element.getChildren(element);
		assertEquals("1.1", 1, children.length); // the nested category should get removed from the list
		assertTrue("1.2", children[0] instanceof CategoryElement);
		CategoryElement cat = (CategoryElement) children[0];
		children = cat.getChildren(cat);
		boolean foundNestedCategory = false;
		for (Object children1 : children) {
			if (children1 instanceof CategoryElement) {
				if (((CategoryElement) children1).getIU().equals(nestedCategory)) {
					foundNestedCategory = true;
					break;
				}
			}
		}
		assertTrue("1.3", foundNestedCategory);
	}

	public void testInstallDrilldown() {
		IUElementListRoot root = new IUElementListRoot();
		AvailableIUElement element = new AvailableIUElement(root, a, TESTPROFILE,
				getPolicy().getShowDrilldownRequirements());
		root.setChildren(new Object[] { element });
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<>();
		iusInvolved.add(a);
		InstallOperation op = new InstallOperation(getSession(), iusInvolved);
		op.setProfileId(TESTPROFILE);
		op.resolveModal(getMonitor());
		IQueryable<IInstallableUnit> queryable = op.getProvisioningPlan().getAdditions();
		element.setQueryable(queryable);
		Object[] children = element.getChildren(element);
		assertEquals("1.1", 1, children.length);
	}

	public void testFlatViewShowsCategoryMembers() {
		getPolicy().setGroupByCategory(false);
		MetadataRepositoryElement element = new MetadataRepositoryElement(null, testRepo.getLocation(), true);
		IUViewQueryContext ctx = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		ctx.setUseCategories(false);
		element.setQueryContext(ctx);

		Object[] children = element.getChildren(element);

		java.util.Set<String> ids = new java.util.HashSet<>();
		for (Object child : children) {
			IInstallableUnit iu = org.eclipse.equinox.internal.p2.ui.ProvUI.getAdapter(child, IInstallableUnit.class);
			if (iu != null) {
				ids.add(iu.getId());
			}
		}

		assertTrue(ids.contains(A));        // group, direct member of NESTED — must appear
		assertTrue(ids.contains(B));        // group — must appear
		assertTrue(ids.contains(NON_GROUP)); // non-group, direct member of NESTED — must appear
		assertFalse(ids.contains(CAT));     // top-level category itself — must not appear
		assertFalse(ids.contains(NESTED));  // nested category itself — must not appear
		assertFalse(ids.contains(C));       // not a group, not a direct category member — must not appear
	}

	public void testFlatViewShowsMembersFromAllCategoryVersions() {
		getPolicy().setGroupByCategory(false);

		HashMap<String, String> categoryProperties = new HashMap<>();
		categoryProperties.put("org.eclipse.equinox.p2.type.category", "true");

		// Two versions of the SAME category id, each requiring a different, otherwise
		// unreachable member. A fix that keys categories by id alone will drop one
		// version's requirements when the other overwrites it in the map.
		IInstallableUnit catV1 = createIU(CAT, Version.create("1.0.0"),
				createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, D), categoryProperties, true);
		IInstallableUnit catV2 = createIU(CAT, Version.create("2.0.0"),
				createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, E), categoryProperties, true);
		IInstallableUnit d = createIU(D, Version.create("1.0.0"), NO_REQUIRES, NO_PROPERTIES, true);
		IInstallableUnit e = createIU(E, Version.create("1.0.0"), NO_REQUIRES, NO_PROPERTIES, true);

		IMetadataRepository multiVersionRepo = createTestMetdataRepository(catV1, catV2, d, e);
		MetadataRepositoryElement element = new MetadataRepositoryElement(null, multiVersionRepo.getLocation(), true);
		IUViewQueryContext ctx = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		ctx.setUseCategories(false);
		element.setQueryContext(ctx);

		Object[] children = element.getChildren(element);

		java.util.Set<String> ids = new java.util.HashSet<>();
		for (Object child : children) {
			IInstallableUnit iu = org.eclipse.equinox.internal.p2.ui.ProvUI.getAdapter(child, IInstallableUnit.class);
			if (iu != null) {
				ids.add(iu.getId());
			}
		}

		assertTrue("member reachable only via category v1.0.0 is missing", ids.contains(D));
		assertTrue("member reachable only via category v2.0.0 is missing", ids.contains(E));
	}

}
