/*******************************************************************************
 *  Copyright (c) 2008, 2023 IBM Corporation and others.
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
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.extensionlocation;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;

public class ExtensionLocationMetadataRepository extends AbstractMetadataRepository implements Constants {

	public static final String TYPE = "org.eclipse.equinox.p2.extensionlocation.metadataRepository"; //$NON-NLS-1$
	public static final Integer VERSION = 1;
	public static final List<String> STANDARD_P2_REPOSITORY_FILE_NAMES = Arrays.asList("artifacts.jar", "content.jar", "artifacts.xml", "content.xml", "compositeArtifacts.xml", "compositeContent.xml", "compositeArtifacts.jar", "compositeContent.jar"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

	IMetadataRepository metadataRepository;
	private File base;
	private Object state = SiteListener.UNINITIALIZED;

	/*
	 * Return the URL for this repo's nested local repository.
	 */
	public static URI getLocalRepositoryLocation(URI location) {
		BundleContext context = Activator.getContext();
		String stateDirName = Integer.toString(location.toString().hashCode());
		File bundleData = context.getDataFile(null);
		return new File(bundleData, stateDirName).toURI();
	}

	/*
	 * Constructor for the class. Return a new extension location repository based on the
	 * given location and specified nested repo.
	 */
	public ExtensionLocationMetadataRepository(IProvisioningAgent agent, URI location, IMetadataRepository repository, IProgressMonitor monitor) throws ProvisionException {
		super(agent, Activator.getRepositoryName(location), TYPE, VERSION.toString(), location, null, null, null);
		this.metadataRepository = repository;
		this.base = getBaseDirectory(location);
	}

	public synchronized void ensureInitialized() {
		if (state == SiteListener.INITIALIZED || state == SiteListener.INITIALIZING)
			return;
		// if the repo has not been synchronized for us already, synchronize it.
		// Note: this will reload "metadataRepository"
		SiteListener.synchronizeRepositories(this, null, base);
	}

	void reload() {
		try {
			ExtensionLocationMetadataRepositoryFactory factory = new ExtensionLocationMetadataRepositoryFactory();
			factory.setAgent(getProvisioningAgent());
			ExtensionLocationMetadataRepository repo = (ExtensionLocationMetadataRepository) factory.load(getLocation(), 0, null);
			metadataRepository = repo.metadataRepository;
			base = repo.base;
		} catch (ProvisionException e) {
			//unexpected
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	void state(Object value) {
		state = value;
	}

	@Override
	public Collection<IRepositoryReference> getReferences() {
		return Collections.emptyList();
	}

	@Override
	public void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		ensureInitialized();
		return metadataRepository.query(query, monitor);
	}

	@Override
	public boolean contains(IInstallableUnit element) {
		return metadataRepository.contains(element);
	}

	public static void validate(URI location, IProgressMonitor monitor) throws ProvisionException {
		File base = getBaseDirectory(location);
		if (new File(base, EXTENSION_LOCATION).exists() || location.getPath().endsWith(EXTENSION_LOCATION))
			return;
		if (containsUpdateSiteFile(base)) {
			String message = NLS.bind(Messages.error_update_site, location.toString());
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, message, null));
		}
		if (containsStandardP2Repository(base)) {
			String message = NLS.bind(Messages.error_p2_repository, location.toString());
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, message, null));
		}
	}

	private static boolean containsStandardP2Repository(File base) {
		File[] foundRepos = base.listFiles((FilenameFilter) (dir, name) -> (STANDARD_P2_REPOSITORY_FILE_NAMES.contains(name)));
		return foundRepos.length > 0;
	}

	private static boolean containsUpdateSiteFile(File base) {
		String[] fileNames = base.list();
		if (fileNames == null)
			return false;
		for (String fileName : fileNames) {
			if (fileName.endsWith(DOT_XML) && fileName.contains(SITE)) {
				return true;
			}
		}
		return false;
	}

	public static File getBaseDirectory(URI uri) throws ProvisionException {
		if (!FILE.equals(uri.getScheme()))
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, Messages.not_file_protocol, null));

		File base = URIUtil.toFile(uri);
		String path = base.getAbsolutePath();
		if (path.endsWith(EXTENSION_LOCATION))
			base = new File(path.substring(0, path.length() - EXTENSION_LOCATION.length()));

		if (!base.isDirectory())
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.not_directory, uri.toString()), null));

		if (isBaseDirectory(base))
			return base;

		File eclipseBase = new File(base, ECLIPSE);
		if (isBaseDirectory(eclipseBase))
			return eclipseBase;

		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.not_eclipse_extension, uri.toString()), null));
	}

	private static boolean isBaseDirectory(File base) {
		File plugins = new File(base, PLUGINS);
		File features = new File(base, FEATURES);

		return plugins.isDirectory() || features.isDirectory();
	}

	@Override
	public Map<String, String> getProperties() {
		ensureInitialized();
		return metadataRepository.getProperties();
	}

	@Override
	public void initialize(RepositoryState repositoryState) {
		//nothing to do
	}

	@Override
	public String setProperty(String key, String value, IProgressMonitor monitor) {
		try {
			ensureInitialized();
			String oldValue = metadataRepository.setProperty(key, value);
			// if the value didn't really change then just return
			if (oldValue == value || (oldValue != null && oldValue.equals(value)))
				return oldValue;
			// we want to re-initialize if we are changing the site policy or plug-in list
			if (!SiteListener.SITE_LIST.equals(key) && !SiteListener.SITE_POLICY.equals(key))
				return oldValue;
			state = SiteListener.UNINITIALIZED;
			ensureInitialized();
			return oldValue;
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}
}
