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
import org.eclipse.equinox.internal.provisional.spi.p2.core.repository.AbstractRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.osgi.framework.BundleContext;

public class ExtensionLocationMetadataRepository extends AbstractRepository implements IMetadataRepository {

	private static final String POOLED = ".pooled"; //$NON-NLS-1$
	private static final String ECLIPSE = "eclipse"; //$NON-NLS-1$
	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String FILE = "file"; //$NON-NLS-1$
	private final IMetadataRepository metadataRepository;

	public ExtensionLocationMetadataRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		super("Extension: " + location.toExternalForm(), null, null, location, null, null); //$NON-NLS-1$

		File base = getBaseDirectory(location);
		File plugins = new File(base, PLUGINS);
		File features = new File(base, FEATURES);

		BundleContext context = Activator.getContext();
		String stateDirName = Integer.toString(location.toExternalForm().hashCode());
		File bundleData = context.getDataFile(null);
		File stateDir = new File(bundleData, stateDirName);
		URL localRepositoryURL;
		try {
			localRepositoryURL = stateDir.toURL();
		} catch (MalformedURLException e) {
			// unexpected
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, Messages.failed_create_local_artifact_repository, e));
		}

		metadataRepository = initializeMetadataRepository(localRepositoryURL, "extension location implementation - " + location.toExternalForm()); //$NON-NLS-1$

		DirectoryWatcher watcher = new DirectoryWatcher(new File[] {plugins, features});
		DirectoryChangeListener listener = new RepositoryListener(context, metadataRepository, null);
		if (location.getPath().endsWith(POOLED))
			listener = new BundlePoolFilteredListener(listener);

		watcher.addListener(listener);
		watcher.poll();
	}

	private IMetadataRepository initializeMetadataRepository(URL stateDirURL, String repositoryName) {
		SimpleMetadataRepositoryFactory factory = new SimpleMetadataRepositoryFactory();
		try {
			return factory.load(stateDirURL, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		return factory.create(stateDirURL, repositoryName, null);
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnits) {
		throw new UnsupportedOperationException();
	}

	public void removeAll() {
		throw new UnsupportedOperationException();
	}

	public boolean removeInstallableUnits(Query query, IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return metadataRepository.query(query, collector, monitor);
	}

	public static void validate(URL location, IProgressMonitor monitor) throws ProvisionException {
		getBaseDirectory(location);
	}

	public static File getBaseDirectory(URL url) throws ProvisionException {
		if (!FILE.equals(url.getProtocol()))
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, Messages.not_file_protocol, null));

		String path = url.getPath();
		File base = new File(path);
		if (path.endsWith(POOLED)) {
			base = base.getParentFile();
		}

		if (!base.isDirectory())
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, Messages.not_directory, null));

		if (isBaseDirectory(base))
			return base;

		File eclipseBase = new File(base, ECLIPSE);
		if (isBaseDirectory(eclipseBase))
			return eclipseBase;

		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, Messages.not_eclipse_extension, null));
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
		if (metadataRepository == null)
			return super.getProperties();

		return metadataRepository.getProperties();
	}
}
