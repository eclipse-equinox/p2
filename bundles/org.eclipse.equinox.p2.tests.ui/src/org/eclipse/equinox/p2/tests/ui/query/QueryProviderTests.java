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
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

public class QueryProviderTests extends AbstractProvisioningUITest {
	IInstallableUnit nestedCategory;
	IInstallableUnit a, b, c;
	static final String CAT = "Category";
	static final String NESTED = "NestedCategory";
	static final String A = "A";
	static final String B = "B";
	static final String C = "C";
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
		nestedCategory = createIU(NESTED, Version.create("1.0.0"),
				createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, A), categoryProperties, true);
		a = createIU(A, Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, B),
				groupProperties, true);
		b = createIU(B, Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, C),
				groupProperties, true);
		c = createIU(C, Version.create("1.0.0"), NO_REQUIRES, NO_PROPERTIES, true);
		testRepo = createTestMetdataRepository(new IInstallableUnit[] { cat, nestedCategory, a, b, c });
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

}
