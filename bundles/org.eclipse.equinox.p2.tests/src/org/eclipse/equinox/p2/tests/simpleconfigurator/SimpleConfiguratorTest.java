/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.equinox.internal.simpleconfigurator.SimpleConfiguratorImpl;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleConfiguratorTest extends AbstractProvisioningTest {

	private URL relativeURL;
	private File userConfiguration;
	private File masterConfguration;
	private URL[] sharedConfiguration = new URL[2];
	private URL[] localConfiguration = new URL[1];
	private SimpleConfiguratorImpl configurator;

	public void setUp() throws Exception {
		relativeURL = new URL("file://bundles.info");
		userConfiguration = getTestData("userConfiguration", "testData/simpleconfigurator/user");
		sharedConfiguration[0] = userConfiguration.toURL();
		masterConfguration = getTestData("userConfiguration", "testData/simpleconfigurator/master");
		sharedConfiguration[1] = masterConfguration.toURL();
		localConfiguration[0] = sharedConfiguration[1];
		configurator = getSimpleConfigurator();
	}

	private SimpleConfiguratorImpl getSimpleConfigurator() {
		return new SimpleConfiguratorImpl(null, null);
	}

	private void storeTimestamp(long timestamp) throws IOException {
		File f = new File(userConfiguration.getParent(), SimpleConfiguratorImpl.BASE_TIMESTAMP_FILE_BUNDLESINFO);
		Properties p = new Properties();
		p.put(SimpleConfiguratorImpl.KEY_BUNDLESINFO_TIMESTAMP, "" + timestamp);
		p.store(new FileWriter(f), "");
	}

	@Override
	protected void tearDown() throws Exception {
		System.setProperty(SimpleConfiguratorImpl.PROP_IGNORE_USER_CONFIGURATION, "false");
		File f = new File(userConfiguration.getParent(), SimpleConfiguratorImpl.BASE_TIMESTAMP_FILE_BUNDLESINFO);
		if (f.exists()) {
			f.delete();
		}
		super.tearDown();
	}

	private void assertIsPropertySet(boolean set) {
		assertEquals(set, Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty(SimpleConfiguratorImpl.PROP_IGNORE_USER_CONFIGURATION)));
	}

	public void testSimpleConfiguration() throws MalformedURLException {
		assertEquals(localConfiguration[0], configurator.chooseConfigurationURL(relativeURL, localConfiguration));
		assertIsPropertySet(false);
	}

	public void testNotExistingConfigiration() throws MalformedURLException {
		assertNull(configurator.chooseConfigurationURL(relativeURL, new URL[] {new File(".", "notexisting").toURL()}));
		assertIsPropertySet(false);
	}

	public void testSharedConfigurationUserNotExisting() throws MalformedURLException {
		sharedConfiguration[0] = new File(".", "notexisting").toURL();
		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(false);
	}

	// no timestamp -> pick user
	public void testSharedConfigurationNoTimestamp() throws MalformedURLException {
		assertEquals(sharedConfiguration[0], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(false);
	}

	//master modified -> pick master
	public void testSharedConfigurationMasterModified() throws IOException {
		storeTimestamp(1000);
		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(true);
	}

	//master not modified -> pick user
	public void testSharedConfigurationMasterUnmodified() throws IOException {
		storeTimestamp(new File(masterConfguration, relativeURL.getFile()).lastModified());
		assertEquals(sharedConfiguration[0], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(false);
	}

	//master not modified, but property present -> pick master
	public void testSharedConfigurationMasterUnmodifiedPropertySet() throws IOException {
		System.setProperty(SimpleConfiguratorImpl.PROP_IGNORE_USER_CONFIGURATION, "true");
		storeTimestamp(new File(masterConfguration, relativeURL.getFile()).lastModified());
		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(true);
	}

}
