/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.discovery.compatibility;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.compatibility.Directory.Entry;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * @author David Green
 */
class DiscoveryRegistryStrategy extends RegistryStrategy {

	private final List<JarFile> jars = new ArrayList<JarFile>();

	private final Map<IContributor, File> contributorToJarFile = new HashMap<IContributor, File>();

	private final Map<IContributor, Entry> contributorToDirectoryEntry = new HashMap<IContributor, Entry>();

	private final Object token;

	private Map<File, Entry> bundleFileToDirectoryEntry;

	public DiscoveryRegistryStrategy(File[] storageDirs, boolean[] cacheReadOnly, Object token) {
		super(storageDirs, cacheReadOnly);
		this.token = token;
	}

	@Override
	public void onStart(IExtensionRegistry registry, boolean loadedFromCache) {
		super.onStart(registry, loadedFromCache);
		if (!loadedFromCache) {
			processDiscoveryCoreBundle(registry);
			processBundles(registry);
		}
	}

	private void processDiscoveryCoreBundle(IExtensionRegistry registry) {
		// we must add a contribution from the core bundle so that we get the
		// extension point itself
		try {
			Bundle bundle = Platform.getBundle("org.eclipse.equinox.p2.discovery.compatibility"); //$NON-NLS-1$
			IContributor contributor = new RegistryContributor(bundle.getSymbolicName(), bundle.getSymbolicName(), null, null);

			InputStream inputStream = bundle.getEntry("plugin.xml").openStream(); //$NON-NLS-1$
			try {
				registry.addContribution(inputStream, contributor, false, bundle.getSymbolicName(), null, token);
			} finally {
				inputStream.close();
			}
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

	private void processBundles(IExtensionRegistry registry) {
		if (bundleFileToDirectoryEntry == null) {
			throw new IllegalStateException();
		}
		for (java.util.Map.Entry<File, Entry> bundleFile : bundleFileToDirectoryEntry.entrySet()) {
			try {
				processBundle(registry, bundleFile.getValue(), bundleFile.getKey());
			} catch (Exception e) {
				LogHelper.log(new Status(IStatus.ERROR, DiscoveryCore.ID_PLUGIN, NLS.bind(Messages.DiscoveryRegistryStrategy_cannot_load_bundle, new Object[] {bundleFile.getKey().getName(), bundleFile.getValue().getLocation(), e.getMessage()}), e));
			}
		}
	}

	private void processBundle(IExtensionRegistry registry, Directory.Entry entry, File bundleFile) throws IOException {
		JarFile jarFile = new JarFile(bundleFile);
		jars.add(jarFile);

		ZipEntry pluginXmlEntry = jarFile.getEntry("plugin.xml"); //$NON-NLS-1$
		if (pluginXmlEntry == null) {
			throw new IOException(Messages.DiscoveryRegistryStrategy_missing_pluginxml);
		}
		IContributor contributor = new RegistryContributor(bundleFile.getName(), bundleFile.getName(), null, null);
		if (((IDynamicExtensionRegistry) registry).hasContributor(contributor)) {
			jarFile.close();
			return;
		}
		contributorToJarFile.put(contributor, bundleFile);
		contributorToDirectoryEntry.put(contributor, entry);

		ResourceBundle translationBundle = loadTranslationBundle(jarFile);

		InputStream inputStream = jarFile.getInputStream(pluginXmlEntry);
		try {
			registry.addContribution(inputStream, contributor, false, bundleFile.getPath(), translationBundle, token);
		} finally {
			inputStream.close();
		}
	}

	private ResourceBundle loadTranslationBundle(JarFile jarFile) throws IOException {
		List<String> bundleNames = computeBundleNames("plugin"); //$NON-NLS-1$
		for (String bundleName : bundleNames) {
			ZipEntry entry = jarFile.getEntry(bundleName);
			if (entry != null) {
				InputStream inputStream = jarFile.getInputStream(entry);
				try {
					PropertyResourceBundle resourceBundle = new PropertyResourceBundle(inputStream);
					return resourceBundle;
				} finally {
					inputStream.close();
				}
			}
		}
		return null;
	}

	private List<String> computeBundleNames(String baseName) {
		String suffix = ".properties"; //$NON-NLS-1$
		String name = baseName;
		List<String> bundleNames = new ArrayList<String>();
		Locale locale = Locale.getDefault();
		bundleNames.add(name + suffix);
		if (locale.getLanguage() != null && locale.getLanguage().length() > 0) {
			name = name + '_' + locale.getLanguage();
			bundleNames.add(0, name + suffix);
		}
		if (locale.getCountry() != null && locale.getCountry().length() > 0) {
			name = name + '_' + locale.getCountry();
			bundleNames.add(0, name + suffix);
		}
		if (locale.getVariant() != null && locale.getVariant().length() > 0) {
			name = name + '_' + locale.getVariant();
			bundleNames.add(0, name + suffix);
		}
		return bundleNames;
	}

	@Override
	public void onStop(IExtensionRegistry registry) {
		try {
			super.onStop(registry);
		} finally {
			for (JarFile jar : jars) {
				try {
					jar.close();
				} catch (Exception e) {
					// ignore
				}
			}
			jars.clear();
		}
	}

	/**
	 * get the jar file that corresponds to the given contributor.
	 * 
	 * @throws IllegalArgumentException
	 *             if the given contributor is unknown
	 */
	public File getJarFile(IContributor contributor) {
		File file = contributorToJarFile.get(contributor);
		if (file == null) {
			throw new IllegalArgumentException(contributor.getName());
		}
		return file;
	}

	/**
	 * get the directory entry that corresponds to the given contributor.
	 * 
	 * @throws IllegalArgumentException
	 *             if the given contributor is unknown
	 */
	public Entry getDirectoryEntry(IContributor contributor) {
		Entry entry = contributorToDirectoryEntry.get(contributor);
		if (entry == null) {
			throw new IllegalArgumentException(contributor.getName());
		}
		return entry;
	}

	public void setBundles(Map<File, Entry> bundleFileToDirectoryEntry) {
		this.bundleFileToDirectoryEntry = bundleFileToDirectoryEntry;
	}

}
