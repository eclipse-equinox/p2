/*******************************************************************************
 *  Copyright (c) 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.tests;

import java.io.OutputStream;

public class StringBufferStream extends OutputStream {
	private StringBuffer buffer;

	public StringBufferStream() {
		this.buffer = new StringBuffer();
	}

	public StringBufferStream(StringBuffer buffer) {
		this.buffer = buffer;
	}

	public StringBuffer getBuffer() {
		return buffer;
	}

	@Override
	public void close() {
		//nothing
	}

	@Override
	public void flush() {
		//nothing
	}

	@Override
	public void write(byte[] b) {
		buffer.append(new String(b));
	}

	@Override
	public void write(int b) {
		buffer.append(new String(new byte[] {(byte) b}));
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		buffer.append(new String(buf, off, len));
	}
}
