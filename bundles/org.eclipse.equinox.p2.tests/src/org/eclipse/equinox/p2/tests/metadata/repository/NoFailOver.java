/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests that failing to load an invalid p2 repository doesn't result in a legacy
 * Update Site at the same location being loaded. For details see
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=273756.
 */
public class NoFailOver extends AbstractProvisioningTest {
	/**
	 * Tests fail over on an metadata repository
	 */
	public void testMetadataDoesNotOver() {
		File repoLocation = getTestData("fail over", "testData/noFailOver");
		try {
			getMetadataRepositoryManager().loadRepository(repoLocation.toURI(), null);
			fail("The repository should not have been loaded");
		} catch (ProvisionException e) {
			return;
		}
	}

	/**
	 * Tests fail over on an artifact repository
	 */
	public void testArtifactDoesNotOver() {
		File repoLocation = getTestData("fail over", "testData/noFailOver");
		try {
			getArtifactRepositoryManager().loadRepository(repoLocation.toURI(), null);
			fail("The repository should not have been loaded");
		} catch (ProvisionException e) {
			return;
		}
	}
}
