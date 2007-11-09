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
import java.util.jar.*;

public class DeltaApplier {
	private static final String DELETE_SUFFIX = ".delete"; //$NON-NLS-1$
	private File delta;
	private File base;
	private File destination;
	private JarFile baseJar;
	private JarFile deltaJar;
	private Set baseEntries;

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
		for (Enumeration e = deltaJar.entries(); e.hasMoreElements();) {
			JarEntry entry = ((JarEntry) e.nextElement());
			String name = entry.getName();
			if (name.endsWith(DELETE_SUFFIX))
				name = name.substring(0, name.length() - DELETE_SUFFIX.length());
			baseEntries.remove(name);
		}
	}

	private void writeResult() {
		JarOutputStream result = null;
		try {
			try {
				result = new JarOutputStream(new FileOutputStream(destination));
				// write out the things we know are staying from the base JAR
				for (Iterator i = baseEntries.iterator(); i.hasNext();) {
					JarEntry entry = baseJar.getJarEntry((String) i.next());
					writeEntry(result, entry, baseJar);
				}
				// write out the changes/additions from the delta.
				for (Enumeration e = deltaJar.entries(); e.hasMoreElements();) {
					JarEntry entry = (JarEntry) e.nextElement();
					if (!entry.getName().endsWith(DELETE_SUFFIX))
						writeEntry(result, entry, deltaJar);
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

	private void writeEntry(JarOutputStream result, JarEntry entry, JarFile sourceJar) throws IOException {
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
			baseJar = new JarFile(base);
			deltaJar = new JarFile(delta);
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

	private Set getEntries(JarFile jar) {
		HashSet result = new HashSet(jar.size());
		for (Enumeration e = jar.entries(); e.hasMoreElements();)
			result.add(((JarEntry) e.nextElement()).getName());
		return result;
	}
}
