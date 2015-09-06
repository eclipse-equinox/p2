/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationArtifactRepository;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationMetadataRepository;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

public class DropinsRepositoryListener extends RepositoryListener {
	private static final String PREFIX = "[reconciler] [dropins] "; //$NON-NLS-1$
	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String JAR = ".jar"; //$NON-NLS-1$
	private static final String LINK = ".link"; //$NON-NLS-1$
	private static final String ZIP = ".zip"; //$NON-NLS-1$
	private static final String LINKS_PATH = "path"; //$NON-NLS-1$
	private static final String LINK_IS_OPTIONAL = "optional"; //$NON-NLS-1$
	private static final String DROPIN_ARTIFACT_REPOSITORIES = "dropin.artifactRepositories"; //$NON-NLS-1$
	private static final String DROPIN_METADATA_REPOSITORIES = "dropin.metadataRepositories"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$
	private final IProvisioningAgent agent;
	private List<IMetadataRepository> metadataRepositories = new ArrayList<IMetadataRepository>();
	private List<IArtifactRepository> artifactRepositories = new ArrayList<IArtifactRepository>();

	static class LinkedRepository {
		LinkedRepository(File location) {
			super();
			if (location == null)
				throw new IllegalArgumentException("Repository location cannot be null."); //$NON-NLS-1$
			this.location = location;
		}

		boolean exists() {
			return location.exists();
		}

		File getLocation() {
			return location;
		}

		boolean isOptional() {
			return optional;
		}

		void setOptional(boolean optional) {
			this.optional = optional;
		}

		private File location;
		private boolean optional = false;
	}

	public DropinsRepositoryListener(IProvisioningAgent agent, String repositoryName, Map<String, String> properties) {
		super(repositoryName, properties);
		this.agent = agent;
	}

	public boolean isInterested(File file) {
		return true;
	}

	public boolean added(File file) {
		if (super.added(file)) {
			if (Tracing.DEBUG_RECONCILER)
				Tracing.debug(PREFIX + "Interesting feature or bundle added: " + file); //$NON-NLS-1$
			return true;
		}
		addRepository(file);
		return true;
	}

	public boolean changed(File file) {
		if (super.changed(file)) {
			if (Tracing.DEBUG_RECONCILER)
				Tracing.debug(PREFIX + "Interesting feature or bundle changed: " + file); //$NON-NLS-1$			
			return true;
		}
		addRepository(file);
		return true;
	}

	private void addRepository(File file) {
		URI repoLocation = createRepositoryLocation(file);
		if (repoLocation == null)
			return;
		Map<String, String> properties = new HashMap<String, String>();
		// if the file pointed to a link file, keep track of the attribute
		// so we can add it to the repo later
		if (file.isFile() && file.getName().endsWith(LINK)) {
			URI linkLocation = getLinkRepository(file, false);
			if (linkLocation != null)
				properties.put(Site.PROP_LINK_FILE, file.getAbsolutePath());
		}
		getMetadataRepository(repoLocation, properties);
		getArtifactRepository(repoLocation, properties);
	}

