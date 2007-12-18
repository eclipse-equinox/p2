/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
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
 * The SarInputStream reads a streaming archive as an InputStream. Methods are
 * provided to position at each successive entry in the archive, and the read
 * each entry as a normal input stream using read().
 */
public class SarInputStream extends InputStream {

	private final DataInputStream dataInputStream;
	private final int version;
	private InputStream contentStream;

	/**
	 * Constructor for SarInputStream.
	 * 
	 * @param inputStream
	 *            the input stream to use
	 * @throws IOException
	 */
	public SarInputStream(InputStream inputStream) throws IOException {

		this.dataInputStream = new DataInputStream(inputStream);

		// SarFile marker
		String marker = readString();
		if (!marker.equals(SarConstants.SARFILE_MARKER)) {
			throw new IOException("Does not contain org.eclipse.equinox.p2.sar marker.");
		}

		// SarFile version
		version = dataInputStream.readInt();
		if (version != SarConstants.SARFILE_VERSION) {
			throw new IOException("Unsupported version.");
		}
	}

	/**
	 * Closes this stream.
	 * 
	 * @throws IOException
	 *             on error
	 */
	public void close() throws IOException {
		dataInputStream.close();
	}

	/**
	 * Since we do not support marking just yet, we return false.
	 * 
	 * @return False.
	 */
	public boolean markSupported() {
		return false;
	}

	/**
	 * Since we do not support marking just yet, we do nothing.
	 * 
	 * @param markLimit
	 *            The limit to mark.
	 */
	public void mark(int markLimit) {
		// nothing
	}

	/**
	 * Since we do not support marking just yet, we do nothing.
	 */
	public void reset() {
		// nothing
	}

	/**
	 * Get the next entry in this org.eclipse.equinox.p2.sar archive. This will skip
	 * over any remaining data in the current entry, if there is one, and place
	 * the input stream at the header of the next entry, and read the header and
	 * instantiate a new SarEntry from the header bytes and return that entry.
	 * If there are no more entries in the archive, null will be returned to
	 * indicate that the end of the archive has been reached.
	 * 
	 * @return the next SarEntry in the archive, or null.
	 * @throws IOException
	 *             on error
	 */
	public SarEntry getNextEntry() throws IOException {
		SarEntry sarEntry = new SarEntry(this);
		if (sarEntry.isEof())
			return null;

		byte[] content = readBytes();
		contentStream = new ByteArrayInputStream(content);
		return sarEntry;

	}

	/**
	 * Close the entry.
	 * 
	 * @throws IOException
	 */
	public void closeEntry() throws IOException {
		contentStream.close();
	}

	/**
	 * @return String
	 * @throws IOException
	 */
	String readString() throws IOException {
		byte[] bytes = readBytes();
		if (bytes == null)
			return null;

		return new String(bytes, SarConstants.DEFAULT_ENCODING);
	}

	/**
	 * @return byte[]
	 * @throws IOException
	 */
	byte[] readBytes() throws IOException {
		int length = dataInputStream.readInt();
		if (length == -1)
			return null;

		byte[] bytes = new byte[length];
		dataInputStream.readFully(bytes, 0, length);
		return bytes;
	}

	/**
	 * @return int
	 * @throws IOException
	 */
	int readInt() throws IOException {
		return dataInputStream.readInt();
	}

	/**
	 * @return boolean
	 * @throws IOException
	 */
	boolean readBoolean() throws IOException {
		return dataInputStream.readBoolean();
	}

	/**
	 * @return long
	 * @throws IOException
	 */
	long readLong() throws IOException {
		return dataInputStream.readLong();
	}

	/**
	 * Reads a byte from the current tar archive entry.
	 * 
	 * This method simply calls read( byte[], int, int ).
	 * 
	 * @return The byte read, or -1 at EOF.
	 * @throws IOException
	 *             on error
	 */
	public int read() throws IOException {
		return contentStream.read();
	}

	/**
	 * Reads bytes from the current tar archive entry.
	 * 
	 * This method is aware of the boundaries of the current entry in the
	 * archive and will deal with them as if they were this stream's start and
	 * EOF.
	 * 
	 * @param buffer
	 *            The buffer into which to place bytes read.
	 * @param offset
	 *            The offset at which to place bytes read.
	 * @param numToRead
	 *            The number of bytes to read.
	 * @return The number of bytes read, or -1 at EOF.
	 * @throws IOException
	 *             on error
	 */
	public int read(byte[] buffer, int offset, int numToRead) throws IOException {
		return contentStream.read(buffer, offset, numToRead);
	}

}