/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.utils;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.equinox.frameworkadmin.BundleInfo;

public class Utils {
	private final static String PATH_SEP = "/";
	private static final String[] EMPTY_STRING_ARRAY = new String[] {};

	/**
	 * Overwrite all properties of from to the properties of to. Return the result of to.
	 * 
	 * @param to Properties whose keys and values of other Properties will be appended to.
	 * @param from Properties whose keys and values will be set to the other properties.
	 * @return Properties as a result of this method. 
	 */
	public static Properties appendProperties(Properties to, Properties from) {
		if (from != null) {
			if (to == null)
				to = new Properties();
			//			printoutProperties(System.out, "to", to);
			//			printoutProperties(System.out, "from", from);

			for (Enumeration enumeration = from.keys(); enumeration.hasMoreElements();) {
				String key = (String) enumeration.nextElement();
				to.setProperty(key, from.getProperty(key));
			}
		}
		//		printoutProperties(System.out, "to", to);
		return to;
	}

	//Return a dictionary representing a manifest. The data may result from plugin.xml conversion  
	private static Dictionary basicLoadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		ZipFile jarFile = null;
		try {
			String fileExtention = bundleLocation.getName();
			fileExtention = fileExtention.substring(fileExtention.lastIndexOf('.') + 1);
			if ("jar".equalsIgnoreCase(fileExtention) && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				manifestStream = new BufferedInputStream(new FileInputStream(new File(bundleLocation, JarFile.MANIFEST_NAME)));
			}
		} catch (IOException e) {
			//ignore
		}
		Dictionary manifest = null;

		//It is not a manifest, but a plugin or a fragment

