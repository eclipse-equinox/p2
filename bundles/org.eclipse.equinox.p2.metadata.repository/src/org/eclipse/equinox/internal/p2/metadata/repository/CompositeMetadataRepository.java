/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.ICompositeRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.osgi.util.NLS;

public class CompositeMetadataRepository extends AbstractMetadataRepository implements IMetadataRepository, ICompositeRepository {

	static final public String REPOSITORY_TYPE = CompositeMetadataRepository.class.getName();
	public static final String PI_REPOSITORY_TYPE = "compositeMetadataRepository"; //$NON-NLS-1$
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final public String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	static final private String JAR_EXTENSION = ".jar"; //$NON-NLS-1$

	// keep a list of the child URIs. they can be absolute or relative. they may or may not point
	// to a valid reachable repo
	private List childrenURIs = new ArrayList();
	// keep a list of the repositories that we have successfully loaded
	private List loadedRepos = new ArrayList();

	/**
	 * Create a Composite repository in memory.
	 * @return the repository or null if unable to create one
	 */
	public static CompositeMetadataRepository createMemoryComposite() {
		IMetadataRepositoryManager manager = getManager();
		if (manager == null)
			return null;
		try {
			//create a unique opaque URI 
			long time = System.currentTimeMillis();
			URI repositoryURI = new URI("memory:" + String.valueOf(time)); //$NON-NLS-1$
			while (manager.contains(repositoryURI))
				repositoryURI = new URI("memory:" + String.valueOf(++time)); //$NON-NLS-1$

			CompositeMetadataRepository result = (CompositeMetadataRepository) manager.createRepository(repositoryURI, repositoryURI.toString(), IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
			manager.removeRepository(repositoryURI);
			return result;
		} catch (ProvisionException e) {
			// just return null
			LogHelper.log(e);
		} catch (URISyntaxException e) {
			// just return null
		}
		return null;
	}

	static private IMetadataRepositoryManager getManager() {
		return (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
	}

	private boolean isLocal() {
		return "file".equalsIgnoreCase(location.getScheme()); //$NON-NLS-1$
	}

	public boolean isModifiable() {
		return isLocal();
	}

	public CompositeMetadataRepository(URI location, String name, Map properties) {
		super(name == null ? (location != null ? location.toString() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties); //$NON-NLS-1$
		//when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
		save();
	}

	/*
	 * This is only called by the parser when loading a repository.
	 */
	public CompositeMetadataRepository(CompositeRepositoryState state) {
		super(state.getName(), state.getType(), state.getVersion(), state.getLocation(), state.getDescription(), state.getProvider(), state.getProperties());
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
		result.setChildren((URI[]) childrenURIs.toArray(new URI[childrenURIs.size()]));
		return result;
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		try {
			// Query all the all the repositories this composite repo contains
			CompoundQueryable queryable = new CompoundQueryable((IQueryable[]) loadedRepos.toArray(new IQueryable[loadedRepos.size()]));
			collector = queryable.query(query, collector, monitor);
		} finally {
			if (monitor != null)
				monitor.done();
		}
		return collector;
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

	public void addChild(URI childURI) {
		addChild(childURI, true);
	}

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
			for (Iterator iter = loadedRepos.iterator(); found == null && iter.hasNext();) {
				IMetadataRepository current = (IMetadataRepository) iter.next();
				URI repoLocation = current.getLocation();
				if (URIUtil.sameURI(childURI, repoLocation))
					found = current;
				else if (URIUtil.sameURI(other, repoLocation))
					found = current;
			}
			if (found != null)
				loadedRepos.remove(found);
			save();
		}
	}

	public void removeAllChildren() {
		childrenURIs.clear();
		loadedRepos.clear();
		save();
	}

	public synchronized void addInstallableUnits(IInstallableUnit[] installableUnits) {
		throw new UnsupportedOperationException("Cannot add IUs to a composite repository");
	}

	public synchronized void removeAll() {
		throw new UnsupportedOperationException("Cannot remove IUs to a composite repository");
	}

	public synchronized boolean removeInstallableUnits(Query query, IProgressMonitor monitor) {
		throw new UnsupportedOperationException("Cannot remove IUs to a composite repository");
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

	public synchronized void addReference(URI repositoryLocation, int repositoryType, int options) {
		throw new UnsupportedOperationException("Cannot add References to a composite repository");
	}

	// caller should be synchronized
	private void save() {
		if (!isModifiable())
			return;
		File file = getActualLocation(location);
		File jarFile = getActualLocation(location, JAR_EXTENSION);
		boolean compress = "true".equalsIgnoreCase((String) properties.get(PROP_COMPRESSED)); //$NON-NLS-1$
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

	public List getChildren() {
		List result = new ArrayList();
		for (Iterator iter = childrenURIs.iterator(); iter.hasNext();)
			result.add(URIUtil.makeAbsolute((URI) iter.next(), location));
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
