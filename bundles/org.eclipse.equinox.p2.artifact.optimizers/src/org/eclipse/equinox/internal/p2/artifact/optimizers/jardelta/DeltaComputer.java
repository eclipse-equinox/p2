package org.eclipse.equinox.internal.p2.artifact.optimizers.jardelta;

import java.io.*;
import java.util.*;
import java.util.jar.*;

public class DeltaComputer {
	private File target;
	private File base;
	private File destination;
	private JarFile baseJar;
	private JarFile targetJar;
	private Set baseEntries;
	private ArrayList additions;
	private ArrayList changes;

	public DeltaComputer(File base, File target, File destination) {
		this.base = base;
		this.target = target;
		this.destination = destination;
	}

	public void run() {
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
		JarOutputStream result = null;
		try {
			result = new JarOutputStream(new FileOutputStream(destination));
			// write out the removals.  These are all the entries left in the baseEntries
			// since they were not seen in the targetJar.  Here just write out an empty
			// entry with a name that signals the delta processor to delete.
			for (Iterator i = baseEntries.iterator(); i.hasNext();)
				writeEntry(result, new JarEntry(((JarEntry) i.next()).getName() + ".delete"), null);
			// write out the additions.
			for (Iterator i = additions.iterator(); i.hasNext();)
				writeEntry(result, (JarEntry) i.next(), targetJar);
			// write out the changes.
			for (Iterator i = changes.iterator(); i.hasNext();)
				writeEntry(result, (JarEntry) i.next(), targetJar);
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

	private void computeDelta() {
		ArrayList changes = new ArrayList();
		ArrayList additions = new ArrayList();
		// start out assuming that all the base entries are being removed
		baseEntries = getEntries(baseJar);
		for (Enumeration e = targetJar.entries(); e.hasMoreElements();) {
			JarEntry entry = (JarEntry) e.nextElement();
			check(entry);
		}
	}

	private boolean openJars() {
		try {
			baseJar = new JarFile(base);
			targetJar = new JarFile(target);
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
	 */
	private void check(JarEntry entry) {
		JarEntry baseEntry = baseJar.getJarEntry(entry.getName());
		// if there is no entry then this is an addition.  remember the addition and return;
		if (baseEntry == null) {
			additions.add(entry);
			return;
		}
		// now we know each JAR has an entry for the name, compare and see how/if they differ
		boolean changed = equals(entry, baseEntry);
		if (changed)
			changes.add(entry);
		baseEntries.remove(baseEntry);
	}

	// compare the two entries.  We already know that they have the same name.
	private boolean equals(JarEntry entry, JarEntry baseEntry) {
		if (entry.getSize() != baseEntry.getSize())
			return false;
		if (entry.getTime() != baseEntry.getTime())
			return false;
		return true;
	}

	private Set getEntries(JarFile jar) {
		HashSet result = new HashSet(jar.size());
		for (Enumeration e = jar.entries(); e.hasMoreElements();)
			result.add(e.nextElement());
		return result;
	}
}
