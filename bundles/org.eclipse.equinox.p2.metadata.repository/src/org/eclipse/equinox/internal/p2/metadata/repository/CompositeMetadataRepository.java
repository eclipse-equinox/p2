/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.osgi.util.NLS;

public class CompositeMetadataRepository extends AbstractMetadataRepository implements ICompositeRepository<IInstallableUnit> {

	static final public String REPOSITORY_TYPE = CompositeMetadataRepository.class.getName();
	public static final String PI_REPOSITORY_TYPE = "compositeMetadataRepository"; //$NON-NLS-1$
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final public String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	static final private String JAR_EXTENSION = ".jar"; //$NON-NLS-1$

	// keep a list of the child URIs. they can be absolute or relative. they may or may not point
	// to a valid reachable repo
	private List<URI> childrenURIs = new ArrayList<URI>();
	// keep a list of the repositories that we have successfully loaded
	private List<IMetadataRepository> loadedRepos = new ArrayList<IMetadataRepository>();
	private IMetadataRepositoryManager manager;

	/**
	 * Create a Composite repository in memory.
	 * @return the repository or null if unable to create one
	 */
	public static CompositeMetadataRepository createMemoryComposite(IProvisioningAgent agent) {
		if (agent == null)
			return null;
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (repoManager == null)
			return null;
		try {
			//create a unique opaque URI 
			long time = System.currentTimeMillis();
			URI repositoryURI = new URI("memory:" + String.valueOf(time)); //$NON-NLS-1$
			while (repoManager.contains(repositoryURI))
				repositoryURI = new URI("memory:" + String.valueOf(++time)); //$NON-NLS-1$

			CompositeMetadataRepository result = (CompositeMetadataRepository) repoManager.createRepository(repositoryURI, repositoryURI.toString(), IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
			repoManager.removeRepository(repositoryURI);
			return result;
		} catch (ProvisionException e) {
			// just return null
			LogHelper.log(e);
		} catch (URISyntaxException e) {
			// just return null
		}
		return null;
	}

	private IMetadataRepositoryManager getManager() {
		return manager;
	}

	private boolean isLocal() {
		return "file".equalsIgnoreCase(location.getScheme()); //$NON-NLS-1$
	}

	public boolean isModifiable() {
		return isLocal();
	}

	CompositeMetadataRepository(IMetadataRepositoryManager manager, URI location, String name, Map<String, String> properties) {
		super(manager.getAgent(), name == null ? (location != null ? location.toString() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties); //$NON-NLS-1$
		this.manager = manager;
		//when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
		save();
	}

	/*
	 * This is only called by the parser when loading a repository.
	 */
	CompositeMetadataRepository(IMetadataRepositoryManager manager, CompositeRepositoryState state) {
		super(manager.getAgent(), state.getName(), state.getType(), state.getVersion(), state.getLocation(), state.getDescription(), state.getProvider(), state.getProperties());
		this.manager = manager;
		for (int i = 0; i < state.getChildren().length; i++)
			addChild(state.getChildren()[i], false);
	}

	/*
	 * Create and return a new repository state object which represents this repository.
	 * It will be used while persisting the repository to disk.
	 */
	public CompositeRepositoryState toState() {
		CompositeRepositoryState result = new CompositeRepositoryState();
		result.setName(getName());
		result.setType(getType());
		result.setVersion(getVersion());
		result.setLocation(getLocation());
		result.setDescription(getDescription());
		result.setProvider(getProvider());
		result.setProperties(getProperties());
		// it is important to directly access the field so we have the relative URIs
		result.setChildren(childrenURIs.toArray(new URI[childrenURIs.size()]));
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query.IQueryable#query(org.eclipse.equinox.p2.query.IQuery, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		try {
			// Query all the all the repositories this composite repo contains
			IQueryable<IInstallableUnit> queryable = QueryUtil.compoundQueryable(loadedRepos);
			return queryable.query(query, monitor);
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	private void addChild(URI childURI, boolean save) {
		URI absolute = URIUtil.makeAbsolute(childURI, location);
		if (childrenURIs.contains(childURI) || childrenURIs.contains(absolute))
			return;
		// always add the URI to the list of child URIs (even if we can't load it later)
		childrenURIs.add(childURI);
		if (save)
			save();
		try {
			boolean currentLoaded = getManager().contains(absolute);
			IMetadataRepository currentRepo = getManager().loadRepository(absolute, null);
			if (!currentLoaded) {
				//set enabled to false so repositories do not polled twice
				getManager().setEnabled(absolute, false);
				//set repository to system to hide from users
				getManager().setRepositoryProperty(absolute, IRepository.PROP_SYSTEM, String.valueOf(true));
			}
			// we successfully loaded the repo so remember it
			loadedRepos.add(currentRepo);
		} catch (ProvisionException e) {
			//repository failed to load. fall through
			LogHelper.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.ICompositeRepository#addChild(java.net.URI)
	 */
	public void addChild(URI childURI) {
		addChild(childURI, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.ICompositeRepository#removeChild(java.net.URI)
	 */
	public void removeChild(URI childURI) {
		boolean removed = childrenURIs.remove(childURI);
		// if the child wasn't there make sure and try the other permutation
		// (absolute/relative) to see if it really is in the list.
		URI other = childURI.isAbsolute() ? URIUtil.makeRelative(childURI, location) : URIUtil.makeAbsolute(childURI, location);
		if (!removed)
			removed = childrenURIs.remove(other);

		if (removed) {
			// we removed the child from the list so remove the associated repo object as well
			IMetadataRepository found = null;
			for (IMetadataRepository current : loadedRepos) {
				URI repoLocation = current.getLocation();
				if (URIUtil.sameURI(childURI, repoLocation) || URIUtil.sameURI(other, repoLocation)) {
					found = current;
					break;
				}
			}
			if (found != null)
				loadedRepos.remove(found);
			save();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.ICompositeRepository#removeAllChildren()
	 */
	public void removeAllChildren() {
		childrenURIs.clear();
		loadedRepos.clear();
		save();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository#addInstallableUnits(java.util.Collection)
	 */
	@Override
	public void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		throw new UnsupportedOperationException("Cannot add IUs to a composite repository"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository#removeAll()
	 */
	@Override
	public synchronized void removeAll() {
		throw new UnsupportedOperationException("Cannot remove IUs from a composite repository"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository#removeInstallableUnits(java.util.Collection)
	 */
	@Override
	public boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		throw new UnsupportedOperationException("Cannot remove IUs from a composite repository"); //$NON-NLS-1$
	}

	private static File getActualLocation(URI location, String extension) {
		File spec = URIUtil.toFile(location);
		String path = spec.getAbsolutePath();
		if (path.endsWith(CompositeMetadataRepositoryFactory.CONTENT_FILENAME + extension)) {
			//todo this is the old code that doesn't look right
			//			return new File(spec + extension);
			return spec;
		}
		if (path.endsWith("/")) //$NON-NLS-1$
			path += CompositeMetadataRepositoryFactory.CONTENT_FILENAME;
		else
			path += "/" + CompositeMetadataRepositoryFactory.CONTENT_FILENAME; //$NON-NLS-1$
		return new File(path + extension);
	}

	public static File getActualLocation(URI location) {
		return getActualLocation(location, XML_EXTENSION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository#addReferences(java.util.Collection)
	 */
	public synchronized void addReferences(Collection<? extends IRepositoryReference> references) {
		throw new UnsupportedOperationException("Cannot add References to a composite repository"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#getReferences()
	 */
	public Collection<IRepositoryReference> getReferences() {
		HashSet<IRepositoryReference> allRefs = new HashSet<IRepositoryReference>();
		for (IMetadataRepository child : loadedRepos)
			allRefs.addAll(child.getReferences());
		return allRefs;
	}

	// caller should be synchronized
	private void save() {
		if (!isModifiable())
			return;
		File file = getActualLocation(location);
		File jarFile = getActualLocation(location, JAR_EXTENSION);
		boolean compress = "true".equalsIgnoreCase(properties.get(PROP_COMPRESSED)); //$NON-NLS-1$
		try {
			OutputStream output = null;
			if (!compress) {
				if (jarFile.exists()) {
					jarFile.delete();
				}
				if (!file.exists()) {
					if (!file.getParentFile().exists())
						file.getParentFile().mkdirs();
					file.createNewFile();
				}
				output = new FileOutputStream(file);
			} else {
				if (file.exists()) {
					file.delete();
				}
				if (!jarFile.exists()) {
					if (!jarFile.getParentFile().exists())
						jarFile.getParentFile().mkdirs();
					jarFile.createNewFile();
				}
				JarEntry jarEntry = new JarEntry(file.getName());
				JarOutputStream jOutput = new JarOutputStream(new FileOutputStream(jarFile));
				jOutput.putNextEntry(jarEntry);
				output = jOutput;
			}
			super.setProperty(IRepository.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()));
			new CompositeRepositoryIO().write(toState(), output, PI_REPOSITORY_TYPE);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_WRITE, NLS.bind(Messages.io_failedWrite, location), e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.ICompositeRepository#getChildren()
	 */
	public List<URI> getChildren() {
		List<URI> result = new ArrayList<URI>();
		for (URI childURI : childrenURIs)
			result.add(URIUtil.makeAbsolute(childURI, location));
		return result;
	}

	public static URI getActualLocationURI(URI base, String extension) {
		if (extension == null)
			extension = XML_EXTENSION;
		return URIUtil.append(base, CompositeMetadataRepositoryFactory.CONTENT_FILENAME + extension);
	}

	//TODO this should never be called. What do we do?
	public void initialize(RepositoryState state) {
		this.name = state.Name;
		this.type = state.Type;
		this.version = state.Version.toString();
		this.provider = state.Provider;
		this.description = state.Description;
		this.location = state.Location;
		this.properties = state.Properties;
	}
}
