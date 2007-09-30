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
package org.eclipse.equinox.prov.metadata.generator;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

public class BundleDescriptionFactory {
	static final String DIR = "dir";
	static final String JAR = "jar";
	static String BUNDLE_FILE_KEY = "eclipse.prov.bundle.format";

	StateObjectFactory factory;
	State state;

	public BundleDescriptionFactory(StateObjectFactory factory, State state) {
		this.factory = factory;
		this.state = state;
		//TODO find a state and a factory when not provided
	}

	public BundleDescription getBundleDescription(File bundleLocation) {
		Dictionary manifest = loadManifest(bundleLocation);
		if (manifest == null)
			return null;
		return getBundleDescription(manifest, bundleLocation);
	}

	public BundleDescription getBundleDescription(InputStream manifestStream, File bundleLocation) {
		Hashtable entries = new Hashtable();
		try {
			ManifestElement.parseBundleManifest(manifestStream, entries);
			return getBundleDescription(entries, bundleLocation);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Dictionary loadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		ZipFile jarFile = null;
		try {
			if ("jar".equalsIgnoreCase(new Path(bundleLocation.getName()).getFileExtension()) && bundleLocation.isFile()) { //$NON-NLS-1$
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

		if (manifestStream == null)
			return null;
		try {
			Dictionary manifest = manifestToProperties(new Manifest(manifestStream).getMainAttributes());
			// remember the format of the bundle as we found it
			manifest.put(BUNDLE_FILE_KEY, bundleLocation.isDirectory() ? DIR : JAR);
			return manifest;
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

	private Properties manifestToProperties(Attributes attributes) {
		Properties result = new Properties();
		for (Iterator i = attributes.keySet().iterator(); i.hasNext();) {
			Attributes.Name key = (Attributes.Name) i.next();
			result.put(key.toString(), attributes.get(key));
		}
		return result;
	}

	public BundleDescription getBundleDescription(Dictionary enhancedManifest, File bundleLocation) {
		try {
			BundleDescription descriptor = factory.createBundleDescription(state, enhancedManifest, bundleLocation != null ? bundleLocation.getAbsolutePath() : null, 1); //TODO Do we need to have a real bundle id
			descriptor.setUserObject(enhancedManifest);
			return descriptor;
		} catch (BundleException e) {
			//			IStatus status = new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_STATE_PROBLEM, NLS.bind(Messages.exception_stateAddition, enhancedManifest.get(Constants.BUNDLE_NAME)), e);
			//			BundleHelper.getDefault().getLog().log(status);
			System.err.println("An error has occured while adding the bundle" + bundleLocation != null ? bundleLocation.getAbsoluteFile() : null);
			return null;
		}
	}
}
