/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.utils;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class Utils {
	private static final String FILE_SCHEME = "file"; //$NON-NLS-1$
	private static final String PATH_SEP = "/"; //$NON-NLS-1$

	/**
	 * Overwrite all properties of from to the properties of to. Return the result
	 * of to.
	 * 
	 * @param to
	 *            Properties whose keys and values of other Properties will be
	 *            appended to.
	 * @param from
	 *            Properties whose keys and values will be set to the other
	 *            properties.
	 * @return Properties as a result of this method.
	 */
	public static Properties appendProperties(Properties to, Properties from) {
		if (from != null) {
			if (to == null)
				to = new Properties();
			// printoutProperties(System.out, "to", to);
			// printoutProperties(System.out, "from", from);

			for (Enumeration<Object> enumeration = from.keys(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				to.setProperty(key, from.getProperty(key));
			}
		}
		// printoutProperties(System.out, "to", to);
		return to;
	}

	// Return a dictionary representing a manifest. The data may result from
	// plugin.xml conversion
	private static Dictionary<String, String> basicLoadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		ZipFile jarFile = null;
		try {
			try {
				// Handle a JAR'd bundle
				if (bundleLocation.isFile()) {
					jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
					ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
					if (manifestEntry != null) {
						manifestStream = jarFile.getInputStream(manifestEntry);
					}
				} else {
					// we have a directory-based bundle
					File bundleManifestFile = new File(bundleLocation, JarFile.MANIFEST_NAME);
					if (bundleManifestFile.exists())
						manifestStream = new BufferedInputStream(
								new FileInputStream(new File(bundleLocation, JarFile.MANIFEST_NAME)));
				}
			} catch (IOException e) {
				// ignore
			}
			// we were unable to get an OSGi manifest file so try and convert an old-style
			// manifest
			if (manifestStream == null)
				return null;

			try {
				Map<String, String> manifest = ManifestElement.parseBundleManifest(manifestStream, null);
				// add this check to handle the case were we read a non-OSGi manifest
				if (manifest.get(Constants.BUNDLE_SYMBOLICNAME) == null)
					return null;
				return manifestToProperties(manifest);
			} catch (IOException | BundleException ioe) {
				return null;
			}
		} finally {
			try {
				if (manifestStream != null)
					manifestStream.close();
			} catch (IOException e1) {
				// Ignore
			}
			try {
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e2) {
				// Ignore
			}
		}
	}

	public static void checkAbsoluteDir(File file, String dirName) throws IllegalArgumentException {
		if (file == null)
			throw new IllegalArgumentException(dirName + " is null"); //$NON-NLS-1$
		if (!file.isAbsolute())
			throw new IllegalArgumentException(dirName + " is not absolute path. file=" + file.getAbsolutePath()); //$NON-NLS-1$
		if (!file.isDirectory())
			throw new IllegalArgumentException(dirName + " is not directory. file=" + file.getAbsolutePath()); //$NON-NLS-1$
	}

	public static void checkAbsoluteFile(File file, String dirName) {// throws ManipulatorException {
		if (file == null)
			throw new IllegalArgumentException(dirName + " is null"); //$NON-NLS-1$
		if (!file.isAbsolute())
			throw new IllegalArgumentException(dirName + " is not absolute path. file=" + file.getAbsolutePath()); //$NON-NLS-1$
		if (file.isDirectory())
			throw new IllegalArgumentException(dirName + " is not file but directory"); //$NON-NLS-1$
	}

	public static URL checkFullUrl(URL url, String urlName) throws IllegalArgumentException {// throws
																								// ManipulatorException
																								// {
		if (url == null)
			throw new IllegalArgumentException(urlName + " is null"); //$NON-NLS-1$
		if (!url.getProtocol().endsWith("file")) //$NON-NLS-1$
			return url;
		File file = new File(url.getFile());
		if (!file.isAbsolute())
			throw new IllegalArgumentException(urlName + "(" + url + ") does not have absolute path"); //$NON-NLS-1$ //$NON-NLS-2$
		if (file.getAbsolutePath().startsWith(PATH_SEP))
			return url;
		try {
			return getUrl("file", null, PATH_SEP + file.getAbsolutePath()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(
					urlName + "(" + "file:" + PATH_SEP + file.getAbsolutePath() + ") is not fully quallified"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	public static boolean createParentDir(File file) {
		File parent = file.getParentFile();
		if (parent == null)
			return false;
		if (parent.exists())
			return true;
		return parent.mkdirs();
	}

	public static BundleInfo[] getBundleInfosFromList(List<BundleInfo> list) {
		if (list == null)
			return new BundleInfo[0];
		BundleInfo[] ret = new BundleInfo[list.size()];
		list.toArray(ret);
		return ret;
	}

	public static String[] getClauses(String header) {
		StringTokenizer token = new StringTokenizer(header, ","); //$NON-NLS-1$
		List<String> list = new LinkedList<>();
		while (token.hasMoreTokens()) {
			list.add(token.nextToken());
		}
		String[] ret = new String[list.size()];
		list.toArray(ret);
		return ret;
	}

	public static String[] getClausesManifestMainAttributes(URI location, String name) {
		return getClauses(getManifestMainAttributes(location, name));
	}

	public static String getManifestMainAttributes(URI location, String name) {
		Dictionary<String, String> manifest = Utils.getOSGiManifest(location);
		if (manifest == null)
			throw new RuntimeException("Unable to locate bundle manifest: " + location); //$NON-NLS-1$
		return manifest.get(name);
	}

	public static Dictionary<String, String> getOSGiManifest(URI location) {
		if (location == null)
			return null;
		// if we have a file-based URL that doesn't end in ".jar" then...
		if (FILE_SCHEME.equals(location.getScheme()))
			return basicLoadManifest(URIUtil.toFile(location));

		try {
			URL url = new URL("jar:" + location.toString() + "!/"); //$NON-NLS-1$//$NON-NLS-2$
			JarURLConnection jarConnection = (JarURLConnection) url.openConnection();

			try (ZipFile jar = jarConnection.getJarFile()) {
				ZipEntry entry = jar.getEntry(JarFile.MANIFEST_NAME);
				if (entry == null)
					return null;

				Map<String, String> manifest = ManifestElement.parseBundleManifest(jar.getInputStream(entry), null);
				// if we have a JAR'd bundle that has a non-OSGi manifest file (like
				// the ones produced by Ant, then try and convert the plugin.xml
				if (manifest.get(Constants.BUNDLE_SYMBOLICNAME) == null) {
					return null;
				}
				return manifestToProperties(manifest);
			} catch (BundleException e) {
				return null;
			}
		} catch (IOException e) {
			if (System.getProperty("osgi.debug") != null) { //$NON-NLS-1$
				System.err.println("location=" + location); //$NON-NLS-1$
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String getPathFromClause(String clause) {
		if (clause == null)
			return null;
		if (clause.indexOf(";") != -1) //$NON-NLS-1$
			clause = clause.substring(0, clause.indexOf(";")); //$NON-NLS-1$
		return clause.trim();
	}

	public static String getRelativePath(File target, File from) {

		String targetPath = Utils.replaceAll(target.getAbsolutePath(), File.separator, PATH_SEP);
		String fromPath = Utils.replaceAll(from.getAbsolutePath(), File.separator, PATH_SEP);

		String[] targetTokens = Utils.getTokens(targetPath, PATH_SEP);
		String[] fromTokens = Utils.getTokens(fromPath, PATH_SEP);
		int index = -1;
		for (int i = 0; i < fromTokens.length; i++)
			if (fromTokens[i].equals(targetTokens[i]))
				index = i;
			else
				break;

		StringBuilder sb = new StringBuilder();
		for (int i = index + 1; i < fromTokens.length; i++)
			sb.append(".." + PATH_SEP); //$NON-NLS-1$

		for (int i = index + 1; i < targetTokens.length; i++)
			if (i != targetTokens.length - 1)
				sb.append(targetTokens[i] + PATH_SEP);
			else
				sb.append(targetTokens[i]);
		return sb.toString();
	}

	/**
	 * This method will be called for create a backup file.
	 * 
	 * @param file
	 *            target file
	 * @return File backup file whose filename consists of
	 *         "hogehoge.yyyyMMddHHmmss.ext" or "hogehoge.yyyyMMddHHmmss".
	 */
	public static File getSimpleDataFormattedFile(File file) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss"); //$NON-NLS-1$
		String date = df.format(new Date());
		String filename = file.getName();
		int index = filename.lastIndexOf("."); //$NON-NLS-1$
		if (index != -1)
			filename = filename.substring(0, index) + "." + date + "." + filename.substring(index + 1); //$NON-NLS-1$ //$NON-NLS-2$
		else
			filename = filename + "." + date; //$NON-NLS-1$
		File dest = new File(file.getParentFile(), filename);
		return dest;
	}

	public static String[] getTokens(String msg, String delim) {
		return getTokens(msg, delim, false);
	}

	public static String[] getTokens(String msg, String delim, boolean returnDelims) {
		StringTokenizer targetST = new StringTokenizer(msg, delim, returnDelims);
		String[] tokens = new String[targetST.countTokens()];
		ArrayList<String> list = new ArrayList<>(targetST.countTokens());
		while (targetST.hasMoreTokens()) {
			list.add(targetST.nextToken());
		}
		list.toArray(tokens);
		return tokens;
	}

	public static URL getUrl(String protocol, String host, String file) throws MalformedURLException {// throws
																										// ManipulatorException
		file = Utils.replaceAll(file, File.separator, "/"); //$NON-NLS-1$
		return new URL(protocol, host, file);
	}

	public static URL getUrlInFull(String path, URL from) throws MalformedURLException {// throws ManipulatorException
		Utils.checkFullUrl(from, "from"); //$NON-NLS-1$
		path = Utils.replaceAll(path, File.separator, "/"); //$NON-NLS-1$
		// System.out.println("from.toExternalForm()=" + from.toExternalForm());
		String fromSt = Utils.removeLastCh(from.toExternalForm(), '/');
		// System.out.println("fromSt=" + fromSt);
		if (path.startsWith("/")) { //$NON-NLS-1$
			String fileSt = from.getFile();
			return new URL(fromSt.substring(0, fromSt.lastIndexOf(fileSt) - 1) + path);
		}
		return new URL(fromSt + "/" + path); //$NON-NLS-1$
	}

	private static Dictionary<String, String> manifestToProperties(Map<String, String> d) {
		Dictionary<String, String> result = new Hashtable<>();
		for (String key : d.keySet()) {
			result.put(key, d.get(key));
		}
		return result;
	}

	/**
	 * Just used for debug.
	 * 
	 * @param ps
	 *            printstream
	 * @param name
	 *            name of properties
	 * @param props
	 *            properties whose keys and values will be printed out.
	 */
	public static void printoutProperties(PrintStream ps, String name, Properties props) {
		if (props == null || props.size() == 0) {
			ps.println("Props(" + name + ") is empty"); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		ps.println("Props(" + name + ")="); //$NON-NLS-1$ //$NON-NLS-2$
		for (Enumeration<Object> enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			ps.print("\tkey=" + key); //$NON-NLS-1$
			ps.println("\tvalue=" + props.getProperty(key)); //$NON-NLS-1$
		}
	}

	public static String removeLastCh(String target, char ch) {
		while (target.charAt(target.length() - 1) == ch) {
			target = target.substring(0, target.length() - 1);
		}
		return target;
	}

	public static String replaceAll(String st, String oldSt, String newSt) {
		if (oldSt.equals(newSt))
			return st;
		int index = -1;
		while ((index = st.indexOf(oldSt)) != -1) {
			st = st.substring(0, index) + newSt + st.substring(index + oldSt.length());
		}
		return st;
	}

	/**
	 * Sort by increasing order of startlevels.
	 * 
	 * @param bInfos
	 *            array of BundleInfos to be sorted.
	 * @param initialBSL
	 *            initial bundle start level to be used.
	 * @return sorted array of BundleInfos
	 */
	public static BundleInfo[] sortBundleInfos(BundleInfo[] bInfos, int initialBSL) {
		SortedMap<Integer, List<BundleInfo>> bslToList = new TreeMap<>();
		for (BundleInfo bInfo : bInfos) {
			Integer sL = Integer.valueOf(bInfo.getStartLevel());
			if (sL.intValue() == BundleInfo.NO_LEVEL)
				sL = Integer.valueOf(initialBSL);
			List<BundleInfo> list = bslToList.get(sL);
			if (list == null) {
				list = new LinkedList<>();
				bslToList.put(sL, list);
			}
			list.add(bInfo);
		}

		// bslToList is sorted by the key (StartLevel).
		List<BundleInfo> bundleInfoList = new LinkedList<>();
		for (Integer sL : bslToList.keySet()) {
			List<BundleInfo> list = bslToList.get(sL);
			for (BundleInfo bInfo : list) {
				bundleInfoList.add(bInfo);
			}
		}
		return getBundleInfosFromList(bundleInfoList);
	}

	/**
	 * get String representing the given properties.
	 * 
	 * @param name
	 *            name of properties
	 * @param props
	 *            properties whose keys and values will be printed out.
	 */
	public static String toStringProperties(String name, Properties props) {
		if (props == null || props.size() == 0) {
			return "Props(" + name + ") is empty\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Props(" + name + ") is \n"); //$NON-NLS-1$ //$NON-NLS-2$
		for (Enumeration<Object> enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			sb.append("\tkey=" + key + "\tvalue=" + props.getProperty(key) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return sb.toString();
	}

}
