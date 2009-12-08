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
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class AnyRequiredCapabilityTest extends AbstractQueryTest {
	public void testMatchOtherObjects() {
		IRequirement requires = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.iu", "test.bundle", ANY_VERSION, null, false, false);
		IInstallableUnit match = createIU("test.bundle");
		IInstallableUnit noMatch = createIU("another.bundle");
		List items = new ArrayList();
		items.add(match);
		items.add(noMatch);
		items.add(new Object());
		items.add(requires);
		Collector result = requires.getMatches().perform(items.iterator(), new Collector());
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
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(ProvisioningUI.getDefaultUI(), false);
		IRequirement requires = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.iu", "test.bundle", ANY_VERSION, null, false, false);
		Collector result = manager.query(requires.getMatches(), getMonitor());
		assertEquals("1.0", 1, result.size());
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertEquals("1.1", "test.bundle", iu.getId());
	}

}
