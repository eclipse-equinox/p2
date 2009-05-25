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

import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;

/**
 * A SarEntry is the header information for an entry within a
 * org.eclipse.equinox.p2.sar stream.<br>
 * <b>Note: </b>The setTime() and getTime() methods of ZipEntry (our super
 * class) perform time zone dependent (!) conversions. For our serialization and
 * deserialization the stored time has to be time zone neutral. Therefore it is
 * necessary to invert those calculations. This is also the reason for
 * duplicating the javaToDosTime() and dosToJavaTime() methods.
 */
public class SarEntry extends ZipEntry {

	private boolean isEof;
	private boolean isZip;

	/**
	 * The name of the eof org.eclipse.equinox.p2.sar entry.
	 */
	private static final String EOF_ENTRY_NAME = "<eof-org.eclipse.equinox.p2.sar>"; //$NON-NLS-1$

	private static final boolean DEBUG = SarConstants.DEBUG;

	/**
	 * Creates an eof org.eclipse.equinox.p2.sar entry
	 */
	public SarEntry() {
		super(EOF_ENTRY_NAME);
		setMethod(ZipEntry.DEFLATED);
		this.isEof = true;
		this.isZip = false;
	}

	/**
	 * @param zipEntry
	 */
	public SarEntry(ZipEntry zipEntry) {
		super(zipEntry);
		this.isZip = false;
		this.isEof = false;
	}

	/**
	 * @param zipEntry
	 * @param isZip
	 */
	public SarEntry(ZipEntry zipEntry, boolean isZip) {
		super(zipEntry);
		this.isZip = isZip;
		this.isEof = false;
	}

	/**
	 * @param sarInputStream
	 * @throws IOException
	 */
	public SarEntry(SarInputStream sarInputStream) throws IOException {
		// read name!
		super(sarInputStream.readString());

		String comment = sarInputStream.readString();
		long compressedSize = sarInputStream.readLong();
		long crc = sarInputStream.readLong();
		byte[] extra = sarInputStream.readBytes();
		int method = sarInputStream.readInt();
		long size = sarInputStream.readLong();
		long dosTime = sarInputStream.readLong();
		boolean isEof = sarInputStream.readBoolean();
		boolean isZip = sarInputStream.readBoolean();

		if (DEBUG) {
			System.out.println(getName() + "," + comment + "," + compressedSize + "," + crc + "," + extra + "," + method + "," + size + "," + dosTime + "," + isEof + "," + isZip);
		}

		if (method == ZipEntry.STORED) {
			setCompressedSize(compressedSize);
			setCrc(crc);
			setSize(size);
		}

		setComment(comment);
		setExtra(extra);
		setMethod(method);
		setTime(dosToJavaTime(dosTime));
		setEof(isEof);
		setZip(isZip);
	}

	/**
	 * @param sarOutputStream
	 * @throws IOException
	 */
	public void writeTo(SarOutputStream sarOutputStream) throws IOException {
		String comment = this.getComment();
		long compressedSize = this.getCompressedSize();
		long crc = this.getCrc();
		byte[] extra = this.getExtra();
		int method = this.getMethod();
		String name = this.getName();
		long size = this.getSize();
		long dosTime = javaToDosTime(this.getTime());
		boolean isZip = this.isZip();
		boolean isEof = this.isEof();

		if (DEBUG) {
			System.out.println(name + "," + comment + "," + compressedSize + "," + crc + "," + extra + "," + method + "," + size + "," + dosTime + "," + isEof + "," + isZip);
		}

		sarOutputStream.writeString(name);
		sarOutputStream.writeString(comment);
		sarOutputStream.writeLong(compressedSize);
		sarOutputStream.writeLong(crc);
		sarOutputStream.writeBytes(extra);
		sarOutputStream.writeInt(method);
		sarOutputStream.writeLong(size);
		sarOutputStream.writeLong(dosTime);
		sarOutputStream.writeBool(isEof);
		sarOutputStream.writeBool(isZip);
	}

	/**
	 * Is this the eof org.eclipse.equinox.p2.sar entry?
	 * 
	 * @return the answer
	 */
	public boolean isEof() {
		return isEof;
	}

	private void setEof(boolean isEof) {
		this.isEof = isEof;
	}

	/**
	 * @return boolean
	 */
	public boolean isZip() {
		return isZip;
	}

	/**
	 * @param isZip
	 */
	private void setZip(boolean isZip) {
		this.isZip = isZip;
	}

	/*
	 * Converts DOS time to Java time (number of milliseconds since epoch).
	 */
	public final static long dosToJavaTime(long dtime) {
		GregorianCalendar cal = new GregorianCalendar((int) (((dtime >> 25) & 0x7f) + 80) + 1900, (int) (((dtime >> 21) & 0x0f) - 1), (int) ((dtime >> 16) & 0x1f), (int) ((dtime >> 11) & 0x1f), (int) ((dtime >> 5) & 0x3f), (int) ((dtime << 1) & 0x3e));
		return cal.getTime().getTime();
	}

	/*
	 * Converts Java time to DOS time.
	 */
	public final static long javaToDosTime(long time) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(new Date(time));
		int year = cal.get(Calendar.YEAR);
		if (year < 1980)
			return (1 << 21) | (1 << 16);
		int month = cal.get(Calendar.MONTH);
		int date = cal.get(Calendar.DAY_OF_MONTH);
		int hours = cal.get(Calendar.HOUR_OF_DAY);
		int minutes = cal.get(Calendar.MINUTE);
		int seconds = cal.get(Calendar.SECOND);
		return (year - 1980) << 25 | (month + 1) << 21 | date << 16 | hours << 11 | minutes << 5 | seconds >> 1;
	}

}
