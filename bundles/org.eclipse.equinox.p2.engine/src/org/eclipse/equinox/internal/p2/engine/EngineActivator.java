/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc - fragments support added
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class EngineActivator implements BundleActivator {
	private static BundleContext context;
	/**
	 * as for now it is exactly the same variable as in simpleconfigurator Activator
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public static String EXTENSIONS = System.getProperty("p2.fragments"); //$NON-NLS-1$
	public static boolean EXTENDED = EXTENSIONS != null;
	private static final String LINK_KEY = "link"; //$NON-NLS-1$
	private static final String LINK_FILE_EXTENSION = ".link"; //$NON-NLS-1$
	private static final Set<File> reportedExtensions = Collections.synchronizedSet(new HashSet<>(0));

	public static final String ID = "org.eclipse.equinox.p2.engine"; //$NON-NLS-1$

	/**
	 * System property describing the profile registry file format
	 */
	public static final String PROP_PROFILE_FORMAT = "eclipse.p2.profileFormat"; //$NON-NLS-1$

	/**
	 * Value for the PROP_PROFILE_FORMAT system property specifying raw XML file
	 * format (used in p2 until and including 3.5.0 release).
	 */
	public static final String PROFILE_FORMAT_UNCOMPRESSED = "uncompressed"; //$NON-NLS-1$

	/**
	 * System property specifying how the engine should handle unsigned artifacts.
	 * If this property is undefined, the default value is assumed to be "prompt".
	 */
	public static final String PROP_UNSIGNED_POLICY = "eclipse.p2.unsignedPolicy"; //$NON-NLS-1$

	/**
	 * System property value specifying that the engine should prompt for confirmation
	 * when installing unsigned artifacts.
	 */
	public static final String UNSIGNED_PROMPT = "prompt"; //$NON-NLS-1$

	/**
	 * System property value specifying that the engine should fail when an attempt
	 * is made to install unsigned artifacts.
	 */
	public static final String UNSIGNED_FAIL = "fail"; //$NON-NLS-1$

	/**
	 * System property value specifying that the engine should silently allow unsigned
	 * artifacts to be installed.
	 */
	public static final String UNSIGNED_ALLOW = "allow"; //$NON-NLS-1$

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * This property indicates repositories that are passed via the fragments mechanism.
	 */
	public static final String P2_FRAGMENT_PROPERTY = "p2.fragment"; //$NON-NLS-1$

	@Override
	public void start(BundleContext aContext) throws Exception {
		EngineActivator.context = aContext;
	}

	@Override
	public void stop(BundleContext aContext) throws Exception {
		EngineActivator.context = null;
	}

	public static File[] getExtensionsDirectories() {
		List<File> files = new ArrayList<>(0);
		if (EXTENSIONS != null) {
			String[] locationToCheck = EXTENSIONS.split(","); //$NON-NLS-1$
			for (String location : locationToCheck) {
				try {
					files.addAll(getInfoFilesFromLocation(location));
				} catch (FileNotFoundException e) {
					LogHelper.log(new Status(IStatus.ERROR, ID, NLS.bind(Messages.EngineActivator_0, location), e));
				} catch (IOException e) {
					LogHelper.log(new Status(IStatus.ERROR, ID, NLS.bind(Messages.EngineActivator_0, location), e));
				} catch (URISyntaxException e) {
					LogHelper.log(new Status(IStatus.ERROR, ID, NLS.bind(Messages.EngineActivator_0, location), e));
				}
			}
		}
		return files.toArray(new File[files.size()]);
	}

	// This method must match the implementation in the SimpleConfiguratorUtils with the only difference that
	// parent folder of the metadata is returned.
	private static ArrayList<File> getInfoFilesFromLocation(String locationToCheck) throws IOException, FileNotFoundException, URISyntaxException {
		ArrayList<File> result = new ArrayList<>(1);

		File extensionsLocation = new File(locationToCheck);

		if (extensionsLocation.exists() && extensionsLocation.isDirectory()) {
			//extension location contains extensions
			File[] extensions = extensionsLocation.listFiles();
			for (File extension : extensions) {
				if (extension.isFile() && extension.getName().endsWith(LINK_FILE_EXTENSION)) {
					Properties link = new Properties();
					try (FileInputStream inStream = new FileInputStream(extension)) {
						link.load(inStream);
					}
					String newInfoName = link.getProperty(LINK_KEY);
					URI newInfoURI = new URI(newInfoName);
					File newInfoFile = null;
					if (newInfoURI.isAbsolute()) {
						newInfoFile = new File(newInfoName);
					} else {
						newInfoFile = new File(extension.getParentFile(), newInfoName);
					}
					if (newInfoFile.exists()) {
						extension = newInfoFile.getParentFile();
					}
				}

				if (extension.isDirectory()) {
					if (Files.isWritable(extension.toPath())) {
						synchronized (reportedExtensions) {
							if (!reportedExtensions.contains(extension)) {
								reportedExtensions.add(extension);
								LogHelper.log(new Status(IStatus.ERROR, ID, NLS.bind(Messages.EngineActivator_1, extension)));
							}
						}
						continue;
					}
					File[] listFiles = extension.listFiles();
					// new magic - multiple info files, f.e.
					//   egit.info (git feature)
					//   cdt.link (properties file containing link=path) to other info file
					for (File file : listFiles) {
						//if it is a info file - load it
						if (file.getName().endsWith(".info")) { //$NON-NLS-1$
							result.add(extension);
						}
						// if it is a link - dereference it
					}
				} else {
					synchronized (reportedExtensions) {
						if (!reportedExtensions.contains(extension)) {
							reportedExtensions.add(extension);
							LogHelper.log(new Status(IStatus.WARNING, ID, NLS.bind(Messages.EngineActivator_3, extension)));
						}
					}
				}
			}
		}
		return result;
	}
}
