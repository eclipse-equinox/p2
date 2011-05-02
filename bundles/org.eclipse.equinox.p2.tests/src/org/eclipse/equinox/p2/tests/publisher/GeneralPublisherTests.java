/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.publisher.Messages;
import org.eclipse.equinox.internal.p2.publisher.QuotedTokenizer;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAndBundlesPublisherApplication;
import org.eclipse.equinox.p2.tests.StringBufferStream;

public class GeneralPublisherTests extends TestCase {

	public void testBug255820_Product_normalize() throws Exception {
		Method normalizeMethod = ProductFile.class.getDeclaredMethod("normalize", String.class);
		normalizeMethod.setAccessible(true);
		assertNotNull(normalizeMethod);

		assertEquals(normalizeMethod.invoke(null, "a b  c\td\ne"), "a b c d e");
		assertEquals(normalizeMethod.invoke(null, "a\fbd\r\n e"), "a bd e");
	}

	public void testInvalidConfiguration1() {
		FeaturesAndBundlesPublisherApplication application = new FeaturesAndBundlesPublisherApplication();
		Integer retValue = 0;
		StringBuffer buffer = new StringBuffer();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			retValue = (Integer) application.run(new String[0]);
		} catch (Exception e) {
			fail("0.99");
		} finally {
			System.setOut(out);
		}
		assertTrue(buffer.toString().contains("A metadata repository must be specified."));
		assertEquals("1.0", 1, retValue.intValue());
		assertEquals("1.1", Messages.exception_noMetadataRepo, application.getStatus().getMessage());
	}

	public void testInvalidConfiguration2() {
		FeaturesAndBundlesPublisherApplication application = new FeaturesAndBundlesPublisherApplication();
		Integer retValue = 0;
		StringBuffer buffer = new StringBuffer();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			retValue = (Integer) application.run(new String[] {"-metadataRepository foo", "-publishArtifacts"});
		} catch (Exception e) {
			fail("0.99");
		} finally {
			System.setOut(out);
		}
		assertTrue(buffer.toString().contains("An artifact repository must be specified in order to publish artifacts."));
		assertEquals("1.0", 1, retValue.intValue());
		assertEquals("1.1", Messages.exception_noArtifactRepo, application.getStatus().getMessage());

	}

	public void testQuotedTokenizer() throws Exception {
		QuotedTokenizer tokenizer = new QuotedTokenizer("abra ca dabra");
		assertEquals("abra", tokenizer.nextToken());
		assertEquals("ca", tokenizer.nextToken());
		assertTrue(tokenizer.hasMoreTokens());
		assertEquals("dabra", tokenizer.nextToken());
		assertFalse(tokenizer.hasMoreTokens());

		boolean exception = false;
		try {
			tokenizer.nextToken();
		} catch (NoSuchElementException e) {
			exception = true;
		}
		assertTrue(exception);

		tokenizer = new QuotedTokenizer("ab c\"de fg\" hi");
		assertEquals("ab", tokenizer.nextToken());
		assertEquals("cde fg", tokenizer.nextToken());
		assertEquals("hi", tokenizer.nextToken());
		assertFalse(tokenizer.hasMoreTokens());

		tokenizer = new QuotedTokenizer("a,b c,d", ",");
		assertEquals("a", tokenizer.nextToken());
		assertEquals("b c", tokenizer.nextToken());
		assertEquals("d", tokenizer.nextToken());
		assertFalse(tokenizer.hasMoreTokens());

		tokenizer = new QuotedTokenizer("a bcd" + '\u7432' + "e fg");
		assertEquals("a", tokenizer.nextToken());
		assertEquals("bcd" + '\u7432' + "e", tokenizer.nextToken());
		assertEquals("fg", tokenizer.nextToken());
		assertFalse(tokenizer.hasMoreTokens());

		tokenizer = new QuotedTokenizer("    ");
		assertFalse(tokenizer.hasMoreTokens());

		tokenizer = new QuotedTokenizer(",,,", ",");
		assertFalse(tokenizer.hasMoreElements());

		tokenizer = new QuotedTokenizer("a \"b\\\" c\" d");
		assertEquals("a", tokenizer.nextToken());
		assertEquals("b\" c", tokenizer.nextToken());
		assertEquals("d", tokenizer.nextToken());
		assertFalse(tokenizer.hasMoreTokens());
	}
}
