/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.equinox.internal.utils.state;

import java.io.File;
import java.io.IOException;
import java.net.*;

import org.osgi.framework.Version;

public class BundleSearch {
	private static final String REFERENCE_PROTOCOL = "reference"; //$NON-NLS-1$
	private static final String FILE_SCHEME = "file:"; //$NON-NLS-1$

	/**
	 * Searches for the given target directory immediately under
	 * the given start location.  If one is found then this location is returned; 
	 * otherwise an exception is thrown.
	 * 
	 * @return the location where target directory was found
	 * @param start the location to begin searching
	 */
	private static String searchFor(final String target, String start) {
		String[] candidates = new File(start).list();
		if (candidates == null)
			return null;
		File result = null;
		Version maxVersion = null;
		for (int i = 0; i < candidates.length; i++) {
			File candidate = new File(start, candidates[i]);
			if (!candidate.getName().equals(target) && !candidate.getName().startsWith(target + "_")) //$NON-NLS-1$
				continue;
			String name = candidate.getName();
			if (name.endsWith(".jar"))
				name = name.substring(0, name.length() - 4);

			String version = ""; //$NON-NLS-1$ // Note: directory with version suffix is always > than directory without version suffix
			int index = name.indexOf('_');
			if (index != -1)
				version = name.substring(index + 1);

			try {
				Version currentVersion = new Version(version);
				if (maxVersion == null) {
					result = candidate;
					maxVersion = currentVersion;
				} else {
					if (currentVersion.compareTo(maxVersion) < 0) {
						result = candidate;
						maxVersion = currentVersion;
					}
				}
			} catch (IllegalArgumentException e) {
				result = candidate;
			}
		}
		if (result == null)
			return null;
		return result.getAbsolutePath().replace(File.separatorChar, '/') + (result.isDirectory() ? "/" : ""); //$NON-NLS-1$
	}

	public static URL searchForBundle(String name, String parent) throws MalformedURLException {
		URL url = null;
		File fileLocation = null;
		boolean reference = false;
		if (parent != null) {
			try {
				new URL(name);
				url = new URL(new File(parent).toURL(), name);
			} catch (MalformedURLException e) {
				// TODO this is legacy support for non-URL names.  It should be removed eventually.
				// if name was not a URL then construct one.  
				// Assume it should be a reference and htat it is relative.  This support need not 
				// be robust as it is temporary..
				File child = new File(name);
				fileLocation = child.isAbsolute() ? child : new File(parent, name);
				url = new URL(REFERENCE_PROTOCOL, null, fileLocation.toURL().toExternalForm());
				reference = true;
			}
		}

		// if the name was a URL then see if it is relative.  If so, insert syspath.
		if (!reference) {
			URL baseURL = url;
			// if it is a reference URL then strip off the reference: and set base to the file:...
			if (url.getProtocol().equals(REFERENCE_PROTOCOL)) {
				reference = true;
				String baseSpec = url.getFile();
				if (baseSpec.startsWith(FILE_SCHEME)) {
					File child = new File(baseSpec.substring(5));
					baseURL = child.isAbsolute() ? child.toURL() : new File(parent, child.getPath()).toURL();
				} else
					baseURL = new URL(baseSpec);
			}

			fileLocation = new File(baseURL.getFile());
			// if the location is relative, prefix it with the parent
			if (!fileLocation.isAbsolute())
				fileLocation = new File(parent, fileLocation.toString());
		}
		// If the result is a reference then search for the real result and 
		// reconstruct the answer.
		if (reference) {
			String result = searchFor(fileLocation.getName(), new File(fileLocation.getParent()).getAbsolutePath());
			if (result != null)
				url = new URL(REFERENCE_PROTOCOL, null, FILE_SCHEME + result);
			else
				return null;
		}

		// finally we have something worth trying	
		try {
			URLConnection result = url.openConnection();
			result.connect();
			return url;
		} catch (IOException e) {
			//			int i = location.lastIndexOf('_');
			//			return i == -1? location : location.substring(0, i);
			return null;
		}
	}
}
