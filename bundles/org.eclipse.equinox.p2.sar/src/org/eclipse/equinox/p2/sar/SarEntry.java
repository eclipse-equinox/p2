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
package org.eclipse.equinox.p2.sar;

import java.io.IOException;
import java.util.zip.ZipEntry;

/**
 * A SarEntry is the header information for an entry within a org.eclipse.equinox.p2.sar stream.
 */
public class SarEntry extends ZipEntry {

	private boolean isEof;
	private boolean isZip;

	/** 
	 * The name of the eof org.eclipse.equinox.p2.sar entry.
	 */
	private static final String EOF_ENTRY_NAME = "<eof-org.eclipse.equinox.p2.sar>";

	private static final boolean DEBUG = SarConstants.DEBUG;

	/**
	 * Creates an eof org.eclipse.equinox.p2.sar entry
	 */
	public SarEntry() {
		super( EOF_ENTRY_NAME );
		setMethod( ZipEntry.DEFLATED );
		this.isEof = true;
		this.isZip = false;
	}

	/**
	 * @param zipEntry
	 */
	public SarEntry( ZipEntry zipEntry ) {
		super( zipEntry );
		this.isZip = false;
		this.isEof = false;
	}

	/**
	 * @param zipEntry
	 * @param isZip
	 */
	public SarEntry( ZipEntry zipEntry, boolean isZip ) {
		super( zipEntry );
		this.isZip = isZip;
		this.isEof = false;
	}

	/**
	 * @param sarInputStream
	 * @throws IOException
	 */
	public SarEntry( SarInputStream sarInputStream ) throws IOException {
		// read name!
		super( sarInputStream.readString() );

		String comment = sarInputStream.readString();
		long compressedSize = sarInputStream.readLong();
		long crc = sarInputStream.readLong();
		byte[] extra = sarInputStream.readBytes();
		int method = sarInputStream.readInt();
		long size = sarInputStream.readLong();
		long time = sarInputStream.readLong();
		boolean isEof = sarInputStream.readBoolean();
		boolean isZip = sarInputStream.readBoolean();

		if ( DEBUG ) {
			System.out.println( getName() + "," + comment + "," + compressedSize + "," + crc + "," + extra + "," + method + "," + size + "," + time + ","
				+ isEof + "," + isZip );
		}

		if ( method == ZipEntry.STORED ) {
			setCompressedSize( compressedSize );
			setCrc( crc );
			setSize( size );
		}

		setComment( comment );
		setExtra( extra );
		setMethod( method );
		setTime( time );
		setEof( isEof );
		setZip( isZip );
	}

	/**
	 * @param sarOutputStream
	 * @throws IOException
	 */
	public void writeTo( SarOutputStream sarOutputStream ) throws IOException {
		String comment = this.getComment();
		long compressedSize = this.getCompressedSize();
		long crc = this.getCrc();
		byte[] extra = this.getExtra();
		int method = this.getMethod();
		String name = this.getName();
		long size = this.getSize();
		long time = this.getTime();
		boolean isZip = this.isZip();
		boolean isEof = this.isEof();

		if ( DEBUG ) {
			System.out.println( name + "," + comment + "," + compressedSize + "," + crc + "," + extra + "," + method + "," + size + "," + time + "," + isEof
				+ "," + isZip );
		}

		sarOutputStream.writeString( name );
		sarOutputStream.writeString( comment );
		sarOutputStream.writeLong( compressedSize );
		sarOutputStream.writeLong( crc );
		sarOutputStream.writeBytes( extra );
		sarOutputStream.writeInt( method );
		sarOutputStream.writeLong( size );
		sarOutputStream.writeLong( time );
		sarOutputStream.writeBool( isEof );
		sarOutputStream.writeBool( isZip );
	}

	/**
	 * Is this the eof org.eclipse.equinox.p2.sar entry?
	 * 
	 * @return  the answer
	 */
	public boolean isEof() {
		return isEof;
	}

	private void setEof( boolean isEof ) {
		this.isEof = isEof;
	}

	/**
	 * @return
	 */
	public boolean isZip() {
		return isZip;
	}

	/**
	 * @param isZip
	 */
	private void setZip( boolean isZip ) {
		this.isZip = isZip;
	}

}
