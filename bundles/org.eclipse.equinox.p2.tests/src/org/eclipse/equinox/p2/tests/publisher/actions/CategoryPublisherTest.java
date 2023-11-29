/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import org.eclipse.equinox.internal.p2.updatesite.CategoryPublisherApplication;
import org.eclipse.equinox.p2.publisher.AbstractPublisherApplication;
import org.eclipse.equinox.p2.tests.*;

public class CategoryPublisherTest extends AbstractProvisioningTest {

	/**
	 * runs default director app.
	 */
	protected StringBuffer runPublisherApp(AbstractPublisherApplication application, final String[] args) throws Exception {
		PrintStream out = System.out;
		StringBuffer buffer = new StringBuffer();
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			application.run(args);
		} finally {
			System.setOut(out);
		}
		return buffer;
	}

	public void testCompressCategoryRepo() throws Exception {
		File repository = null;
		try {
			repository = getTempFolder();
			URI repositoryURI = repository.toURI();
			URI category1 = TestData.getFile("CategoryPublisherTests", "category1.xml").toURI();
			String[] firstRun = new String[] {"-metadataRepository", repositoryURI.toString(), "-categoryDefinition", category1.toString(), "-categoryQualifier", "foo"};
			String[] secondRun = new String[] {"-metadataRepository", repositoryURI.toString(), "-categoryDefinition", category1.toString(), "-categoryQualifier", "foo", "-compress"};

			CategoryPublisherApplication categoryPublisherApplication = new CategoryPublisherApplication();
			runPublisherApp(categoryPublisherApplication, firstRun);

			categoryPublisherApplication = new CategoryPublisherApplication();
			runPublisherApp(categoryPublisherApplication, secondRun);

			assertContains(repository, "content.jar");

		} finally {
			if (repository != null & repository.exists())
				delete(repository);
		}
	}

	public void testRememberRepoShapeJar() throws Exception {
		File repository = null;
		try {
			repository = getTempFolder();
			URI repositoryURI = repository.toURI();
			URI category1 = TestData.getFile("CategoryPublisherTests", "category1.xml").toURI();
			String[] firstRun = new String[] {"-metadataRepository", repositoryURI.toString(), "-categoryDefinition", category1.toString(), "-categoryQualifier", "foo", "-compress"};
			String[] secondRun = new String[] {"-metadataRepository", repositoryURI.toString(), "-categoryDefinition", category1.toString(), "-categoryQualifier", "foo"};

			CategoryPublisherApplication categoryPublisherApplication = new CategoryPublisherApplication();
			runPublisherApp(categoryPublisherApplication, firstRun);

			categoryPublisherApplication = new CategoryPublisherApplication();
			runPublisherApp(categoryPublisherApplication, secondRun);

			assertContains(repository, "content.jar");

		} finally {
			if (repository != null & repository.exists())
				delete(repository);
		}
	}

	public void testRememberRepoShapeXML() throws Exception {
		File repository = null;
		try {
			repository = getTempFolder();
			URI repositoryURI = repository.toURI();
			URI category1 = TestData.getFile("CategoryPublisherTests", "category1.xml").toURI();
			String[] firstRun = new String[] {"-metadataRepository", repositoryURI.toString(), "-categoryDefinition", category1.toString(), "-categoryQualifier", "foo"};
			String[] secondRun = new String[] {"-metadataRepository", repositoryURI.toString(), "-categoryDefinition", category1.toString(), "-categoryQualifier", "foo"};

			CategoryPublisherApplication categoryPublisherApplication = new CategoryPublisherApplication();
			runPublisherApp(categoryPublisherApplication, firstRun);

			categoryPublisherApplication = new CategoryPublisherApplication();
			runPublisherApp(categoryPublisherApplication, secondRun);

			assertContains(repository, "content.xml");

		} finally {
			if (repository != null & repository.exists())
				delete(repository);
		}
	}

	public void assertContains(File directory, String file) {
		assertNotNull(directory);
		assertTrue(directory.exists());
		assertTrue(directory.isDirectory());
		File[] listFiles = directory.listFiles();
		for (File listFile : listFiles) {
			if (listFile.getName().equals(file)) {
				return;
			}
		}
		fail("Directory does not contain file: " + file);
	}
}
