/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.metadata.query.UpdateQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class UpdateQueryTest extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit updateOfA;
	private IInstallableUnit a11;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		a1 = createIU("A", Version.create("2.0.0"));
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor("A", new VersionRange("[2.0.0, 2.0.0]"), 0, "update description");
		updateOfA = createIU("UpdateA", Version.createOSGi(1, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		a11 = createIUUpdate();
	}

	public void testUpdateWithDifferentId() {
		IMetadataRepository repo = createTestMetdataRepository(new IInstallableUnit[] {a1, updateOfA});
		IQueryResult c = repo.query(new UpdateQuery(a1), null);
		assertEquals(1, queryResultSize(c));
		assertEquals(updateOfA, c.iterator().next());
	}

	public void testWithSuperiorVersion() {
		IMetadataRepository repo2 = createTestMetdataRepository(new IInstallableUnit[] {a11, a1});
		IQueryResult c2 = repo2.query(new UpdateQuery(a1), null);
		assertEquals(1, queryResultSize(c2));
		assertEquals(a11, c2.iterator().next());
	}

	public void testUpdateWithSameId() {

	}

	private IInstallableUnit createIUUpdate() {
		return createIU("A", Version.create("2.1.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, MetadataFactory.createUpdateDescriptor("A", new VersionRange("[2.0.0, 2.1.0]"), 0, "update description"), null);
	}
}
