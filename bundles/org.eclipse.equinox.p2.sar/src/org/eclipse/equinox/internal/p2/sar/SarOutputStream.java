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

import java.io.*;

/**
 * The SarOutputStream writes a stream archive as an OutputStream. Methods are
 * provided to put entries, and then write their contents by writing to this
 * stream using write().
 */
public class SarOutputStream extends OutputStream {

	private boolean finished;
	private final DataOutputStream dataOutputStream;
	private final DirectByteArrayOutputStream entryContent;

	/**
	 * @param outputStream
	 * @throws IOException
	 */
	public SarOutputStream(OutputStream outputStream) throws IOException {
		dataOutputStream = new DataOutputStream(outputStream);
		entryContent = new DirectByteArrayOutputStream(16 * 1024);
		writeString(SarConstants.SARFILE_MARKER);
		dataOutputStream.writeInt(SarConstants.SARFILE_VERSION);
		finished = false;
	}

	/**
	 * Ends the SAR archive and closes the underlying OutputStream.
	 * 
	 * @see java.io.OutputStream#close()
	 */
	// @Override
	public void close() throws IOException {
		finish();
		super.close();
	}

	/**
	 * Finish this SAR archive but does not close the underlying output stream.
	 * 
	 * @throws IOException
	 */
	public void finish() throws IOException {
		if (finished)
			return;

		writeEOFRecord();
		finished = true;
	}

	/**
	 * Put an entry on the output stream. This writes the entry's header record
	 * and positions the output stream for writing the contents of the entry.
	 * Once this method is called, the stream is ready for calls to write() to
	 * write the entry's contents. Once the contents are written, closeEntry()
	 * <B>MUST </B> be called to ensure that all buffered data is completely
	 * written to the output stream.
	 * 
	 * @param entry
	 *            the SarEntry to be written to the archive.
	 * @throws IOException
	 */
	public void putNextEntry(SarEntry entry) throws IOException {
		entry.writeTo(this);
	}

	/**
	 * Close an entry. This method MUST be called for all file entries that
	 * contain data. The reason is that we must buffer data written to the
	 * stream in order to satisfy the buffer's record based writes. Thus, there
	 * may be data fragments still being assembled that must be written to the
	 * output stream before this entry is closed and the next entry written.
	 * 
	 * @throws IOException
	 */
	public void closeEntry() throws IOException {
		writeBytes(entryContent.getBuffer(), entryContent.getBufferLength());
		entryContent.reset();
	}

	/**
	 * @param s
	 * @throws IOException
	 */
	void writeString(String s) throws IOException {
		byte[] bytes = null;
		if (s != null)
			bytes = s.getBytes(SarConstants.DEFAULT_ENCODING);

		writeBytes(bytes);
	}

	/**
	 * @param bytes
	 * @throws IOException
	 */
	void writeBytes(byte[] bytes) throws IOException {
		writeBytes(bytes, bytes != null ? bytes.length : -1);
	}

	/**
	 * @param bytes
	 * @throws IOException
	 */
	void writeBytes(byte[] bytes, int length) throws IOException {
		if (bytes != null) {
			dataOutputStream.writeInt(length);
			dataOutputStream.write(bytes, 0, length);
		} else {
			dataOutputStream.writeInt(-1);
		}
	}

	/**
	 * @param v
	 * @throws IOException
	 */
	void writeInt(int v) throws IOException {
		dataOutputStream.writeInt(v);
	}

	/**
	 * @param bool
	 * @throws IOException
	 */
	public void writeBool(boolean bool) throws IOException {
		dataOutputStream.writeBoolean(bool);
	}

	/**
	 * @param v
	 * @throws IOException
	 */
	void writeLong(long v) throws IOException {
		dataOutputStream.writeLong(v);
	}

	/**
	 * Writes a byte to the current org.eclipse.equinox.p2.sar archive entry.
	 * 
	 * @param b
	 *            the byte written.
	 * @throws IOException
	 * 
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException {
		byte[] bytes = new byte[1];
		bytes[0] = (byte) b;
		entryContent.write(bytes);
	}

	/**
	 * Writes bytes to the current org.eclipse.equinox.p2.sar archive entry.
	 * 
	 * @param bytes
	 *            The buffer to write to the archive.
	 * @throws IOException
	 * 
	 * @see java.io.OutputStream#write(byte[])
	 */
	public void write(byte[] bytes) throws IOException {
		entryContent.write(bytes, 0, bytes.length);
	}

	/**
	 * Writes bytes to the current org.eclipse.equinox.p2.sar archive entry.
	 * 
	 * @param bytes
	 *            The buffer to write to the archive.
	 * @param offset
	 *            The offset in the buffer from which to get bytes.
	 * @param numToWrite
	 *            The number of bytes to write.
	 * 
	 * @throws IOException
	 * 
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(byte[] bytes, int offset, int numToWrite) throws IOException {
		entryContent.write(bytes, offset, numToWrite);
	}

	/**
	 * Write an EOF (end of archive) entry to the org.eclipse.equinox.p2.sar archive.
	 * 
	 * @throws IOException
	 */
	private void writeEOFRecord() throws IOException {
		SarEntry eofEntry = new SarEntry();
		eofEntry.writeTo(this);
	}

}
