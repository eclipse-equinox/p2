/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import static org.junit.Assert.assertArrayEquals;

import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.junit.Test;

/**
 * @since 1.0
 */
public class LauncherDataTest {

	@Test
	public void testRemoveProgramArg() {
		LauncherData data = new LauncherData("equinox", "1.0", "eclipse", "1.0");
		data.setProgramArgs(new String[] { "-console", "-startup", "foo" });
		data.removeProgramArg("-startup");
		assertArrayEquals("1.0", new String[] { "-console" }, data.getProgramArgs());

		data.setProgramArgs(null);
		data.setProgramArgs(new String[] { "-console", "-startup", "foo", "-bar" });
		data.removeProgramArg("-startup");
		assertArrayEquals("2.0", new String[] { "-console", "-bar" }, data.getProgramArgs());

		data.setProgramArgs(null);
		data.setProgramArgs(new String[] { "-startup", "foo" });
		data.removeProgramArg("-startup");
		assertArrayEquals("3.0", new String[0], data.getProgramArgs());

		data.setProgramArgs(null);
		data.setProgramArgs(new String[] { "-console", "-startup", "foo", "bar" });
		data.removeProgramArg("-startup");
		assertArrayEquals("4.0", new String[] { "-console" }, data.getProgramArgs());

		data.setProgramArgs(null);
		data.setProgramArgs(new String[] { "-console", "-startup", "foo", "bar", "-xxx" });
		data.removeProgramArg("-startup");
		assertArrayEquals("5.0", new String[] { "-console", "-xxx" }, data.getProgramArgs());

		// arg which doesn't start with a dash - dont' consume anything but that
		// specific arg
		data.setProgramArgs(null);
		data.setProgramArgs(new String[] { "-console", "-startup", "foo", "bar", "-xxx" });
		data.removeProgramArg("foo");
		assertArrayEquals("6.0", new String[] { "-console", "-startup", "foo", "bar", "-xxx" }, data.getProgramArgs());

		// non-matching arg
		data.setProgramArgs(null);
		data.setProgramArgs(new String[] { "-console", "-startup", "foo", "bar", "-xxx" });
		data.removeProgramArg("zzz");
		assertArrayEquals("7.0", new String[] { "-console", "-startup", "foo", "bar", "-xxx" }, data.getProgramArgs());

		// empty string
		data.setProgramArgs(null);
		data.setProgramArgs(new String[] { "-console", "-startup", "foo", "bar", "-xxx" });
		data.removeProgramArg("foo");
		assertArrayEquals("8.0", new String[] { "-console", "-startup", "foo", "bar", "-xxx" }, data.getProgramArgs());

		// just whitespace
		data.setProgramArgs(null);
		data.setProgramArgs(new String[] { "-console", "-startup", "foo", "bar", "-xxx" });
		data.removeProgramArg(" ");
		assertArrayEquals("9.0", new String[] { "-console", "-startup", "foo", "bar", "-xxx" }, data.getProgramArgs());

	}
}