	/*
	 * Return the file pointed to by the given link file. Return null if there is a problem
	 * reading the file or resolving the location.
	 */
	static LinkedRepository getLinkedRepository(File file) {
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
		path = Activator.substituteVariables(path);
		File linkedFile = new File(path);
		if (!linkedFile.isAbsolute()) {
			// link support is relative to the install root
			File root = Activator.getEclipseHome();
			if (root != null)
				linkedFile = new File(root, path);
		}
		try {
			LinkedRepository result = new LinkedRepository(linkedFile.getCanonicalFile());
			// Check if the link target is marked as optional.
			// If link is optional, then the link target may not exist.
			// So IF link is marked as optional AND does not exist, simply ignore it.
			String optional = links.getProperty(LINK_IS_OPTIONAL);
			result.setOptional(Boolean.parseBoolean(optional));
			return result;
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.error_resolving_link, linkedFile.getAbsolutePath(), file.getAbsolutePath()), e));
			return null;
		}
	}

	private URI createRepositoryLocation(File file) {
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
					return (parentFile != null) ? parentFile.toURI() : null;
				}
				if (file.getName().equals(FEATURES)) {
					File parentFile = file.getParentFile();
					if (parentFile == null || new File(parentFile, PLUGINS).isDirectory())
						return null;
					return parentFile.toURI();
				}
				return file.toURI();
			}

			//TODO: Should we remove this? We only should support directly runnable repos
			if (fileName.endsWith(ZIP) || fileName.endsWith(JAR))
				return new URI("jar:" + file.toURI() + "!/"); //$NON-NLS-1$ //$NON-NLS-2$

			// last resort -- we'll try to interpret the file as a link
			return getLinkRepository(file, false);
		} catch (URISyntaxException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while building repository location from file: " + file.getAbsolutePath(), e)); //$NON-NLS-1$
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while building repository location from file: " + file.getAbsolutePath(), e)); //$NON-NLS-1$
		}
		return null;
	}

	private URI getLinkRepository(File file, boolean logMissingLink) {
		LinkedRepository repo = getLinkedRepository(file);
		if (repo == null) {
			if (logMissingLink)
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Unable to determine link location from file: " + file.getAbsolutePath())); //$NON-NLS-1$
			return null;
		}
		return repo.isOptional() && !repo.exists() ? null : repo.getLocation().toURI();
	}

	public void getMetadataRepository(URI repoURL, Map<String, String> properties) {
		try {
			IMetadataRepository repository = null;
			try {
				ExtensionLocationMetadataRepository.validate(repoURL, null);
				repository = Activator.createExtensionLocationMetadataRepository(repoURL, "dropins metadata repo: " + repoURL, properties); //$NON-NLS-1$
			} catch (ProvisionException e) {
				repository = Activator.loadMetadataRepository(repoURL, null);
			}
			metadataRepositories.add(repository);
			debugRepository(repository);
		} catch (ProvisionException ex) {
			LogHelper.log(ex);
		}
	}

	private void debugRepository(IMetadataRepository repository) {
		if (!Tracing.DEBUG_RECONCILER)
			return;
		Tracing.debug(PREFIX + "Repository created " + repository.getLocation()); //$NON-NLS-1$
		// Print out a list of all the IUs in the repository
		IQueryResult<IInstallableUnit> result = repository.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		for (Iterator<IInstallableUnit> iter = result.iterator(); iter.hasNext();)
			Tracing.debug(PREFIX + "\t" + iter.next()); //$NON-NLS-1$
	}

	public void getArtifactRepository(URI repoURL, Map<String, String> properties) {
		try {
			IArtifactRepository repository = null;
			try {
				ExtensionLocationArtifactRepository.validate(repoURL, null);
				repository = Activator.createExtensionLocationArtifactRepository(repoURL, "dropins artifact repo: " + repoURL, properties); //$NON-NLS-1$
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
		List<String> currentRepositories = new ArrayList<String>();
		for (Iterator<IMetadataRepository> it = metadataRepositories.iterator(); it.hasNext();) {
			IMetadataRepository repository = it.next();
			currentRepositories.add(repository.getLocation().toString());
		}
		List<String> previousRepositories = getListRepositoryProperty(getMetadataRepository(), DROPIN_METADATA_REPOSITORIES);
		for (Iterator<String> iterator = previousRepositories.iterator(); iterator.hasNext();) {
			String repository = iterator.next();
			if (!currentRepositories.contains(repository))
				removeMetadataRepository(repository);
		}
		setListRepositoryProperty(getMetadataRepository(), DROPIN_METADATA_REPOSITORIES, currentRepositories);
	}

	private void removeMetadataRepository(String urlString) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException(Messages.metadata_repo_manager_not_registered);
		try {
			manager.removeRepository(new URI(urlString));
		} catch (URISyntaxException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while creating URL from: " + urlString, e)); //$NON-NLS-1$
		}
	}

	private void synchronizeDropinArtifactRepositories() {
		List<String> currentRepositories = new ArrayList<String>();
		for (Iterator<IArtifactRepository> it = artifactRepositories.iterator(); it.hasNext();) {
			IArtifactRepository repository = it.next();
			currentRepositories.add(repository.getLocation().toString());
		}
		List<String> previousRepositories = getListRepositoryProperty(getArtifactRepository(), DROPIN_ARTIFACT_REPOSITORIES);
		for (Iterator<String> iterator = previousRepositories.iterator(); iterator.hasNext();) {
			String repository = iterator.next();
			if (!currentRepositories.contains(repository))
				removeArtifactRepository(repository);
		}
		setListRepositoryProperty(getArtifactRepository(), DROPIN_ARTIFACT_REPOSITORIES, currentRepositories);
	}

	public void removeArtifactRepository(String urlString) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException(Messages.artifact_repo_manager_not_registered);
		try {
			manager.removeRepository(new URI(urlString));
		} catch (URISyntaxException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while creating URL from: " + urlString, e)); //$NON-NLS-1$
		}
	}

	private List<String> getListRepositoryProperty(IRepository<?> repository, String key) {
		List<String> listProperty = new ArrayList<String>();
		String dropinRepositories = repository.getProperties().get(key);
		if (dropinRepositories != null) {
			StringTokenizer tokenizer = new StringTokenizer(dropinRepositories, PIPE);
			while (tokenizer.hasMoreTokens()) {
				listProperty.add(tokenizer.nextToken());
			}
		}
		return listProperty;
	}

	private void setListRepositoryProperty(IRepository<?> repository, String key, List<String> listProperty) {
		StringBuffer buffer = new StringBuffer();
		for (Iterator<String> it = listProperty.iterator(); it.hasNext();) {
			String repositoryString = it.next();
			buffer.append(repositoryString);
			if (it.hasNext())
				buffer.append(PIPE);
		}
		String value = (buffer.length() == 0) ? null : buffer.toString();
		repository.setProperty(key, value);
	}

	public Collection<IMetadataRepository> getMetadataRepositories() {
		List<IMetadataRepository> result = new ArrayList<IMetadataRepository>(metadataRepositories);
		result.add(getMetadataRepository());
		return result;
	}

}
