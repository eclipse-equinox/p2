package org.eclipse.equinox.frameworkadmin.tests;
/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import junit.framework.TestCase;

public class UtilsTest extends TestCase {
//
//	/**
//	 * @param name
//	 */
//	public UtilsTest(String name) {
//		super(name);
//	}
//
//	/* (non-Javadoc)
//	 * @see junit.framework.TestCase#setUp()
//	 */
//	protected void setUp() throws Exception {
//		super.setUp();
//	}
//
//	/* (non-Javadoc)
//	 * @see junit.framework.TestCase#tearDown()
//	 */
//	protected void tearDown() throws Exception {
//		super.tearDown();
//	}
//
//	//	/**
//	//	 * Test method for {@link org.eclipse.configMan.internal.util.Utils#getUrl(java.lang.String, java.lang.String, java.lang.String)}.
//	//	 */
//	//	public void testGetUrl() {
//	//		fail("Not yet implemented");
//	//	}
//
//	/**
//	 * Test method for {@link Utils#getRelativePath(java.net.URL, java.net.URL)}.
//	 */
//	public void testGetRelativePath() {
//		//URL target;
//		//URL from;
//		try {
//			URL target = new URL("http", "www.ntt.co.jp", "dir1/dir2/target.html");
//			URL from = new URL("http", "www.ntt.co.jp", "dir1/dir3/dir4/from.html");
//			String expected = "../../../dir2/target.html";
//			String ret = Utils.getRelativePath(target, from);
//			assertEquals(expected, ret);
//
//			expected = "../../dir3/dir4/from.html";
//			ret = Utils.getRelativePath(from, target);
//			assertEquals(expected, ret);
//
//			try {
//				target = new URL("http", "www.ntt.co.jp", "dir1/dir2/target.html");
//				from = new URL("http", "www.ibm.com", "dir1/dir3/dir4/from.html");
//				ret = Utils.getRelativePath(target, from);
//				fail("IllegalArgumentException must be thrown");
//			} catch (IllegalArgumentException e) {
//
//			}
//			try {
//				target = new URL("file", null, "dir2/target.html");
//				from = new URL("http", "www.ntt.co.jp", "dir1/dir3/dir4/from.html");
//				ret = Utils.getRelativePath(target, from);
//				fail("IllegalArgumentException must be thrown");
//			} catch (IllegalArgumentException e) {
//
//			}
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//
//		}
//
//	}
//
//	/**
//	 * Test method for {@link Utils#replaceAll(java.lang.String, java.lang.String, java.lang.String)}.
//	 */
//	public void testReplaceAll() {
//		String st = "tere/eerere//ty/d";
//		String expected = "tere\\eerere\\\\ty\\d";
//		String oldSt = "/";
//		String newSt = "\\";
//		String ret = Utils.replaceAll(st, oldSt, newSt);
//		assertEquals(expected, ret);
//	}
//
//	/**
//	 * Test method for {@link Utils#getTokens(java.lang.String, java.lang.String)}.
//	 */
//	public void testGetTokens() {
//		String st = "/AAAA/BB//CC/D/";
//		String[] expected = {"AAAA", "BB", "CC", "D"};
//		String delim = "/";
//		String[] ret = Utils.getTokens(st, delim);
//		assertEquals("lengths must equal.", ret.length, expected.length);
//		for (int i = 0; i < ret.length; i++)
//			assertEquals("each elements must equal.", expected[i], ret[i]);
//	}
//
//	/**
//	 * Test method for {@link Utils#removeLastCh(String target, char ch)}.
//	 */
//	public void testRemoveLastCh() {
//		String target = "ddddaaaaaaaa";
//		String expected = "dddd";
//		char ch = 'a';
//		String ret = Utils.removeLastCh(target, ch);
//		assertEquals(expected, ret);
//	}
}
