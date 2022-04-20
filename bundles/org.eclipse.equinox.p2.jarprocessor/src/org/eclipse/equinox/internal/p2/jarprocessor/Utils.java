/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessor;

/**
 * @author aniefer@ca.ibm.com
 *
 */
public class Utils {
	public static final String MARK_FILE_NAME = "META-INF/eclipse.inf"; //$NON-NLS-1$

	/*
	 * Properties found in outer pack.properties file
	 */
	// comma separated list of jars to exclude from sigining
	public static final String SIGN_EXCLUDES = "sign.excludes"; //$NON-NLS-1$
	// comma separated list of jars to exlclude from packing
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             for details.
	 */
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static final String PACK_EXCLUDES = "pack.excludes"; //$NON-NLS-1$
	// Suffix used when specifying arguments to use when running pack200 on a jar

	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             for details.
	 */
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static final String PACK_ARGS_SUFFIX = ".pack.args"; //$NON-NLS-1$

	/*
	 * Properties found in both pack.properties and eclipse.inf
	 */
	// Default arguments to use when running pack200.
	// Affects all jars when specified in pack.properties, affects children when
	// specified in eclipse.inf
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             for details.
	 */
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static final String DEFAULT_PACK_ARGS = "pack200.default.args"; //$NON-NLS-1$

	/*
	 * Properties found in eclipse.inf file
	 */
	// This jar has been conditioned with pack200
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             for details.
	 */
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static final String MARK_PROPERTY = "pack200.conditioned"; //$NON-NLS-1$
	// Exclude this jar from processing
	public static final String MARK_EXCLUDE = "jarprocessor.exclude"; //$NON-NLS-1$
	// Exclude this jar from pack200
	public static final String MARK_EXCLUDE_PACK = "jarprocessor.exclude.pack"; //$NON-NLS-1$
	// Exclude this jar from signing
	public static final String MARK_EXCLUDE_SIGN = "jarprocessor.exclude.sign"; //$NON-NLS-1$
	// Exclude this jar's children from processing
	public static final String MARK_EXCLUDE_CHILDREN = "jarprocessor.exclude.children"; //$NON-NLS-1$
	// Exclude this jar's children from pack200
	public static final String MARK_EXCLUDE_CHILDREN_PACK = "jarprocessor.exclude.children.pack"; //$NON-NLS-1$
	// Exclude this jar's children from signing
	public static final String MARK_EXCLUDE_CHILDREN_SIGN = "jarprocessor.exclude.children.sign"; //$NON-NLS-1$
	// Arguments used in pack200 for this jar
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             for details.
	 */
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static final String PACK_ARGS = "pack200.args"; //$NON-NLS-1$
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             for details.
	 */
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static final String PACK200_PROPERTY = "org.eclipse.update.jarprocessor.pack200"; //$NON-NLS-1$
	public static final String JRE = "@jre"; //$NON-NLS-1$
	public static final String PATH = "@path"; //$NON-NLS-1$
	public static final String NONE = "@none"; //$NON-NLS-1$
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             for details.
	 */
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static final String PACKED_SUFFIX = ".pack.gz"; //$NON-NLS-1$
	public static final String JAR_SUFFIX = ".jar"; //$NON-NLS-1$

	public static final FileFilter JAR_FILTER = pathname -> pathname.isFile() && pathname.getName().endsWith(".jar"); //$NON-NLS-1$
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             for details.
	 */
	@SuppressWarnings("removal")
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static final FileFilter PACK_GZ_FILTER = pathname -> pathname.isFile()
			&& pathname.getName().endsWith(JarProcessor.PACKED_SUFFIX);

