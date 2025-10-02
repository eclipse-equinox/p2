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

import static org.junit.Assert.assertEquals;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.VersionAdvice;
import org.eclipse.equinox.p2.tests.TestData;
import org.junit.Test;

/**
 * Test cases for VersionAdvice
 */
public class VersionAdviceTest {
	@Test
	public void testBadVersionPropertyFile() {
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load(null, "some random string");
	}

	@Test
	public void testLoadNullVersionAdvice() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load(null, versionAdviceFoo);
		assertEquals("Version advice for 'foo' namespace should return version 1.6.4.thisisastring for org.apache.http", 
				Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("Version advice for 'null' namespace should return version 1.3.1 for org.eclipse.cdt", 
				Version.create("1.3.1"), versionAdvice.getVersion("null", "org.eclipse.cdt"));
		assertEquals("Version advice for null namespace should return version 1.4.0 for org.eclipse.tptp", 
				Version.create("1.4.0"), versionAdvice.getVersion(null, "org.eclipse.tptp"));
	}

	@Test
	public void testLoadNullVersionAdvice2() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load("null", versionAdviceFoo);
		assertEquals("Version advice for 'foo' namespace should return version 1.6.4.thisisastring for org.apache.http", 
				Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("Version advice for 'null' namespace should return version 1.3.1 for org.eclipse.cdt", 
				Version.create("1.3.1"), versionAdvice.getVersion("null", "org.eclipse.cdt"));
		assertEquals("Version advice for null namespace should return version 1.4.0 for org.eclipse.tptp", 
				Version.create("1.4.0"), versionAdvice.getVersion(null, "org.eclipse.tptp"));
	}

	@Test
	public void testOverloadNull() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		String versionAdviceBar = TestData.getFile("publisher", "versionadvicebar.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load(null, versionAdviceFoo);
		versionAdvice.load(null, versionAdviceBar);
		assertEquals("Version advice for 'foo' namespace should return version 1.6.4.thisisastring for org.apache.http", 
				Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("Version advice for 'foo' namespace should return version 1.6.4.thisisastring for org.apache.commons", 
				Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.commons"));
		assertEquals("Version advice for 'null' namespace should return overridden version 2.3.1 for org.eclipse.cdt", 
				Version.create("2.3.1"), versionAdvice.getVersion("null", "org.eclipse.cdt"));
		assertEquals("Version advice for null namespace should return overridden version 1.5.0 for org.eclipse.tptp", 
				Version.create("1.5.0"), versionAdvice.getVersion(null, "org.eclipse.tptp"));
	}

	@Test
	public void testLoadVersionAdviceFoo() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load("foo", versionAdviceFoo);
		assertEquals("Version advice loaded for 'foo' namespace should return version 1.6.4.thisisastring for org.apache.http", 
				Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("Version advice loaded for 'foo' namespace should return version 1.3.1 for org.eclipse.cdt", 
				Version.create("1.3.1"), versionAdvice.getVersion("foo", "org.eclipse.cdt"));
		assertEquals("Version advice loaded for 'foo' namespace should return version 1.4.0 for org.eclipse.tptp", 
				Version.create("1.4.0"), versionAdvice.getVersion("foo", "org.eclipse.tptp"));
		assertEquals("Version advice should return null for unloaded null namespace with org.eclipse.tptp", 
				null, versionAdvice.getVersion(null, "org.eclipse.tptp"));
	}

	@Test
	public void testLoadVersionAdviceBar() throws Exception {
		String versionAdviceBar = TestData.getFile("publisher", "versionadvicebar.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load("bar", versionAdviceBar);
		assertEquals("Version advice loaded for 'bar' namespace should return null for org.apache.http", 
				null, versionAdvice.getVersion("bar", "org.apache.http"));
		assertEquals("Version advice loaded for 'bar' namespace should return version 1.6.4.thisisastring for org.apache.commons", 
				Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("bar", "org.apache.commons"));
		assertEquals("Version advice loaded for 'bar' namespace should return version 2.3.1 for org.eclipse.cdt", 
				Version.create("2.3.1"), versionAdvice.getVersion("bar", "org.eclipse.cdt"));
		assertEquals("Version advice loaded for 'bar' namespace should return version 1.5.0 for org.eclipse.tptp", 
				Version.create("1.5.0"), versionAdvice.getVersion("bar", "org.eclipse.tptp"));
	}

	@Test
	public void testLoadVersionAdviceFooBar() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		String versionAdviceBar = TestData.getFile("publisher", "versionadvicebar.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load("foo", versionAdviceFoo);
		versionAdvice.load("bar", versionAdviceBar);
		assertEquals("Version advice for 'foo' namespace should return version 1.6.4.thisisastring for org.apache.http", 
				Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("Version advice for 'foo' namespace should return version 1.3.1 for org.eclipse.cdt", 
				Version.create("1.3.1"), versionAdvice.getVersion("foo", "org.eclipse.cdt"));
		assertEquals("Version advice for 'foo' namespace should return version 1.4.0 for org.eclipse.tptp", 
				Version.create("1.4.0"), versionAdvice.getVersion("foo", "org.eclipse.tptp"));

		assertEquals("Version advice for 'bar' namespace should return null for org.apache.http", 
				null, versionAdvice.getVersion("bar", "org.apache.http"));
		assertEquals("Version advice for 'bar' namespace should return version 1.6.4.thisisastring for org.apache.commons", 
				Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("bar", "org.apache.commons"));
		assertEquals("Version advice for 'bar' namespace should return version 2.3.1 for org.eclipse.cdt", 
				Version.create("2.3.1"), versionAdvice.getVersion("bar", "org.eclipse.cdt"));
		assertEquals("Version advice for 'bar' namespace should return version 1.5.0 for org.eclipse.tptp", 
				Version.create("1.5.0"), versionAdvice.getVersion("bar", "org.eclipse.tptp"));
	}
}