		if (manifestStream != null) {
			try {
				Manifest m = new Manifest(manifestStream);
				manifest = manifestToProperties(m.getMainAttributes());
			} catch (IOException ioe) {
				return null;
			} finally {
				try {
					manifestStream.close();
				} catch (IOException e1) {
					//Ignore
				}
				try {
					if (jarFile != null)
						jarFile.close();
				} catch (IOException e2) {
					//Ignore
				}
			}
		}
		return manifest;
	}

	public static void checkAbsoluteDir(File file, String dirName) throws IllegalArgumentException {
		if (file == null)
			throw new IllegalArgumentException(dirName + " is null");
		if (!file.isAbsolute())
			throw new IllegalArgumentException(dirName + " is not absolute path. file=" + file.getAbsolutePath());
		if (!file.isDirectory())
			throw new IllegalArgumentException(dirName + " is not directory. file=" + file.getAbsolutePath());
	}

	public static void checkAbsoluteFile(File file, String dirName) {//throws ManipulatorException {
		if (file == null)
			throw new IllegalArgumentException(dirName + " is null");
		if (!file.isAbsolute())
			throw new IllegalArgumentException(dirName + " is not absolute path. file=" + file.getAbsolutePath());
		if (file.isDirectory())
			throw new IllegalArgumentException(dirName + " is not file but directory");
	}

	public static URL checkFullUrl(URL url, String urlName) throws IllegalArgumentException {//throws ManipulatorException {
		if (url == null)
			throw new IllegalArgumentException(urlName + " is null");
		if (!url.getProtocol().endsWith("file"))
			return url;
		File file = new File(url.getFile());
		if (!file.isAbsolute())
			throw new IllegalArgumentException(urlName + "(" + url + ") does not have absolute path");
		if (file.getAbsolutePath().startsWith(PATH_SEP))
			return url;
		try {
			return getUrl("file", null, PATH_SEP + file.getAbsolutePath());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(urlName + "(" + "file:" + PATH_SEP + file.getAbsolutePath() + ") is not fully quallified");
		}
	}

	public static void createParentDir(File file) throws IOException {
		File parent = file.getParentFile();
		if (parent == null)
			return;
		parent.mkdirs();
	}

	/**
	 * Deletes the given file recursively, adding failure info to
	 * the provided status object.  The filePath is passed as a parameter
	 * to optimize java.io.File object creation.
	 */
	// Implementation taken from the Eclipse File sytem bundle class LocalFile.
	//  TODO consider putting back the progress and cancelation support.
	private static boolean internalDelete(File target, String pathToDelete) {
		//first try to delete - this should succeed for files and symbolic links to directories
		if (target.delete() || !target.exists())
			return true;
		if (target.isDirectory()) {
			String[] list = target.list();
			if (list == null)
				list = EMPTY_STRING_ARRAY;
			int parentLength = pathToDelete.length();
			boolean failedRecursive = false;
			for (int i = 0, imax = list.length; i < imax; i++) {
				//optimized creation of child path object
				StringBuffer childBuffer = new StringBuffer(parentLength + list[i].length() + 1);
				childBuffer.append(pathToDelete);
				childBuffer.append(File.separatorChar);
				childBuffer.append(list[i]);
				String childName = childBuffer.toString();
				// try best effort on all children so put logical OR at end
				failedRecursive = !internalDelete(new java.io.File(childName), childName) || failedRecursive;
			}
			try {
				// don't try to delete the root if one of the children failed
				if (!failedRecursive && target.delete())
					return true;
			} catch (Exception e) {
				// we caught a runtime exception so log it
				return false;
			}
		}
		//		message = NLS.bind(Messages.couldnotDelete, target.getAbsolutePath());
		return false;
	}

	public static void deleteDir(File target) throws IOException {
		internalDelete(target, target.getAbsolutePath());
		throw new IOException("Fail to delete Dir(" + target.getAbsolutePath() + ")");
	}

	/**
	 * First, it replaces File.seperator of relativePath to "/".
	 * If relativePath is in URL format, return its URL.
	 * Otherwise, create absolute URL based on the baseUrl.
	 * 
	 * @param relativePath
	 * @param baseUrl
	 * @return URL 
	 * @throws MalformedURLException
	 */
	public static URL formatUrl(String relativePath, URL baseUrl) throws MalformedURLException {//throws ManipulatorException {
		relativePath = Utils.replaceAll(relativePath, File.separator, "/");
		URL url = null;
		try {
			url = new URL(relativePath);
			if (url.getProtocol().equals("file"))
				if (!(new File(url.getFile())).isAbsolute())
					url = getUrlInFull(relativePath, baseUrl);
			return url;
		} catch (MalformedURLException e) {
			return getUrlInFull(relativePath, baseUrl);
		}
	}

	public static BundleInfo[] getBundleInfosFromList(List list) {
		if (list == null)
			return new BundleInfo[0];
		BundleInfo[] ret = new BundleInfo[list.size()];
		list.toArray(ret);
		return ret;
	}

	public static String[] getClauses(String header) {
		StringTokenizer token = new StringTokenizer(header, ",");
		List list = new LinkedList();
		while (token.hasMoreTokens()) {
			list.add(token.nextToken());
		}
		String[] ret = new String[list.size()];
		list.toArray(ret);
		return ret;
	}

	public static String[] getClausesManifestMainAttributes(String location, String name) {
		return getClauses(getManifestMainAttributes(location, name));
	}

	public static String getManifestMainAttributes(String location, String name) {
		Dictionary manifest = Utils.getOSGiManifest(location);
		if (manifest == null)
			throw new RuntimeException("Unable to locate bundle manifest: " + location);
		return (String) manifest.get(name);
	}

	public static Dictionary getOSGiManifest(String location) {
		if (location.startsWith("file:") && !location.endsWith(".jar"))
			return basicLoadManifest(new File(location.substring("file:".length())));

		try {
			URL url = new URL("jar:" + location + "!/");
			JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
			Manifest manifest = jarConnection.getManifest();
			Attributes attributes = manifest.getMainAttributes();
			//			Set set = attributes.keySet();
			Hashtable table = new Hashtable();
			for (java.util.Iterator ite = attributes.keySet().iterator(); ite.hasNext();) {
				//				Object obj =  ite.next();
				//System.out.println(obj.getClass().getName());

				String key = (String) ite.next().toString();
				// While table contains non OSGiManifest, it doesn't matter.
				table.put(key, attributes.getValue(key));
				//	System.out.println("key=" + key + " value=" + value);
			}
			//	System.out.println("");
			try {
				jarConnection.getJarFile().close();
			} catch (IOException e) {
				//Ignore
			}
			return table;
		} catch (MalformedURLException e1) {
			// TODO log
			System.err.println("location=" + location);
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO log
			System.err.println("location=" + location);
			e.printStackTrace();
		}
		return null;
	}

	public static String getPathFromClause(String clause) {
		if (clause == null)
			return null;
		if (clause.indexOf(";") != -1)
			clause = clause.substring(0, clause.indexOf(";"));
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

		StringBuffer sb = new StringBuffer();
		for (int i = index + 1; i < fromTokens.length; i++)
			sb.append(".." + PATH_SEP);

		for (int i = index + 1; i < targetTokens.length; i++)
			if (i != targetTokens.length - 1)
				sb.append(targetTokens[i] + PATH_SEP);
			else
				sb.append(targetTokens[i]);
		return sb.toString();
	}

	public static String getRelativePath(URL target, URL from) throws IllegalArgumentException {

		if (!target.getProtocol().equals(from.getProtocol()))
			throw new IllegalArgumentException("Protocols of target(=" + target + ") and from(=" + from + ") does NOT equal");

		if (target.getHost() != null && target.getHost().length() != 0) {
			//System.out.println("target.getHost()=" + target.getHost());
			if (from.getHost() != null && from.getHost().length() != 0) {
				if (!target.getHost().equals(from.getHost()))
					throw new IllegalArgumentException("Hosts of target(=" + target + ") and from(=" + from + ") does NOT equal");
				if (target.getPort() != (from.getPort()))
					throw new IllegalArgumentException("Ports of target(=" + target + ") and from(=" + from + ") does NOT equal");
			} else
				throw new IllegalArgumentException("While Host of target(=" + target + ") is set, Host of from is null.target.getHost()=" + target.getHost());
		} else if (from.getHost() != null && from.getHost().length() != 0)
			throw new IllegalArgumentException("While Host of from(=" + from + ") is set, Host of target is null");

		String targetPath = target.getFile();
		String fromPath = from.getFile();

		String[] targetTokens = Utils.getTokens(targetPath, PATH_SEP);
		String[] fromTokens = Utils.getTokens(fromPath, PATH_SEP);
		int index = -1;
		for (int i = 0; i < fromTokens.length; i++)
			if (fromTokens[i].equals(targetTokens[i]))
				index = i;
			else
				break;

		StringBuffer sb = new StringBuffer();
		for (int i = index + 1; i < fromTokens.length; i++)
			sb.append(".." + PATH_SEP);

		for (int i = index + 1; i < targetTokens.length; i++)
			if (i != targetTokens.length - 1)
				sb.append(targetTokens[i] + PATH_SEP);
			else
				sb.append(targetTokens[i]);
		return sb.toString();
	}

	//	public static URL getAbsoluteUrl(String relativePath, URL baseUrl) throws FwLauncherException {
	//		relativePath = Utils.replaceAll(relativePath, File.separator, "/");
	//		try {
	//			return new URL(baseUrl, relativePath);
	//		} catch (MalformedURLException e) {
	//			throw new FwLauncherException("Absolute URL cannot be created. \nrelativePath=" + relativePath + ",baseUrl=" + baseUrl, e, FwLauncherException.URL_FORMAT_ERROR);
	//		}
	//	}

	//	public static void setProperties(Properties to, Properties from, String key) {
	//		if (from != null) {
	//			String value = from.getProperty(key);
	//			if (value != null) {
	//				if (to != null)
	//					to = new Properties();
	//				to.setProperty(key, value);
	//			}
	//		}
	//	}

	//	public static int getIntProperties(Properties props, String key) {//throws ManipulatorException {
	//		if (props == null)
	//			throw new IllegalArgumentException("props == null");
	//		String value = null;
	//		try {
	//			value = props.getProperty(key);
	//			return Integer.parseInt(value);
	//		} catch (NumberFormatException nfe) {
	//			throw new ManipulatorException("key=" + key + ",value=" + value, nfe, ManipulatorException.OTHERS);
	//		}
	//	}

	/**
	 * This method will be called for create a backup file.
	 * 
	 * @param file target file
	 * @return File backup file whose filename consists of "hogehoge.yyyyMMddHHmmss.ext" or 
	 * 	"hogehoge.yyyyMMddHHmmss".
	 */
	public static File getSimpleDataFormattedFile(File file) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = df.format(new Date());
		String filename = file.getName();
		int index = filename.lastIndexOf(".");
		if (index != -1)
			filename = filename.substring(0, index) + "." + date + "." + filename.substring(index + 1);
		else
			filename = filename + "." + date;
		File dest = new File(file.getParentFile(), filename);
		return dest;
	}

	public static String[] getTokens(String msg, String delim) {
		return getTokens(msg, delim, false);
	}

	public static String[] getTokens(String msg, String delim, boolean returnDelims) {
		StringTokenizer targetST = new StringTokenizer(msg, delim, returnDelims);
		String[] tokens = new String[targetST.countTokens()];
		ArrayList list = new ArrayList(targetST.countTokens());
		while (targetST.hasMoreTokens()) {
			list.add(targetST.nextToken());
		}
		list.toArray(tokens);
		return tokens;
	}

	public static URL getUrl(String protocol, String host, String file) throws MalformedURLException {// throws ManipulatorException {
		file = Utils.replaceAll(file, File.separator, "/");
		return new URL(protocol, host, file);
	}

	public static URL getUrlInFull(String path, URL from) throws MalformedURLException {//throws ManipulatorException {
		Utils.checkFullUrl(from, "from");
		path = Utils.replaceAll(path, File.separator, "/");
		//System.out.println("from.toExternalForm()=" + from.toExternalForm());
		String fromSt = Utils.removeLastCh(from.toExternalForm(), '/');
		//System.out.println("fromSt=" + fromSt);
		if (path.startsWith("/")) {
			String fileSt = from.getFile();
			return new URL(fromSt.substring(0, fromSt.lastIndexOf(fileSt) - 1) + path);
		}
		return new URL(fromSt + "/" + path);
	}

	private static Properties manifestToProperties(Attributes d) {
		Iterator iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}

	/**
	 * Just used for debug.
	 * 
	 * @param ps printstream
	 * @param name name of properties 
	 * @param props properties whose keys and values will be printed out.
	 */
	public static void printoutProperties(PrintStream ps, String name, Properties props) {
		if (props == null || props.size() == 0) {
			ps.println("Props(" + name + ") is empty");
			return;
		}
		ps.println("Props(" + name + ")=");
		for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			ps.print("\tkey=" + key);
			ps.println("\tvalue=" + props.getProperty(key));
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

	public static String shrinkPath(String target) {
		String targetPath = Utils.replaceAll(target, File.separator, PATH_SEP);
		String[] targetTokens = Utils.getTokens(targetPath, PATH_SEP);
		//String[] fromTokens = Utils.getTokens(fromPath, PATH_SEP);
		for (int i = 0; i < targetTokens.length; i++)
			if (targetTokens[i].equals("")) {
				targetTokens[i] = null;
			} else if (targetTokens[i].equals(".")) {
				targetTokens[i] = null;
			} else if (targetTokens[i].equals("..")) {
				int id = i - 1;
				while (targetTokens[id] == null) {
					id--;
				}
				targetTokens[id] = null;
			}

		StringBuffer sb = new StringBuffer();
		if (targetPath.startsWith(PATH_SEP))
			sb.append(PATH_SEP);
		for (int i = 0; i < targetTokens.length; i++)
			if (targetTokens[i] != null)
				sb.append(targetTokens[i] + PATH_SEP);
		String ret = sb.toString();
		if (!targetPath.endsWith(PATH_SEP))
			ret = ret.substring(0, ret.lastIndexOf(PATH_SEP));
		return ret;
	}

	/**
	 * Sort by increasing order of startlevels.
	 * 
	 * @param bInfos array of BundleInfos to be sorted.
	 * @param initialBSL initial bundle start level to be used.
	 * @return sorted array of BundleInfos
	 */
	public static BundleInfo[] sortBundleInfos(BundleInfo[] bInfos, int initialBSL) {
		SortedMap bslToList = new TreeMap();
		for (int i = 0; i < bInfos.length; i++) {
			Integer sL = new Integer(bInfos[i].getStartLevel());
			if (sL.intValue() == BundleInfo.NO_LEVEL)
				sL = new Integer(initialBSL);
			List list = (List) bslToList.get(sL);
			if (list == null) {
				list = new LinkedList();
				bslToList.put(sL, list);
			}
			list.add(bInfos[i]);
		}

		// bslToList is sorted by the key (StartLevel).
		List bundleInfoList = new LinkedList();
		for (Iterator ite = bslToList.keySet().iterator(); ite.hasNext();) {
			Integer sL = (Integer) ite.next();
			List list = (List) bslToList.get(sL);
			for (Iterator ite2 = list.iterator(); ite2.hasNext();) {
				BundleInfo bInfo = (BundleInfo) ite2.next();
				bundleInfoList.add(bInfo);
			}
		}
		return getBundleInfosFromList(bundleInfoList);
	}

	/**
	 * get String representing the given properties.
	 * 
	 * @param name name of properties 
	 * @param props properties whose keys and values will be printed out.
	 */
	public static String toStringProperties(String name, Properties props) {
		if (props == null || props.size() == 0) {
			return "Props(" + name + ") is empty\n";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("Props(" + name + ") is \n");
		for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			sb.append("\tkey=" + key + "\tvalue=" + props.getProperty(key) + "\n");
		}
		return sb.toString();
	}

	public static void validateUrl(URL url) {//throws ManipulatorException {
		try {//test
			URLConnection connection = url.openConnection();
			connection.connect();
		} catch (IOException e) {
			throw new IllegalArgumentException("URL(" + url + ") cannot be connected.");
		}
	}

}
