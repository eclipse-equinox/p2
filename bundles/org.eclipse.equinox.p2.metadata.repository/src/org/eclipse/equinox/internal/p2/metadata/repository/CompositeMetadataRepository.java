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
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO.CompositeRepositoryState;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.ICompositeRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;

public class CompositeMetadataRepository extends AbstractMetadataRepository implements IMetadataRepository, ICompositeRepository {

	static final public String REPOSITORY_TYPE = CompositeMetadataRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final public String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	static final private String JAR_EXTENSION = ".jar"; //$NON-NLS-1$

	private ArrayList childrenURIs = new ArrayList();

	private IMetadataRepositoryManager getManager() {
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
		super(state.Name, state.Type, state.Version, null, state.Description, state.Provider, state.Properties);
		for (int i = 0; i < state.Children.length; i++) {
			if (!childrenURIs.contains(state.Children[i]))
				childrenURIs.add(state.Children[i]);
		}
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URI aLocation) {
		this.location = aLocation;
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		Iterator repositoryIterator = childrenURIs.iterator();
		SubMonitor sub = SubMonitor.convert(monitor, Messages.repo_loading, childrenURIs.size() * 100);
		try {
			while (repositoryIterator.hasNext()) {
				try {
					//Try to load the repositories one by one
					URI currentURI = (URI) repositoryIterator.next();
					SubMonitor loopMonitor = sub.newChild(100);
					boolean currentLoaded = getManager().contains(currentURI);
					IMetadataRepository currentRepo = getManager().loadRepository(currentURI, null);
					if (!currentLoaded) {
						//set enabled to false so repositories do not polled twice
						getManager().setEnabled(currentURI, false);
						//set repository to system to hide from users
						getManager().setRepositoryProperty(currentURI, IRepository.PROP_SYSTEM, String.valueOf(true));
					}
					loopMonitor.worked(50); // work 50% for the load
					//get the query results. Collector should take care of duplicates
					currentRepo.query(query, collector, loopMonitor.newChild(50));
				} catch (ProvisionException e) {
					//repository failed to load. fall through
					LogHelper.log(e);
				}
			}
		} finally {
			if (monitor != null)
				monitor.done();
		}
		return collector;
	}

	public void addChild(URI childURI) {
		if (!childrenURIs.contains(childURI)) {
			childrenURIs.add(childURI);
			save();
		}
	}

	public void removeChild(URI childURI) {
		childrenURIs.remove(childURI);
		save();
	}

	public void removeAllChildren() {
		childrenURIs.clear();
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
			new CompositeRepositoryIO().write(this, output);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_WRITE, "Error saving metadata repository: " + location, e)); //$NON-NLS-1$
		}
	}

	public ArrayList getChildren() {
		return childrenURIs;
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
