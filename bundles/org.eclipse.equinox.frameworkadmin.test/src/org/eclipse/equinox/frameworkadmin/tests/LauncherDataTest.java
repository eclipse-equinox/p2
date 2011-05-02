/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;

/**
 * @since 1.0
 */
public class LauncherDataTest extends TestCase {

	/*
	 * Constructor for the class.
	 */
	public LauncherDataTest(String name) {
		super(name);
	}

	public void testRemoveProgramArg() {
		LauncherData data = new LauncherData("equinox", "1.0", "eclipse", "1.0");
		data.setProgramArgs(new String[] {"-console", "-startup", "foo"});
		data.removeProgramArg("-startup");
		assertEquals("1.0", new String[] {"-console"}, data.getProgramArgs());

		data.setProgramArgs(null);
		data.setProgramArgs(new String[] {"-console", "-startup", "foo", "-bar"});
		data.removeProgramArg("-startup");
		assertEquals("2.0", new String[] {"-console", "-bar"}, data.getProgramArgs());

		data.setProgramArgs(null);
		data.setProgramArgs(new String[] {"-startup", "foo"});
		data.removeProgramArg("-startup");
		assertEquals("3.0", new String[0], data.getProgramArgs());

		data.setProgramArgs(null);
		data.setProgramArgs(new String[] {"-console", "-startup", "foo", "bar"});
		data.removeProgramArg("-startup");
		assertEquals("4.0", new String[] {"-console"}, data.getProgramArgs());

		data.setProgramArgs(null);
		data.setProgramArgs(new String[] {"-console", "-startup", "foo", "bar", "-xxx"});
		data.removeProgramArg("-startup");
		assertEquals("5.0", new String[] {"-console", "-xxx"}, data.getProgramArgs());

		// arg which doesn't start with a dash - dont' consume anything but that specific arg
		data.setProgramArgs(null);
		data.setProgramArgs(new String[] {"-console", "-startup", "foo", "bar", "-xxx"});
		data.removeProgramArg("foo");
		assertEquals("6.0", new String[] {"-console", "-startup", "foo", "bar", "-xxx"}, data.getProgramArgs());

		// non-matching arg
		data.setProgramArgs(null);
		data.setProgramArgs(new String[] {"-console", "-startup", "foo", "bar", "-xxx"});
		data.removeProgramArg("zzz");
		assertEquals("7.0", new String[] {"-console", "-startup", "foo", "bar", "-xxx"}, data.getProgramArgs());

		// empty string
		data.setProgramArgs(null);
		data.setProgramArgs(new String[] {"-console", "-startup", "foo", "bar", "-xxx"});
		data.removeProgramArg("foo");
		assertEquals("8.0", new String[] {"-console", "-startup", "foo", "bar", "-xxx"}, data.getProgramArgs());

		// just whitespace
		data.setProgramArgs(null);
		data.setProgramArgs(new String[] {"-console", "-startup", "foo", "bar", "-xxx"});
		data.removeProgramArg(" ");
		assertEquals("9.0", new String[] {"-console", "-startup", "foo", "bar", "-xxx"}, data.getProgramArgs());

	}

	/*
	 * Compare the give 2 arrays and assert whether or not they should be considered equal.
	 */
	public static void assertEquals(String message, String[] one, String[] two) {
		if (one == null)
			assertNull(message, two);
		if (two == null)
			fail(message);
		assertEquals(message, one.length, two.length);
		for (int i = 0; i < one.length; i++)
			assertEquals(message, one[i], two[i]);
	}
}
