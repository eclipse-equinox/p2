/*******************************************************************************
 * Copyright (c) 2012,2013 Red Hat, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *      Red Hat, Inc. - initial API and implementation
 *      Ericsson AB - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.eclipse.equinox.internal.simpleconfigurator.Activator;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.eclipse.equinox.p2.tests.sharedinstall.AbstractSharedInstallTest;

public class SimpleConfiguratorTestExtendedConfigured extends SimpleConfiguratorTestExtended {

	private File parentFolder;
	private File ext1Info;
	private File ext1Parent;
	private File ext3Info;
	private File ext3Parent;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		parentFolder = new File(getTempFolder(), "extension");
		ext1Parent = new File(parentFolder, "ext1");
		ext1Parent.mkdirs();
		ext1Info = new File(ext1Parent, "ext1.info");
		ext1Info.createNewFile();
		ext3Parent = new File(parentFolder, "ext3");
		ext3Parent.mkdirs();
		ext3Info = new File(ext3Parent, "ext3.info");
		ext3Info.createNewFile();
		ext1Info.setLastModified(System.currentTimeMillis() + 1000);
		AbstractSharedInstallTest.setReadOnly(ext1Parent, true);
		AbstractSharedInstallTest.reallyReadOnly(ext1Parent);
		ext3Info.setLastModified(System.currentTimeMillis() + 1000);
	}

	@Override
	protected void tearDown() throws Exception {
		Activator.EXTENSIONS = null;
		AbstractSharedInstallTest.removeReallyReadOnly(ext1Parent);
		AbstractSharedInstallTest.setReadOnly(ext1Parent, false);
		super.tearDown();
	}

	public void testWriteableExtension() throws FileNotFoundException, IOException, URISyntaxException {
		Activator.EXTENSIONS = parentFolder.toString();
		ArrayList<File> infoFiles = SimpleConfiguratorUtils.getInfoFiles();
		assertEquals("only read-only info file should be considered", 1, infoFiles.size());
		// ext1 is expected because ext3 is writeable
		assertEquals(ext1Info.getName(), infoFiles.get(0).getName());
	}

	public void testExtensionAdded() throws IOException {

		storeTimestamp(new File(masterConfguration, relativeURL.getFile()).lastModified());
		assertEquals(sharedConfiguration[0], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(false);

		Activator.EXTENSIONS = parentFolder.toString();

		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(true);
	}

	public void testExtensionRemoved() throws IOException {

		Activator.EXTENSIONS = parentFolder.toString();
		storeTimestamp(ext1Info.lastModified());
		//on adding extension master must be selected in order to create new profile with extensions!
		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(true);

		//disable extension
		Activator.EXTENSIONS = null;

		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(true);
	}

	public void testExtensionModified() throws IOException {

		Activator.EXTENSIONS = parentFolder.toString();
		storeTimestamp(ext1Info.lastModified());
		//on adding extension master must be selected in order to create new profile with extensions!
		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(true);

		//add new extension
		File ext2Dir = new File(parentFolder, "ext2");
		ext2Dir.mkdirs();
		File file = new File(ext2Dir, "ext2.info");
		file.createNewFile();
		file.setLastModified(parentFolder.lastModified() + 3000);

		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(true);
	}
}
