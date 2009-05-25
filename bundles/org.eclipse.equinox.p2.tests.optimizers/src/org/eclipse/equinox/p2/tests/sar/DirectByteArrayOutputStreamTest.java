/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.sar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.sar.DirectByteArrayOutputStream;

/**
 * Test the <code>DirectByteArrayOutputStream</code>
 */
public class DirectByteArrayOutputStreamTest extends TestCase {

	private static final String JUST11BYTES = "just11bytes";
	private static final int ELEVEN = JUST11BYTES.getBytes().length;

	/**
	 * Test the constraints of the DBAOS
	 * 
	 * @throws IOException
	 */
	public void testDBAOSConstraints() throws IOException {
		DirectByteArrayOutputStream out = new DirectByteArrayOutputStream(1024);
		out.write(JUST11BYTES.getBytes());
		assertEquals(ELEVEN, out.getBufferLength());
		assertEquals(ELEVEN, out.toByteArray().length);
		assertNotSame(out.toByteArray(), out.getBuffer());
		assertEquals(1024, out.getBuffer().length);
		assertEquals(JUST11BYTES, new String(out.getBuffer(), 0, out.getBufferLength()));
		ByteArrayInputStream in = out.getInputStream();
		assertEquals(ELEVEN, in.available());
		byte[] elevenBytes = new byte[ELEVEN];
		in.read(elevenBytes);
		assertTrue(Arrays.equals(JUST11BYTES.getBytes(), elevenBytes));
		assertEquals(-1, in.read());
	}
}
