/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.jardelta;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class DeltaApplier {
	private static final String DELETE_SUFFIX = ".delete"; //$NON-NLS-1$
	private static final String MANIFEST_ENTRY_NAME = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	private File delta;
	private File base;
	private File destination;
	private ZipFile baseJar;
	private ZipFile deltaJar;
	private Set<String> baseEntries;
	private ZipFile manifestJar;

	public DeltaApplier(File base, File delta, File destination) {
		this.base = base;
		this.delta = delta;
		this.destination = destination;
	}

	public void run() {
		try {
			if (!openJars())
				return;
			applyDelta();
			writeResult();
		} finally {
			closeJars();
		}
	}

	private void applyDelta() {
		// start out assuming that all the base entries will be moved over.  
		baseEntries = getEntries(baseJar);
		// remove from the base all the entries that appear in the delta
		for (Enumeration<? extends ZipEntry> e = deltaJar.entries(); e.hasMoreElements();) {
			ZipEntry entry = e.nextElement();
			checkForManifest(entry, deltaJar);
			String name = entry.getName();
			if (name.endsWith(DELETE_SUFFIX)) {
				name = name.substring(0, name.length() - DELETE_SUFFIX.length());
				// if the manifest is being deleted, forget anyone who might have a manifest
				if (name.equalsIgnoreCase(MANIFEST_ENTRY_NAME))
					manifestJar = null;
			}
			baseEntries.remove(name);
		}
	}

	private void writeResult() {
		ZipOutputStream result = null;
		try {
			try {
				result = new ZipOutputStream(new FileOutputStream(destination));
				// if the delta includes the manifest, be sure to write it first
				if (manifestJar != null)
					writeEntry(result, manifestJar.getEntry(MANIFEST_ENTRY_NAME), manifestJar, true);
				// write out the things we know are staying from the base JAR
				for (Iterator<String> i = baseEntries.iterator(); i.hasNext();) {
					ZipEntry entry = baseJar.getEntry(i.next());
					writeEntry(result, entry, baseJar, false);
				}
				// write out the changes/additions from the delta.
				for (Enumeration<? extends ZipEntry> e = deltaJar.entries(); e.hasMoreElements();) {
					ZipEntry entry = e.nextElement();
					if (!entry.getName().endsWith(DELETE_SUFFIX))
						writeEntry(result, entry, deltaJar, false);
				}
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
		if (!manifest && entry.getName().equalsIgnoreCase(MANIFEST_ENTRY_NAME))
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

	private boolean openJars() {
		try {
			baseJar = new ZipFile(base);
			deltaJar = new ZipFile(delta);
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
		if (deltaJar != null)
			try {
				deltaJar.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
		if (entry.getName().equalsIgnoreCase(MANIFEST_ENTRY_NAME))
			manifestJar = jar;
	}

}
