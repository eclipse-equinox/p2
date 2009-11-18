/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Tests for {@link AnyRequiredCapabilityQuery}.
 */
public class AnyRequiredCapabilityTest extends AbstractQueryTest {
	public void testMatchOtherObjects() {
		IRequiredCapability[] requires = createRequiredCapabilities("org.eclipse.equinox.p2.iu", "test.bundle", null);
		CapabilityQuery query = new CapabilityQuery(requires);
		IInstallableUnit match = createIU("test.bundle");
		IInstallableUnit noMatch = createIU("another.bundle");
		List items = new ArrayList();
		items.add(match);
		items.add(noMatch);
		items.add(new Object());
		items.add(requires);
		Collector result = query.perform(items.iterator(), new Collector());
		assertEquals("1.0", 1, result.size());
		assertEquals("1.1", match, result.iterator().next());
	}

	public void testExistingRepository() {
		URI location;
		try {
			location = TestData.getFile("metadataRepo", "good").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.addRepository(location);
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(ProvisioningUI.getDefaultUI().getSession(), ProvisioningUI.getDefaultUI().getRepositoryTracker(), false);
		IRequiredCapability[] requires = createRequiredCapabilities("org.eclipse.equinox.p2.iu", "test.bundle", null);
		CapabilityQuery query = new CapabilityQuery(requires);
		Collector result = manager.query(query, new Collector(), getMonitor());
		assertEquals("1.0", 1, result.size());
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "test.bundle", iu.getId());
	}

}
