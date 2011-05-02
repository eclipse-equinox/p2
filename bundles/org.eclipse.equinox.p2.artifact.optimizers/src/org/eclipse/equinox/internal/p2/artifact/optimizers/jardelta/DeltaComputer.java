/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.jardelta;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class DeltaComputer {
	private File target;
	private File base;
	private File destination;
	private ZipFile baseJar;
	private ZipFile targetJar;
	private Set<String> baseEntries;
	private ArrayList<ZipEntry> additions;
	private ArrayList<ZipEntry> changes;
	private ZipFile manifestJar = null;

	public DeltaComputer(File base, File target, File destination) {
		this.base = base;
		this.target = target;
		this.destination = destination;
	}

	public void run() throws IOException {
		try {
			if (!openJars())
				return;
			computeDelta();
			writeDelta();
		} finally {
			closeJars();
		}
	}

	private void writeDelta() {
		ZipOutputStream result = null;
		try {
			try {
				result = new ZipOutputStream(new FileOutputStream(destination));
				// if the delta includes the manifest, be sure to write it first
				if (manifestJar != null)
					writeEntry(result, manifestJar.getEntry("META-INF/MANIFEST.MF"), manifestJar, true);
				// write out the removals.  These are all the entries left in the baseEntries
				// since they were not seen in the targetJar.  Here just write out an empty
				// entry with a name that signals the delta processor to delete.
				for (String baseEntry : baseEntries)
					writeEntry(result, new ZipEntry(baseEntry + ".delete"), null, false);
				// write out the additions.
				for (ZipEntry entry : additions)
					writeEntry(result, entry, targetJar, false);
				// write out the changes.
				for (ZipEntry entry : changes)
					writeEntry(result, entry, targetJar, false);
			} finally {
				if (result != null)
					result.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	private void writeEntry(ZipOutputStream result, ZipEntry entry, ZipFile sourceJar, boolean manifest) throws IOException {
		if (!manifest && entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF"))
			return;
		// add the entry
		result.putNextEntry(entry);
		try {
			// if there is a sourceJar copy over the content for the entry into the result
			if (sourceJar != null) {
				InputStream contents = sourceJar.getInputStream(entry);
				try {
					transferStreams(contents, result);
				} finally {
					contents.close();
				}
			}
		} finally {
			result.closeEntry();
		}
	}

	/**
	 * Transfers all available bytes from the given input stream to the given
	 * output stream. Does not close either stream.
	 * 
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	public static void transferStreams(InputStream source, OutputStream destination) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			destination.flush();
		}
	}

	private void computeDelta() throws IOException {
		changes = new ArrayList<ZipEntry>();
		additions = new ArrayList<ZipEntry>();
		// start out assuming that all the base entries are being removed
		baseEntries = getEntries(baseJar);
		for (Enumeration<? extends ZipEntry> e = targetJar.entries(); e.hasMoreElements();)
			check(e.nextElement(), targetJar);
	}

	private boolean openJars() {
		try {
			baseJar = new ZipFile(base);
			targetJar = new ZipFile(target);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private void closeJars() {
		if (baseJar != null)
			try {
				baseJar.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		if (targetJar != null)
			try {
				targetJar.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	/** 
	 * Compare the given entry against the base JAR to see if/how it differs.  Update the appropriate set
	 * based on the discovered difference.
	 * @param entry the entry to test
	 * @throws IOException 
	 */
	private void check(ZipEntry entry, ZipFile file) throws IOException {
		ZipEntry baseEntry = baseJar.getEntry(entry.getName());

		// remember the manifest if we see it
		checkForManifest(entry, file);
		// if there is no entry then this is an addition.  remember the addition and return;
		if (baseEntry == null) {
			additions.add(entry);
			return;
		}
		// now we know each JAR has an entry for the name, compare and see how/if they differ
		boolean changed = !equals(entry, baseEntry);
		if (changed)
			changes.add(entry);
		baseEntries.remove(baseEntry.getName());
	}

	// compare the two entries.  We already know that they have the same name.
	private boolean equals(ZipEntry entry, ZipEntry baseEntry) {
		if (entry.getSize() != baseEntry.getSize())
			return false;
		// make sure the entries are of the same type
		if (entry.isDirectory() != baseEntry.isDirectory())
			return false;
		// if the entries are files then compare the times.
		if (!entry.isDirectory())
			if (entry.getTime() != baseEntry.getTime())
				return false;
		return true;
	}

	private Set<String> getEntries(ZipFile jar) {
		HashSet<String> result = new HashSet<String>(jar.size());
		for (Enumeration<? extends ZipEntry> e = jar.entries(); e.hasMoreElements();) {
			ZipEntry entry = e.nextElement();
			checkForManifest(entry, jar);
			result.add(entry.getName());
		}
		return result;
	}

	/**
	 * Check to see if the given entry is the manifest.  If so, remember it for use when writing
	 * the resultant JAR.
	 * @param entry
	 * @param jar
	 */
	private void checkForManifest(ZipEntry entry, ZipFile jar) {
		if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF"))
			manifestJar = jar;
	}
}