	public static void close(Object stream) {
		if (stream != null) {
			try {
				if (stream instanceof InputStream)
					((InputStream) stream).close();
				else if (stream instanceof OutputStream)
					((OutputStream) stream).close();
				else if (stream instanceof JarFile)
					((JarFile) stream).close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/**
	 * Get the set of commands to try to execute pack/unpack
	 * 
	 * @param cmd the command, either "pack200" or "unpack200"
	 * @return <code>null</code> as pack200 is not supported on Java >= 14
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             and <a href=
	 *             "https://github.com/eclipse-equinox/p2/issues/40">issue</a> for
	 *             details.
	 */
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static String[] getPack200Commands(String cmd) {
		return null;
	}

	/**
	 * Transfers all available bytes from the given input stream to the given output
	 * stream. Closes both streams if close == true, regardless of failure. Flushes
	 * the destination stream if close == false
	 * 
	 * @param source
	 * @param destination
	 * @param close
	 * @throws IOException
	 */
	public static void transferStreams(InputStream source, OutputStream destination, boolean close) throws IOException {
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
			if (close) {
				close(source);
				close(destination);
			} else {
				destination.flush();
			}
		}
	}

	/**
	 * Deletes all the files and directories from the given root down (inclusive).
	 * Returns false if we could not delete some file or an exception occurred at
	 * any point in the deletion. Even if an exception occurs, a best effort is made
	 * to continue deleting.
	 */
	public static boolean clear(java.io.File root) {
		boolean result = clearChildren(root);
		try {
			if (root.exists())
				result &= root.delete();
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	/**
	 * Deletes all the files and directories from the given root down, except for
	 * the root itself. Returns false if we could not delete some file or an
	 * exception occurred at any point in the deletion. Even if an exception occurs,
	 * a best effort is made to continue deleting.
	 */
	public static boolean clearChildren(java.io.File root) {
		boolean result = true;
		if (root.isDirectory()) {
			String[] list = root.list();
			// for some unknown reason, list() can return null.
			// Just skip the children If it does.
			if (list != null)
				for (String list1 : list) {
					result &= clear(new java.io.File(root, list1));
				}
		}
		return result;
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @deprecated See <a href=
	 *             "https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a>
	 *             for details.
	 */
	@Deprecated(forRemoval = true, since = "1.2.0")
	public static Set<String> getPackExclusions(Properties properties) {
		return Collections.emptySet();
	}

	public static Set<String> getSignExclusions(Properties properties) {
		if (properties == null)
			return Collections.emptySet();
		String signExcludes = properties.getProperty(SIGN_EXCLUDES);
		if (signExcludes != null) {
			String[] excludes = toStringArray(signExcludes, ","); //$NON-NLS-1$
			Set<String> signExclusions = new HashSet<>();
			for (String exclude : excludes) {
				signExclusions.add(exclude);
			}
			return signExclusions;
		}
		return Collections.emptySet();
	}

	public static String concat(String[] array) {
		return String.join(String.valueOf(' '), array);
	}

	public static String[] toStringArray(String input, String separator) {
		StringTokenizer tokenizer = new StringTokenizer(input, separator);
		int count = tokenizer.countTokens();
		String[] result = new String[count];
		for (int i = 0; i < count; i++) {
			result[i] = tokenizer.nextToken().trim();
		}
		return result;
	}

	/**
	 * Get the properties from the eclipse.inf file from the given jar. If the file
	 * is not a jar, null is returned. If the file is a jar, but does not contain an
	 * eclipse.inf file, an empty Properties object is returned.
	 * 
	 * @param jarFile
	 * @return The eclipse.inf properties for the given jar file
	 */
	public static Properties getEclipseInf(File jarFile, boolean verbose) {
		if (jarFile == null || !jarFile.exists()) {
			if (verbose)
				System.out.println("Failed to obtain eclipse.inf due to missing jar file: " + jarFile); //$NON-NLS-1$
			return null;
		}
		JarFile jar = null;
		try {
			jar = new JarFile(jarFile, false);
			JarEntry mark = jar.getJarEntry(MARK_FILE_NAME);
			if (mark != null) {
				try (InputStream in = jar.getInputStream(mark)) {
					Properties props = new Properties();
					props.load(in);
					return props;
				}
			}
			return new Properties();
		} catch (ZipException e) {
			// not a jar, don't bother logging this.
			return null;
		} catch (IOException e) {
			if (verbose) {
				System.out.println("Failed to obtain eclipse.inf due to IOException: " + jarFile); //$NON-NLS-1$
				e.printStackTrace();
			}
			return null;
		} finally {
			close(jar);
		}
	}

	public static boolean shouldSkipJar(File input, boolean processAll, boolean verbose) {
		Properties inf = getEclipseInf(input, verbose);
		if (inf == null) {
			// not a jar, could be a pack.gz
			return false;
		}
		String exclude = inf.getProperty(MARK_EXCLUDE);

		// was marked as exclude, we should skip
		if (exclude != null && Boolean.parseBoolean(exclude))
			return true;

		// process all was set, don't skip
		if (processAll)
			return false;

		// otherwise, we skip if not marked marked
		String marked = inf.getProperty(MARK_PROPERTY);
		return !Boolean.parseBoolean(marked);
	}

	/**
	 * Stores the given properties in the output stream. We store the properties in
	 * sorted order so that the signing hash doesn't change if the properties didn't
	 * change.
	 * 
	 * @param props
	 * @param stream
	 */
	public static void storeProperties(Properties props, OutputStream stream) {
		PrintStream printStream = new PrintStream(stream);
		printStream.print("#Processed using Jarprocessor\n"); //$NON-NLS-1$
		SortedMap<Object, Object> sorted = new TreeMap<>(props);
		for (Object object : sorted.keySet()) {
			String key = (String) object;
			printStream.print(key);
			printStream.print(" = "); //$NON-NLS-1$
			printStream.print(sorted.get(key));
			printStream.print("\n"); //$NON-NLS-1$

		}
		printStream.flush();
	}
}
