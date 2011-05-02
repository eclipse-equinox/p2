/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.frameworkadmin.equinox.ParserUtils;

/**
 * Tests for {@link ParserUtils}.
 */
public class ParserUtilsTest extends AbstractFwkAdminTest {

	public ParserUtilsTest(String name) {
		super(name);
	}
	public void testGetValueForArgument() throws Exception {
		List args = new ArrayList();
		args.add("-foo");
		args.add("bar");
		assertEquals( "bar", ParserUtils.getValueForArgument("-foo", args));
		
		args.set(1, "-bar");
		assertEquals(null, ParserUtils.getValueForArgument("-foo", args));
	}
	
	public void testRemoveArgument() throws Exception {
		String [] args = new String [] { "-bar", "-foo", "-other"};
		ParserUtils.removeArgument("-foo", Arrays.asList(args));
		assertEquals(args, new String [] {"-bar", null, "-other"});
		
		args = new String [] { "-bar", "-foo", "other"};
		ParserUtils.removeArgument("-foo", Arrays.asList(args));
		assertEquals(args, new String [] {"-bar", null, null});
		
		args = new String [] { "-bar", "-foo", "s-pecial"};
		ParserUtils.removeArgument("-foo", Arrays.asList(args));
		assertEquals(args, new String [] {"-bar", null, null});
	}
	
	public void testSetValueForArgument() throws Exception {
		List args = new ArrayList();
		ParserUtils.setValueForArgument("-foo", "bar", args);
		assertTrue(args.size() == 2);
		assertEquals(args.get(0), "-foo");
		assertEquals(args.get(1), "bar");
		
		args.add("-other");
		args.set(1, "s-pecial");
		ParserUtils.setValueForArgument("-foo", "bas", args);
		assertTrue(args.size() == 3);
		assertEquals(args.get(0), "-foo");
		assertEquals(args.get(1), "bas");
		assertEquals(args.get(2), "-other");
		
		args.remove(1);
		ParserUtils.setValueForArgument("-foo", "bas", args);
		assertTrue(args.size() == 3);
		assertEquals(args.get(0), "-foo");
		assertEquals(args.get(1), "bas");
		assertEquals(args.get(2), "-other");
	}
	
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
