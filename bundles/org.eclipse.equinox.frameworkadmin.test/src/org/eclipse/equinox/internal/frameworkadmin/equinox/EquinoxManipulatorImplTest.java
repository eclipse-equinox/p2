/********************************************************************************
 * Copyright (c) 2020, 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;

public class EquinoxManipulatorImplTest {

	@Test
	public void testToFile() throws MalformedURLException {
		String filename = "test.txt";

		String unixUrl = "file:/program files/eclipse/";
		String expectedUnixFolder = "/program files/eclipse";
		String expectedUnixFile = expectedUnixFolder + "/" + filename;
		assertEquals(new File(expectedUnixFolder), EquinoxManipulatorImpl.toFile(new URL(unixUrl)));
		assertEquals(new File(expectedUnixFile), EquinoxManipulatorImpl.toFile(new URL(unixUrl + filename)));

		String windowsUrl = "file:/C:/Program Files/eclipse/";
		String expectedWindowsFolder = "C:\\Program Files\\eclipse";
		String expectedWindowsFile = expectedWindowsFolder + "\\" + filename;
		assertEquals(new File(expectedWindowsFolder), EquinoxManipulatorImpl.toFile(new URL(windowsUrl)));
		assertEquals(new File(expectedWindowsFile), EquinoxManipulatorImpl.toFile(new URL(windowsUrl + filename)));

		String uncUrl = "file://server/share/folder/";
		String expectedUncFolder = "//server/share/folder";
		String expectedUncFile = expectedUncFolder + "/" + filename;
		assertEquals(new File(expectedUncFolder), EquinoxManipulatorImpl.toFile(new URL(uncUrl)));
		assertEquals(new File(expectedUncFile), EquinoxManipulatorImpl.toFile(new URL(uncUrl + filename)));

		String notFileUrl = "http://example.com/path/to/folder/";
		String expectedOtherFolder = "/path/to/folder";
		String expectedOtherFile = expectedOtherFolder + "/" + filename;
		assertEquals(new File(expectedOtherFolder), EquinoxManipulatorImpl.toFile(new URL(notFileUrl)));
		assertEquals(new File(expectedOtherFile), EquinoxManipulatorImpl.toFile(new URL(notFileUrl + filename)));
	}

}
