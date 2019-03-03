/*******************************************************************************
 * Copyright (c) 2012,2018 Red Hat, Inc. and others.
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
 *      Red Hat, Inc. - initial API and implementation
 *      Ericsson AB - ongoing development
 *      Red Hat, Inc. - fragment support
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Properties;
import org.eclipse.equinox.internal.simpleconfigurator.SimpleConfiguratorImpl;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.sharedinstall.AbstractSharedInstallTest;

public class SimpleConfiguratorTest extends AbstractProvisioningTest {

	protected URL relativeURL;
	protected File userConfiguration;
	protected File masterConfguration;
	protected URL[] sharedConfiguration = new URL[2];
	protected URL[] localConfiguration = new URL[1];
	protected SimpleConfiguratorImpl configurator;

	@Override
	public void setUp() throws Exception {
		relativeURL = new URL("file://bundles.info");
		File tmp = getTempFolder();
		final String USER_PATH = "testData/simpleconfigurator/user";
		userConfiguration = new File(tmp, USER_PATH);
		copy("copyUserConfiguration", getTestData("userConfiguration", USER_PATH), userConfiguration);

		final String MASTER_PATH = "testData/simpleconfigurator/master";
		masterConfguration = new File(tmp, MASTER_PATH);
		copy("copymasterConfiguration", getTestData("masterConfiguration", MASTER_PATH), masterConfguration);

		sharedConfiguration[0] = userConfiguration.toURL();

		sharedConfiguration[1] = masterConfguration.toURL();
		localConfiguration[0] = sharedConfiguration[1];
		configurator = getSimpleConfigurator();
	}

	private SimpleConfiguratorImpl getSimpleConfigurator() {
		return new SimpleConfiguratorImpl(null, null);
	}

	protected void storeTimestamp(long timestamp) throws IOException {
		File f = new File(userConfiguration.getParent(), SimpleConfiguratorImpl.BASE_TIMESTAMP_FILE_BUNDLESINFO);
		Properties p = new Properties();
		p.put(SimpleConfiguratorImpl.KEY_BUNDLESINFO_TIMESTAMP, "" + timestamp);
		p.store(new FileOutputStream(f), "");
	}

	@Override
	protected void tearDown() throws Exception {
		System.setProperty(SimpleConfiguratorImpl.PROP_IGNORE_USER_CONFIGURATION, "false");

		super.tearDown();
	}

	protected void assertIsPropertySet(boolean set) {
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

	//master not modified -> extension configured
	//on adding extension master must be selected in order to create new profile with extensions!
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

	/**
	 * Sets the mtime of the given file to zero, optionally record zero to the timestamp file instead of the last modified time.
	 */
	protected void clearLastModified(File file, boolean storeZero) throws IOException {
		long ctime = file.lastModified();
		file.setLastModified(0);
		try {
			FileTime ft = (FileTime) Files.getAttribute(file.toPath(), "unix:ctime");
			ctime = ft.toMillis();
		} catch (UnsupportedOperationException | IllegalArgumentException | IOException e) {
			// Not applicable on non-posix platforms
		}
		if (storeZero) {
			storeTimestamp(0);
		} else {
			storeTimestamp(ctime);
		}
	}

	// master modified, but the mtime of the master config is set to zero --> choose master
	public void testSharedConfigurationMasterModifiedNoMtime() throws IOException {
		if (AbstractSharedInstallTest.WINDOWS) {
			// See bug 540069 and bug 540310. Test below is for posix OS only
			return;
		}
		clearLastModified(masterConfguration, true);
		assertEquals(sharedConfiguration[1], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(true);
	}

	// master not modified, but the mtime of the master config is set to zero --> choose user
	public void testSharedConfigurationMasterUnmodifiedNoMtime() throws IOException {
		if (AbstractSharedInstallTest.WINDOWS) {
			// See bug 540069 and bug 540310. Test below is for posix OS only
			return;
		}
		clearLastModified(masterConfguration, false);
		assertEquals(sharedConfiguration[0], configurator.chooseConfigurationURL(relativeURL, sharedConfiguration));
		assertIsPropertySet(false);
	}
}
