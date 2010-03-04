/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationArtifactRepository;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DropinsRepositoryListener extends RepositoryListener {

	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String JAR = ".jar"; //$NON-NLS-1$
	private static final String LINK = ".link"; //$NON-NLS-1$
	private static final String ZIP = ".zip"; //$NON-NLS-1$
	private static final String LINKS_PATH = "path"; //$NON-NLS-1$
	private static final String DROPIN_ARTIFACT_REPOSITORIES = "dropin.artifactRepositories"; //$NON-NLS-1$
	private static final String DROPIN_METADATA_REPOSITORIES = "dropin.metadataRepositories"; //$NON-NLS-1$
	final private String PREFIX = "[reconciler][dropins] "; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$
	private BundleContext context;
	private List metadataRepositories = new ArrayList();
	private List artifactRepositories = new ArrayList();

	public DropinsRepositoryListener(BundleContext context, String repositoryName) {
		super(context, repositoryName, null, true);
		this.context = context;
	}

	public boolean isInterested(File file) {
		return true;
	}

	public boolean added(File file) {
		if (super.added(file)) {
			if (Tracing.DEBUG_RECONCILER) {
				Tracing.debug(PREFIX + "Interesting feature or bundle added " + file); //$NON-NLS-1$
			}
			return true;
		}

		URL repositoryURL = createRepositoryURL(file);
		if (repositoryURL != null) {
			getMetadataRepository(repositoryURL);
			getArtifactRepository(repositoryURL);
		}
		return true;
	}

	public boolean changed(File file) {
		if (super.changed(file)) {
			if (Tracing.DEBUG_RECONCILER) {
				Tracing.debug(PREFIX + "Interesting feature or bundle changed " + file); //$NON-NLS-1$
			}
			return true;
		}

		URL repositoryURL = createRepositoryURL(file);
		if (repositoryURL != null) {
			getMetadataRepository(repositoryURL);
			getArtifactRepository(repositoryURL);
		}
		return true;
	}

	static File getLinkedFile(File file) {
		Properties links = new Properties();
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(file));
			try {
				links.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.error_reading_link, file.getAbsolutePath()), e));
			return null;
		}
		String path = links.getProperty(LINKS_PATH);
		if (path == null) {
			return null;
		}

		// parse out link information
		if (path.startsWith("r ")) { //$NON-NLS-1$
			path = path.substring(2).trim();
		} else if (path.startsWith("rw ")) { //$NON-NLS-1$
			path = path.substring(3).trim();
		} else {
			path = path.trim();
		}
		File linkedFile = new File(path);
		if (!linkedFile.isAbsolute()) {
			// link support is relative to the install root
			File root = Activator.getEclipseHome();
			if (root != null)
				linkedFile = new File(root, path);
		}
		try {
			return linkedFile.getCanonicalFile();
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.error_resolving_link, linkedFile.getAbsolutePath(), file.getAbsolutePath()), e));
			return null;
		}
	}

	private URL createRepositoryURL(File file) {
		try {
			file = file.getCanonicalFile();
			String fileName = file.getName();
			if (fileName.endsWith(LINK))
				return getLinkRepository(file, true);

			if (file.isDirectory()) {
				// Check if the directory is either the plugins directory of an extension location
				// or the features directory and the plugins folder is not present.
				// This extra check on the features directory is done to avoid adding the parent URL twice
				if (file.getName().equals(PLUGINS)) {
					File parentFile = file.getParentFile();
					return (parentFile != null) ? parentFile.toURL() : null;
				}
				if (file.getName().equals(FEATURES)) {
					File parentFile = file.getParentFile();
					if (parentFile == null || new File(parentFile, PLUGINS).isDirectory())
						return null;
					return parentFile.toURL();
				}
				return file.toURL();
			}

			if (fileName.endsWith(ZIP) || fileName.endsWith(JAR))
				return new URL("jar:" + file.toURL().toExternalForm() + "!/"); //$NON-NLS-1$ //$NON-NLS-2$

			// last resort -- we'll try to interpret the file as a link
			return getLinkRepository(file, false);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while building repository location from file: " + file.getAbsolutePath(), e)); //$NON-NLS-1$
		}
		return null;
	}

	private URL getLinkRepository(File file, boolean logMissingLink) throws IOException {
		File linkedFile = getLinkedFile(file);
		if (linkedFile == null) {
			if (logMissingLink)
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Unable to determine link location from file: " + file.getAbsolutePath())); //$NON-NLS-1$
			return null;
		}
		return linkedFile.toURL();
	}

	public void getMetadataRepository(URL repoURL) {
		try {
			IMetadataRepository repository = null;
			try {
				ExtensionLocationMetadataRepository.validate(repoURL, null);
				Map properties = new HashMap();
				properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
				repository = Activator.createExtensionLocationMetadataRepository(repoURL, "dropins metadata repo: " + repoURL.toExternalForm(), properties); //$NON-NLS-1$
			} catch (ProvisionException e) {
				repository = Activator.loadMetadataRepository(repoURL, null);
			}
			debugRepository(repository);
			metadataRepositories.add(repository);
		} catch (ProvisionException ex) {
			LogHelper.log(ex);
		}
	}

	public void getArtifactRepository(URL repoURL) {
		try {
			IArtifactRepository repository = null;
			try {
				ExtensionLocationArtifactRepository.validate(repoURL, null);
				Map properties = new HashMap();
				properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
				repository = Activator.createExtensionLocationArtifactRepository(repoURL, "dropins artifact repo: " + repoURL.toExternalForm(), properties); //$NON-NLS-1$
				// fall through here and call the load which then adds the repo to the manager's list
			} catch (ProvisionException ex) {
				repository = Activator.loadArtifactRepository(repoURL, null);
			}
			artifactRepositories.add(repository);
		} catch (ProvisionException ex) {
			LogHelper.log(ex);
		}
	}

	public void stopPoll() {
		synchronizeDropinMetadataRepositories();
		synchronizeDropinArtifactRepositories();
		super.stopPoll();
	}

	private void synchronizeDropinMetadataRepositories() {
		List currentRepositories = new ArrayList();
		for (Iterator it = metadataRepositories.iterator(); it.hasNext();) {
			IMetadataRepository repository = (IMetadataRepository) it.next();
			String urlString = repository.getLocation().toExternalForm();
			currentRepositories.add(urlString);
		}
		List previousRepositories = getListRepositoryProperty(getMetadataRepository(), DROPIN_METADATA_REPOSITORIES);
		for (Iterator iterator = previousRepositories.iterator(); iterator.hasNext();) {
			String repository = (String) iterator.next();
			if (!currentRepositories.contains(repository))
				removeMetadataRepository(repository);
		}
		setListRepositoryProperty(getMetadataRepository(), DROPIN_METADATA_REPOSITORIES, currentRepositories);
	}

	private void removeMetadataRepository(String urlString) {
		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		IMetadataRepositoryManager manager = null;
		if (reference != null)
			manager = (IMetadataRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException(Messages.metadata_repo_manager_not_registered);

		try {
			manager.removeRepository(new URL(urlString));
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while creating URL from: " + urlString, e)); //$NON-NLS-1$
		} finally {
			context.ungetService(reference);
		}
	}

	private void synchronizeDropinArtifactRepositories() {
		List currentRepositories = new ArrayList();
		for (Iterator it = artifactRepositories.iterator(); it.hasNext();) {
			IArtifactRepository repository = (IArtifactRepository) it.next();
			String urlString = repository.getLocation().toExternalForm();
			currentRepositories.add(urlString);
		}
		List previousRepositories = getListRepositoryProperty(getArtifactRepository(), DROPIN_ARTIFACT_REPOSITORIES);
		for (Iterator iterator = previousRepositories.iterator(); iterator.hasNext();) {
			String repository = (String) iterator.next();
			if (!currentRepositories.contains(repository))
				removeArtifactRepository(repository);
		}
		setListRepositoryProperty(getArtifactRepository(), DROPIN_ARTIFACT_REPOSITORIES, currentRepositories);
	}

	public void removeArtifactRepository(String urlString) {
		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
		IArtifactRepositoryManager manager = null;
		if (reference != null)
			manager = (IArtifactRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException(Messages.artifact_repo_manager_not_registered);

		try {
			manager.removeRepository(new URL(urlString));
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while creating URL from: " + urlString, e)); //$NON-NLS-1$
		} finally {
			context.ungetService(reference);
		}
	}

	private List getListRepositoryProperty(IRepository repository, String key) {
		List listProperty = new ArrayList();
		String dropinRepositories = (String) repository.getProperties().get(key);
		if (dropinRepositories != null) {
			StringTokenizer tokenizer = new StringTokenizer(dropinRepositories, PIPE);
			while (tokenizer.hasMoreTokens()) {
				listProperty.add(tokenizer.nextToken());
			}
		}
		return listProperty;
	}

	private void setListRepositoryProperty(IRepository repository, String key, List listProperty) {
		StringBuffer buffer = new StringBuffer();
		for (Iterator it = listProperty.iterator(); it.hasNext();) {
			String repositoryString = (String) it.next();
			buffer.append(repositoryString);
			if (it.hasNext())
				buffer.append(PIPE);
		}
		String value = (buffer.length() == 0) ? null : buffer.toString();
		repository.setProperty(key, value);
	}

	public Collection getMetadataRepositories() {
		List result = new ArrayList(metadataRepositories);
		result.add(getMetadataRepository());
		return result;
	}

	private void debugRepository(IMetadataRepository repository) {
		if (!Tracing.DEBUG_RECONCILER)
			return;
		Tracing.debug(PREFIX + "Repository created " + repository.getLocation()); //$NON-NLS-1$
		Collector result = repository.query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor());
		for (Iterator iter = result.iterator(); iter.hasNext();)
			Tracing.debug(PREFIX + "\t" + iter.next()); //$NON-NLS-1$
	}

}
