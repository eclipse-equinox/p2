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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.core.repository.AbstractRepository;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;

public class ExtensionLocationArtifactRepository extends AbstractRepository implements IFileArtifactRepository, Constants {

	public static final String TYPE = "org.eclipse.equinox.p2.extensionlocation.artifactRepository"; //$NON-NLS-1$
	public static final Integer VERSION = new Integer(1);
	private static final String POOLED = ".pooled"; //$NON-NLS-1$
	private final IFileArtifactRepository artifactRepository;

	/*
	 * Return the location of a local repository based on
	 * the given URL.
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
	 * Constructor for the class. Return a new extension location repository based on 
	 * the given url and nested repository.
	 */
	public ExtensionLocationArtifactRepository(URL location, IFileArtifactRepository repository, IProgressMonitor monitor) throws ProvisionException {
		super(Activator.getRepositoryName(location), TYPE, VERSION.toString(), location, null, null, null);
		this.artifactRepository = repository;

		File base = getBaseDirectory(location);
		File plugins = new File(base, PLUGINS);
		File features = new File(base, FEATURES);

		DirectoryWatcher watcher = new DirectoryWatcher(new File[] {plugins, features});
		DirectoryChangeListener listener = new RepositoryListener(Activator.getContext(), null, artifactRepository);
		if (location.getPath().endsWith(POOLED))
			listener = new BundlePoolFilteredListener(listener);

		watcher.addListener(listener);
		watcher.poll();
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
		File base = new File(path);
		if (path.endsWith(POOLED)) {
			base = base.getParentFile();
		}

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

	public void addDescriptor(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException();
	}

	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		throw new UnsupportedOperationException();
	}

	public void removeAll() {
		throw new UnsupportedOperationException();
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException();
	}

	public void removeDescriptor(IArtifactKey key) {
		throw new UnsupportedOperationException();
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		return artifactRepository.contains(descriptor);
	}

	public boolean contains(IArtifactKey key) {
		return artifactRepository.contains(key);
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return artifactRepository.getArtifact(descriptor, destination, monitor);
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		return artifactRepository.getArtifactDescriptors(key);
	}

	public IArtifactKey[] getArtifactKeys() {
		return artifactRepository.getArtifactKeys();
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		return artifactRepository.getArtifacts(requests, monitor);
	}

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
		return artifactRepository.getOutputStream(descriptor);
	}

	public File getArtifactFile(IArtifactKey key) {
		return artifactRepository.getArtifactFile(key);
	}

	public File getArtifactFile(IArtifactDescriptor descriptor) {
		return artifactRepository.getArtifactFile(descriptor);
	}

	public Map getProperties() {
		return artifactRepository.getProperties();
	}

	public String setProperty(String key, String value) {
		return artifactRepository.setProperty(key, value);
	}
}
