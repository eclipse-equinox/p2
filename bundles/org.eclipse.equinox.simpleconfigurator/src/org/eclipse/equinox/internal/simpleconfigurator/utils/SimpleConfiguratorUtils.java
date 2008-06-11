/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class SimpleConfiguratorUtils {

	public static List readConfiguration(URL url) throws IOException {
		List bundles = new ArrayList();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));

			String line;
			try {
				URL baseUrl = new URL(url, "./"); //$NON-NLS-1$
				while ((line = r.readLine()) != null) {
					if (line.startsWith("#")) //$NON-NLS-1$
						continue;
					line = line.trim();// symbolicName,version,location,startlevel,expectedState
					if (line.length() == 0)
						continue;

					// (expectedState is an integer).
					if (line.startsWith(SimpleConfiguratorConstants.PARAMETER_BASEURL + "=")) { //$NON-NLS-1$
						String baseUrlSt = line.substring((SimpleConfiguratorConstants.PARAMETER_BASEURL + "=").length()); //$NON-NLS-1$
						if (!baseUrlSt.endsWith("/")) //$NON-NLS-1$
							baseUrlSt += "/"; //$NON-NLS-1$
						baseUrl = new URL(url, baseUrlSt);
						continue;
					}
					StringTokenizer tok = new StringTokenizer(line, ",", true); //$NON-NLS-1$
					String symbolicName = tok.nextToken();
					if (symbolicName.equals(",")) //$NON-NLS-1$
						symbolicName = null;
					else
						tok.nextToken(); // ,

					String version = tok.nextToken();
					if (version.equals(",")) //$NON-NLS-1$
						version = null;
					else
						tok.nextToken(); // ,

					String urlSt = tok.nextToken();
					if (urlSt.equals(",")) { //$NON-NLS-1$
						if (symbolicName != null && version != null)
							urlSt = symbolicName + "_" + version + ".jar"; //$NON-NLS-1$ //$NON-NLS-2$
						else
							urlSt = null;
					} else
						tok.nextToken(); // ,
					try {
						Utils.buildURL(urlSt);
					} catch (MalformedURLException e) {
						urlSt = Utils.getUrlInFull(urlSt, baseUrl).toExternalForm();
					}

					int sl = Integer.parseInt(tok.nextToken().trim());
					tok.nextToken(); // ,
					boolean markedAsStarted = Boolean.valueOf(tok.nextToken()).booleanValue();
					// URL urlBundle = null;
					// try {
					// urlBundle = Utils.buildURL(urlSt);
					// } catch (MalformedURLException e) {
					// urlBundle = Utils.getFullUrl(urlSt, baseUrl);
					// }

					BundleInfo bInfo = new BundleInfo(symbolicName, version, urlSt, sl, markedAsStarted);
					bundles.add(bInfo);
				}
			} finally {
				try {
					r.close();
				} catch (IOException ex) {
					// ignore
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			// TODO log something
			// bundleInfos = NULL_BUNDLEINFOS;
		}
		return bundles;
	}

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
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
