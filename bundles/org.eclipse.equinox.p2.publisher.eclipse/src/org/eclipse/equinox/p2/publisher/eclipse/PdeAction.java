/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.*;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * This publisher action can handle a PDE project root and transform this into
 * InstallableUnits that can be used to provision a system that is used for
 * <b>building</b>, so this action will usually used in the context of Tycho,
 * Oomph or similar.
 */
@SuppressWarnings("restriction")
public class PdeAction extends AbstractPublisherAction {

	private final File basedir;

	public PdeAction(File basedir) {
		this.basedir = basedir;
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
		File buildProperties = new File(basedir, "build.properties"); //$NON-NLS-1$
		if (buildProperties.isFile()) {
			new BuildPropertiesAction(buildProperties).perform(publisherInfo, results, subMonitor.split(1));
		}
		subMonitor.setWorkRemaining(2);
		File pdeBnd = new File(basedir, "pde.bnd"); //$NON-NLS-1$
		if (pdeBnd.isFile()) {
			new BndInstructionsAction(pdeBnd).perform(publisherInfo, results, subMonitor.split(1));
		}
		subMonitor.setWorkRemaining(1);
		File manifest = getManifestFile();
		if (manifest.isFile()) {
			Attributes mainAttributes;
			try (FileInputStream stream = new FileInputStream(manifest)) {
				mainAttributes = new Manifest(stream).getMainAttributes();
			} catch (IOException e) {
				return Status.error("Can't parse manifest", e); //$NON-NLS-1$
			}
			CaseInsensitiveDictionaryMap<String, String> headers = new CaseInsensitiveDictionaryMap<>(
					mainAttributes.size());
			Set<Entry<Object, Object>> entrySet = mainAttributes.entrySet();
			for (Entry<Object, Object> entry : entrySet) {
				headers.put(entry.getKey().toString(), entry.getValue().toString());
			}
			BundleDescription bundleDescription = BundlesAction.createBundleDescription(headers, basedir);
			new BundlesAction(new BundleDescription[] { bundleDescription }).perform(publisherInfo, results,
					subMonitor.split(1));
		}
		return Status.OK_STATUS;
	}

	private File getManifestFile() {
		File pdePreferences = new File(basedir, ".settings/org.eclipse.pde.core.prefs"); //$NON-NLS-1$
		if (pdePreferences.isFile()) {
			Properties properties = new Properties();
			try {
				properties.load(new FileInputStream(pdePreferences));
			} catch (IOException e) {
				// if loading fails, handle this as if we have no special settings...
			}
			String property = properties.getProperty("BUNDLE_ROOT_PATH"); //$NON-NLS-1$
			if (property != null) {
				File file = new File(new File(basedir, property), JarFile.MANIFEST_NAME);
				if (file.isFile()) {
					// even though configured, the manifest might not be there (e.g. not generated
					// yet) in such case, still fall back to the default location, it can only
					// happen that noting is there as well so things are not getting worser this
					// way...
					return file;
				}
			}
		}
		// return (possibly non existent) default location...
		return new File(basedir, JarFile.MANIFEST_NAME);
	}

}
