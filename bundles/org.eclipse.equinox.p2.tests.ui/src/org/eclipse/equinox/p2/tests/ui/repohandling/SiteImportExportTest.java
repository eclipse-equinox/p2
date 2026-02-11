/*******************************************************************************
 * Copyright (c) 2009, 2026 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.ui.repohandling;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.internal.p2.ui.UpdateManagerCompatibility;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

public class SiteImportExportTest extends AbstractProvisioningUITest {
	public void testUpdateManagerImport() throws IOException {
		int siteCount;
		File bookmarks = getTestData("Getting site bookmarks", "/testData/siteBookmarks/siteexport33.xml/");
		MetadataRepositoryElement[] elements = UpdateManagerCompatibility.readBookmarkFile(bookmarks);
		siteCount = elements.length;
		assertNotNull(elements);
		assertTrue(siteCount > 0);
		MetadataRepositoryElement element = elements[0];
		element.setNickname("Foo");

		File folder = getTempFolder();
		File testExport = new File(folder, "testExport.xml");
		UpdateManagerCompatibility.writeBookmarkFile(testExport.getAbsolutePath(), elements);

		elements = UpdateManagerCompatibility.readBookmarkFile(testExport);
		assertEquals(siteCount, elements.length);
		assertEquals(elements[0].getName(), "Foo");
	}
}
