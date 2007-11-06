/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.apache.tools.bzip2.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

public class CBZip2OutputStreamTest extends TestCase {

	/**
	 * @throws IOException
	 */
	public void testEmtpyOutputShouldNotFail() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		CBZip2OutputStream outputStream = new CBZip2OutputStream(
				byteArrayOutputStream);
		outputStream.close();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				byteArrayOutputStream.toByteArray());

		CBZip2InputStream inputStream = new CBZip2InputStream(
				byteArrayInputStream);
		assertEquals(-1, inputStream.read());
	}

	/**
	 * @throws IOException
	 */
	public void testOneByteOutputAndOneByteIn() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		CBZip2OutputStream outputStream = new CBZip2OutputStream(
				byteArrayOutputStream);
		outputStream.write(42);
		outputStream.close();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				byteArrayOutputStream.toByteArray());

		CBZip2InputStream inputStream = new CBZip2InputStream(
				byteArrayInputStream);
		assertEquals(42, inputStream.read());
		assertEquals(-1, inputStream.read());
	}

	/**
	 * @throws IOException
	 */
	public void testWriteFinishAndClose() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		CBZip2OutputStream outputStream = new CBZip2OutputStream(
				byteArrayOutputStream);
		outputStream.flush();
		int len = byteArrayOutputStream.toByteArray().length;

		outputStream.write(42);
		outputStream.finish();

		int lenAfterWrite = byteArrayOutputStream.toByteArray().length;
		assertTrue(len != lenAfterWrite);

		outputStream.close();
		int lenAfterClose = byteArrayOutputStream.toByteArray().length;
		assertTrue(lenAfterWrite == lenAfterClose);

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				byteArrayOutputStream.toByteArray());

		CBZip2InputStream inputStream = new CBZip2InputStream(
				byteArrayInputStream);
		assertEquals(42, inputStream.read());
		assertEquals(-1, inputStream.read());
	}

	/**
	 * @throws IOException
	 */
	public void testWriteFinishFinishAndClose() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		CBZip2OutputStream outputStream = new CBZip2OutputStream(
				byteArrayOutputStream);
		outputStream.flush();
		int len = byteArrayOutputStream.toByteArray().length;

		outputStream.write(42);
		outputStream.finish();
		outputStream.finish();

		int lenAfterWrite = byteArrayOutputStream.toByteArray().length;
		assertTrue(len != lenAfterWrite);

		outputStream.close();
		int lenAfterClose = byteArrayOutputStream.toByteArray().length;
		assertTrue(lenAfterWrite == lenAfterClose);

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				byteArrayOutputStream.toByteArray());

		CBZip2InputStream inputStream = new CBZip2InputStream(
				byteArrayInputStream);
		assertEquals(42, inputStream.read());
		assertEquals(-1, inputStream.read());
	}

	/**
	 * @throws IOException
	 */
	public void testWriteFinishAndCloseClose() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		CBZip2OutputStream outputStream = new CBZip2OutputStream(
				byteArrayOutputStream);
		outputStream.flush();
		int len = byteArrayOutputStream.toByteArray().length;

		outputStream.write(42);
		outputStream.finish();

		int lenAfterWrite = byteArrayOutputStream.toByteArray().length;
		assertTrue(len != lenAfterWrite);

		outputStream.close();
		int lenAfterClose1 = byteArrayOutputStream.toByteArray().length;
		assertTrue(lenAfterWrite == lenAfterClose1);
		outputStream.close();
		int lenAfterClose2 = byteArrayOutputStream.toByteArray().length;
		assertTrue(lenAfterWrite == lenAfterClose2);

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				byteArrayOutputStream.toByteArray());

		CBZip2InputStream inputStream = new CBZip2InputStream(
				byteArrayInputStream);
		assertEquals(42, inputStream.read());
		assertEquals(-1, inputStream.read());
	}

	/**
	 * @throws IOException
	 */
	public void testWriteFinishWriteClose() throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		CBZip2OutputStream outputStream = new CBZip2OutputStream(
				byteArrayOutputStream);
		outputStream.flush();
		int len = byteArrayOutputStream.toByteArray().length;

		outputStream.write(42);
		outputStream.finish();
		outputStream.write(21);
		outputStream.finish();

		int lenAfterWrite = byteArrayOutputStream.toByteArray().length;
		assertTrue(len != lenAfterWrite);

		outputStream.close();
		int lenAfterClose = byteArrayOutputStream.toByteArray().length;
		assertTrue(lenAfterWrite == lenAfterClose);

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				byteArrayOutputStream.toByteArray());

		CBZip2InputStream inputStream = new CBZip2InputStream(
				byteArrayInputStream);
		assertEquals(42, inputStream.read());
		assertEquals(-1, inputStream.read());
	}

}
