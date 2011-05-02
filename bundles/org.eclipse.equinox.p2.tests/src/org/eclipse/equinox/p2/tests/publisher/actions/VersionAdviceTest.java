/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import junit.framework.TestCase;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.VersionAdvice;
import org.eclipse.equinox.p2.tests.TestData;

/**
 * Test cases for VersionAdvice
 */
public class VersionAdviceTest extends TestCase {

	public void testBadVersionPropertyFile() {
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load(null, "some random string");
	}

	public void testLoadNullVersionAdvice() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load(null, versionAdviceFoo);
		assertEquals("1.0", Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("2.0", Version.create("1.3.1"), versionAdvice.getVersion("null", "org.eclipse.cdt"));
		assertEquals("3.0", Version.create("1.4.0"), versionAdvice.getVersion(null, "org.eclipse.tptp"));
	}

	public void testLoadNullVersionAdvice2() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load("null", versionAdviceFoo);
		assertEquals("1.0", Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("2.0", Version.create("1.3.1"), versionAdvice.getVersion("null", "org.eclipse.cdt"));
		assertEquals("3.0", Version.create("1.4.0"), versionAdvice.getVersion(null, "org.eclipse.tptp"));
	}

	public void testOverloadNull() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		String versionAdviceBar = TestData.getFile("publisher", "versionadvicebar.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load(null, versionAdviceFoo);
		versionAdvice.load(null, versionAdviceBar);
		assertEquals("1.0", Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("1.0", Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.commons"));
		assertEquals("2.0", Version.create("2.3.1"), versionAdvice.getVersion("null", "org.eclipse.cdt"));
		assertEquals("3.0", Version.create("1.5.0"), versionAdvice.getVersion(null, "org.eclipse.tptp"));
	}

	public void testLoadVersionAdviceFoo() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load("foo", versionAdviceFoo);
		assertEquals("1.0", Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("2.0", Version.create("1.3.1"), versionAdvice.getVersion("foo", "org.eclipse.cdt"));
		assertEquals("3.0", Version.create("1.4.0"), versionAdvice.getVersion("foo", "org.eclipse.tptp"));
		assertEquals("4.0", null, versionAdvice.getVersion(null, "org.eclipse.tptp"));
	}

	public void testLoadVersionAdviceBar() throws Exception {
		String versionAdviceBar = TestData.getFile("publisher", "versionadvicebar.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load("bar", versionAdviceBar);
		assertEquals("1.0", null, versionAdvice.getVersion("bar", "org.apache.http"));
		assertEquals("2.0", Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("bar", "org.apache.commons"));
		assertEquals("3.0", Version.create("2.3.1"), versionAdvice.getVersion("bar", "org.eclipse.cdt"));
		assertEquals("4.0", Version.create("1.5.0"), versionAdvice.getVersion("bar", "org.eclipse.tptp"));
	}

	public void testLoadVersionAdviceFooBar() throws Exception {
		String versionAdviceFoo = TestData.getFile("publisher", "versionadvicefoo.prop").toString();
		String versionAdviceBar = TestData.getFile("publisher", "versionadvicebar.prop").toString();
		VersionAdvice versionAdvice = new VersionAdvice();
		versionAdvice.load("foo", versionAdviceFoo);
		versionAdvice.load("bar", versionAdviceBar);
		assertEquals("1.0", Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("foo", "org.apache.http"));
		assertEquals("2.0", Version.create("1.3.1"), versionAdvice.getVersion("foo", "org.eclipse.cdt"));
		assertEquals("3.0", Version.create("1.4.0"), versionAdvice.getVersion("foo", "org.eclipse.tptp"));

		assertEquals("4.0", null, versionAdvice.getVersion("bar", "org.apache.http"));
		assertEquals("5.0", Version.create("1.6.4.thisisastring"), versionAdvice.getVersion("bar", "org.apache.commons"));
		assertEquals("6.0", Version.create("2.3.1"), versionAdvice.getVersion("bar", "org.eclipse.cdt"));
		assertEquals("7.0", Version.create("1.5.0"), versionAdvice.getVersion("bar", "org.eclipse.tptp"));
	}
}
