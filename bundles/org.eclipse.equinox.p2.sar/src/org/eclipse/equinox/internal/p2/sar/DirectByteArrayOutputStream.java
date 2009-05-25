/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *  IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.sar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * The DirectByteArrayOutputStream discloses its guts (internal byte buffer and
 * byte buffer length) to avoid unnecessary allocation of byte arrays usually
 * involved with toByteArray().
 */
public class DirectByteArrayOutputStream extends ByteArrayOutputStream {

	/**
	 * Creates a new direct byte array output stream. The buffer capacity is
	 * initially as defined by super class.
	 */
	public DirectByteArrayOutputStream() {
		super();
	}

	/**
	 * Creates a new byte array output stream, with a buffer capacity of the
	 * specified size, in bytes.
	 * 
	 * @param size
	 *            the initial size.
	 * @throws IllegalArgumentException
	 *             if size is negative.
	 */
	public DirectByteArrayOutputStream(int size) {
		super(size);
	}

	/**
	 * Return the actual internal byte buffer.
	 * 
	 * @return internal byte buffer
	 */
	public final byte[] getBuffer() {
		return super.buf;
	}

	/**
	 * Return the actual length of the internal byte buffer.
	 * 
	 * @return actual length of the buffer
	 */
	public final int getBufferLength() {
		return super.count;
	}

	/**
	 * Return an input stream containing all the (shared) bytes this output
	 * stream has already consumed.
	 * 
	 * @return ByteArrayInputStream
	 */
	public ByteArrayInputStream getInputStream() {
		return new ByteArrayInputStream(super.buf, 0, super.count);
	}

}