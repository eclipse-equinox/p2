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
package org.eclipse.equinox.internal.p2.extensionlocation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;

public class ExtensionLocationMetadataRepository extends AbstractMetadataRepository implements Constants {

	public static final String TYPE = "org.eclipse.equinox.p2.extensionlocation.metadataRepository"; //$NON-NLS-1$
	public static final Integer VERSION = new Integer(1);
	private final IMetadataRepository metadataRepository;
	private boolean initialized = false;
	private File base;

	/*
	 * Return the URL for this repo's nested local repository.
	 */
	public static URL getLocalRepositoryLocation(URL location) {
		BundleContext context = Activator.getContext();
		String stateDirName = Integer.toString(location.toExternalForm().hashCode());
		File bundleData = context.getDataFile(null);
		File stateDir = new File(bundleData, stateDirName);
		try {
			return stateDir.toURL();
		} catch (MalformedURLException e) {
			// unexpected
			return null;
		}
	}

	/*
	 * Constructor for the class. Return a new extension location repository based on the 
	 * given location and specified nested repo.
	 */
	public ExtensionLocationMetadataRepository(URL location, IMetadataRepository repository, IProgressMonitor monitor) throws ProvisionException {
		super(Activator.getRepositoryName(location), TYPE, VERSION.toString(), location, null, null, null);
		this.metadataRepository = repository;
		this.base = getBaseDirectory(location);
	}

	public synchronized void ensureInitialized() {
		if (initialized)
			return;
		File plugins = new File(base, PLUGINS);
		File features = new File(base, FEATURES);
		DirectoryWatcher watcher = new DirectoryWatcher(new File[] {plugins, features});
		DirectoryChangeListener listener = new RepositoryListener(Activator.getContext(), metadataRepository, null);
		if (getProperties().get(SiteListener.SITE_POLICY) != null)
			listener = new SiteListener(getProperties(), location.toExternalForm(), new BundlePoolFilteredListener(listener));
		watcher.addListener(listener);
		watcher.poll();
		initialized = true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository#addInstallableUnits(org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit[])
	 */
	public void addInstallableUnits(IInstallableUnit[] installableUnits) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository#removeAll()
	 */
	public void removeAll() {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository#removeInstallableUnits(org.eclipse.equinox.internal.provisional.p2.query.Query, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean removeInstallableUnits(Query query, IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.query.IQueryable#query(org.eclipse.equinox.internal.provisional.p2.query.Query, org.eclipse.equinox.internal.provisional.p2.query.Collector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		ensureInitialized();
		return metadataRepository.query(query, collector, monitor);
	}

	public static void validate(URL location, IProgressMonitor monitor) throws ProvisionException {
		File base = getBaseDirectory(location);
		if (new File(base, EXTENSION_LOCATION).exists())
			return;
		if (containsUpdateSiteFile(base)) {
			String message = NLS.bind(Messages.error_update_site, location.toExternalForm());
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, message, null));
		}
	}

	private static boolean containsUpdateSiteFile(File base) {
		String[] fileNames = base.list();
		if (fileNames == null)
			return false;
		for (int i = 0; i < fileNames.length; i++) {
			if (fileNames[i].endsWith(DOT_XML) && fileNames[i].indexOf(SITE) != -1)
				return true;
		}
		return false;
	}

	public static File getBaseDirectory(URL url) throws ProvisionException {
		if (!FILE.equals(url.getProtocol()))
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, Messages.not_file_protocol, null));

		String path = url.getPath();
		if (path.endsWith(EXTENSION_LOCATION))
			path = path.substring(0, path.length() - EXTENSION_LOCATION.length());
		File base = new File(path);

		if (!base.isDirectory())
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.not_directory, url.toExternalForm()), null));

		if (isBaseDirectory(base))
			return base;

		File eclipseBase = new File(base, ECLIPSE);
		if (isBaseDirectory(eclipseBase))
			return eclipseBase;

		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.not_eclipse_extension, url.toExternalForm()), null));
	}

	private static boolean isBaseDirectory(File base) {
		File plugins = new File(base, PLUGINS);
		File features = new File(base, FEATURES);

		return plugins.isDirectory() || features.isDirectory();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.core.repository.AbstractRepository#getProperties()
	 */
	public Map getProperties() {
		return metadataRepository.getProperties();
	}

	public void initialize(RepositoryState state) {
		//nothing to do
	}

	public String setProperty(String key, String value) {
		return metadataRepository.setProperty(key, value);
	}
}
