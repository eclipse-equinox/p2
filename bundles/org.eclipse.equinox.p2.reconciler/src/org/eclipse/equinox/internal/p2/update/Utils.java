/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.*;

/**
 * 
 * @since 1.0
 */
public class Utils {
	public static boolean isWindows = System.getProperty("os.name").startsWith("Win"); //$NON-NLS-1$ //$NON-NLS-2$	
	private static boolean init = false;
	private static boolean useEnc = true;

	/*
	 * Copied from UpdateURLDecoder v1.4 in org.eclipse.update.configurator.
	 */
	public static String decode(String s, String enc) throws UnsupportedEncodingException {
		if (!init) {
			init = true;
			try {
				return URLDecoder.decode(s, enc);
			} catch (NoSuchMethodError e) {
				useEnc = false;
			}
		}
		return useEnc ? URLDecoder.decode(s, enc) : URLDecoder.decode(s);
	}

	/*
	 * Copied from Utils v1.32 in org.eclipse.update.configurator.
	 * 
	 * Ensures file: URLs on Windows have the right form (i.e. '/' as segment separator, drive letter in lower case, etc)
	 */
	public static String canonicalizeURL(String url) {
		if (!(isWindows && url.startsWith("file:"))) //$NON-NLS-1$
			return url;
		try {
			String path = new URL(url).getPath();
			// normalize to not have leading / so we can check the form
			File file = new File(path);
			path = file.toString().replace('\\', '/');
			if (Character.isUpperCase(path.charAt(0))) {
				char[] chars = path.toCharArray();
				chars[0] = Character.toLowerCase(chars[0]);
				path = new String(chars);
				return new File(path).toURL().toExternalForm();
			}
		} catch (MalformedURLException e) {
			// default to original url
		}
		return url;
	}

}
