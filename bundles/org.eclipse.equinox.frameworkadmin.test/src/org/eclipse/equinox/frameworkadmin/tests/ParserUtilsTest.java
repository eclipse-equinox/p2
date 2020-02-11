/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.frameworkadmin.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.frameworkadmin.equinox.ParserUtils;
import org.junit.Test;

/**
 * Tests for {@link ParserUtils}.
 */
public class ParserUtilsTest extends AbstractFwkAdminTest {

	@Test
	public void testGetValueForArgument() throws Exception {
		List<String> args = new ArrayList<>();
		args.add("-foo");
		args.add("bar");
		assertEquals( "bar", ParserUtils.getValueForArgument("-foo", args));

		args.set(1, "-bar");
		assertEquals(null, ParserUtils.getValueForArgument("-foo", args));
	}
	@Test
	public void testRemoveArgument() throws Exception {
		String [] args = new String [] { "-bar", "-foo", "-other"};
		ParserUtils.removeArgument("-foo", Arrays.asList(args));
		assertArrayEquals(args, new String [] {"-bar", null, "-other"});

		args = new String [] { "-bar", "-foo", "other"};
		ParserUtils.removeArgument("-foo", Arrays.asList(args));
		assertArrayEquals(args, new String [] {"-bar", null, null});

		args = new String [] { "-bar", "-foo", "s-pecial"};
		ParserUtils.removeArgument("-foo", Arrays.asList(args));
		assertArrayEquals(args, new String [] {"-bar", null, null});
	}
	@Test
	public void testSetValueForArgument() throws Exception {
		List<String> args = new ArrayList<>();
		ParserUtils.setValueForArgument("-foo", "bar", args);
		assertEquals(2, args.size());
		assertEquals(args.get(0), "-foo");
		assertEquals(args.get(1), "bar");

		args.add("-other");
		args.set(1, "s-pecial");
		ParserUtils.setValueForArgument("-foo", "bas", args);
		assertEquals(3, args.size());
		assertEquals(args.get(0), "-foo");
		assertEquals(args.get(1), "bas");
		assertEquals(args.get(2), "-other");

		args.remove(1);
		ParserUtils.setValueForArgument("-foo", "bas", args);
		assertEquals(3, args.size());
		assertEquals(args.get(0), "-foo");
		assertEquals(args.get(1), "bas");
		assertEquals(args.get(2), "-other");
	}
	@Test
	public void testFromOSGiJarToOSGiInstallArea() {
		String path = "";
		File result =ParserUtils.fromOSGiJarToOSGiInstallArea(path);
		assertNotNull("1.0", result);

		path = "osgi.jar";
		result =ParserUtils.fromOSGiJarToOSGiInstallArea(path);
		assertNotNull("1.0", result);
		assertEquals("1.1", "", result.toString());
	}

}
