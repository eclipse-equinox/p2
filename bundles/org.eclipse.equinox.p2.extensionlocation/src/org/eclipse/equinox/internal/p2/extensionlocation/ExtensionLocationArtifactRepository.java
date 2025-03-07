/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
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

import java.io.*;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;

public class ExtensionLocationArtifactRepository extends AbstractRepository<IArtifactKey> implements IFileArtifactRepository, Constants {

	public static final String TYPE = "org.eclipse.equinox.p2.extensionlocation.artifactRepository"; //$NON-NLS-1$
	public static final Integer VERSION = 1;
	public static final List<String> STANDARD_P2_REPOSITORY_FILE_NAMES = Arrays.asList(new String[] {"artifacts.xml", "content.xml", "compositeArtifacts.xml", "compositeContent.xml"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	IFileArtifactRepository artifactRepository;
	private File base;
	private Object state = SiteListener.UNINITIALIZED;

	/*
	 * Return the location of a local repository based on
	 * the given URL.
	 */
	public static URI getLocalRepositoryLocation(URI location) {
		BundleContext context = Activator.getContext();
		String stateDirName = Integer.toString(location.toString().hashCode());
		File bundleData = context.getDataFile(null);
		File stateDir = new File(bundleData, stateDirName);
		return stateDir.toURI();
	}

	/*
	 * Constructor for the class. Return a new extension location repository based on 
	 * the given url and nested repository.
	 */
	public ExtensionLocationArtifactRepository(IProvisioningAgent agent, URI location, IFileArtifactRepository repository, IProgressMonitor monitor) throws ProvisionException {
		super(agent, Activator.getRepositoryName(location), TYPE, VERSION.toString(), location, null, null, null);
		this.artifactRepository = repository;
		this.base = getBaseDirectory(location);
	}

	public synchronized void ensureInitialized() {
		if (state == SiteListener.INITIALIZED || state == SiteListener.INITIALIZING) {
			return;
		}
		// if the repo has not been synchronized for us already, synchronize it.
		// Note: this will reload "artifactRepository"
		SiteListener.synchronizeRepositories(null, this, base);
	}

	void reload() {
		try {
			ExtensionLocationArtifactRepositoryFactory factory = new ExtensionLocationArtifactRepositoryFactory();
			factory.setAgent(getProvisioningAgent());
			ExtensionLocationArtifactRepository repo = (ExtensionLocationArtifactRepository) factory.load(getLocation(), 0, null);
			artifactRepository = repo.artifactRepository;
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

	public static void validate(URI location, IProgressMonitor monitor) throws ProvisionException {
		File base = getBaseDirectory(location);
		if (new File(base, EXTENSION_LOCATION).exists() || location.getPath().endsWith(EXTENSION_LOCATION)) {
			return;
		}
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
		if (fileNames == null) {
			return false;
		}
		for (String fileName : fileNames) {
			if (fileName.endsWith(DOT_XML) && fileName.contains(SITE)) {
				return true;
			}
		}
		return false;
	}

	public static File getBaseDirectory(URI uri) throws ProvisionException {
		if (!FILE.equals(uri.getScheme())) {
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, Messages.not_file_protocol, null));
		}

		String path = URIUtil.toFile(uri).getAbsolutePath();
		if (path.endsWith(EXTENSION_LOCATION)) {
			path = path.substring(0, path.length() - EXTENSION_LOCATION.length());
		}
		File base = new File(path);

		if (!base.isDirectory()) {
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.not_directory, uri.toString()), null));
		}

		if (isBaseDirectory(base)) {
			return base;
		}

		File eclipseBase = new File(base, ECLIPSE);
		if (isBaseDirectory(eclipseBase)) {
			return eclipseBase;
		}

		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, NLS.bind(Messages.not_eclipse_extension, uri.toString()), null));
	}

	private static boolean isBaseDirectory(File base) {
		File plugins = new File(base, PLUGINS);
		File features = new File(base, FEATURES);

		return plugins.isDirectory() || features.isDirectory();
	}

	@Override
	public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void addDescriptor(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAll(IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void removeAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void removeDescriptor(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void removeDescriptor(IArtifactKey key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void removeDescriptors(IArtifactDescriptor[] descriptors) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void removeDescriptors(IArtifactKey[] keys) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(IArtifactDescriptor descriptor) {
		ensureInitialized();
		return artifactRepository.contains(descriptor);
	}

	@Override
	public boolean contains(IArtifactKey key) {
		ensureInitialized();
		return artifactRepository.contains(key);
	}

	@Override
	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ensureInitialized();
		return artifactRepository.getArtifact(descriptor, destination, monitor);
	}

	@Override
	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ensureInitialized();
		return artifactRepository.getRawArtifact(descriptor, destination, monitor);
	}

	@Override
	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		ensureInitialized();
		return artifactRepository.getArtifactDescriptors(key);
	}

	@Override
	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		ensureInitialized();
		return artifactRepository.getArtifacts(requests, monitor);
	}

	@Override
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
		ensureInitialized();
		return artifactRepository.getOutputStream(descriptor);
	}

	@Override
	public File getArtifactFile(IArtifactKey key) {
		ensureInitialized();
		return artifactRepository.getArtifactFile(key);
	}

	@Override
	public File getArtifactFile(IArtifactDescriptor descriptor) {
		ensureInitialized();
		return artifactRepository.getArtifactFile(descriptor);
	}

	@Override
	public Map<String, String> getProperties() {
		ensureInitialized();
		return artifactRepository.getProperties();
	}

	@Override
	public String setProperty(String key, String value, IProgressMonitor monitor) {
		try {
			ensureInitialized();
			String oldValue = artifactRepository.setProperty(key, value);
			// if the value didn't really change then just return
			if (oldValue == value || (oldValue != null && oldValue.equals(value))) {
				return oldValue;
			}
			// we want to re-initialize if we are changing the site policy or plug-in list
			if (!SiteListener.SITE_LIST.equals(key) && !SiteListener.SITE_POLICY.equals(key)) {
				return oldValue;
			}
			state = SiteListener.UNINITIALIZED;
			ensureInitialized();
			return oldValue;
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	@Override
	public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
		return artifactRepository.createArtifactDescriptor(key);
	}

	@Override
	public IArtifactKey createArtifactKey(String classifier, String id, Version version) {
		return artifactRepository.createArtifactKey(classifier, id, version);
	}

	@Override
	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		ensureInitialized();
		return artifactRepository.descriptorQueryable();
	}

	@Override
	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		ensureInitialized();
		return artifactRepository.query(query, monitor);
	}

	@Override
	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		try {
			runnable.run(monitor);
		} catch (OperationCanceledException oce) {
			return new Status(IStatus.CANCEL, Activator.ID, oce.getMessage(), oce);
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e);
		}
		return Status.OK_STATUS;
	}
}
