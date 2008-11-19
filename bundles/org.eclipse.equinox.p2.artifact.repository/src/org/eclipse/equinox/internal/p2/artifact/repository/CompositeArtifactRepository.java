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
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO.CompositeRepositoryState;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.ICompositeRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.AbstractArtifactRepository;

public class CompositeArtifactRepository extends AbstractArtifactRepository implements IArtifactRepository, ICompositeRepository {

	static final public String REPOSITORY_TYPE = CompositeArtifactRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final public String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	static final public String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	static final public String CONTENT_FILENAME = "compositeArtifacts"; //$NON-NLS-1$

	private ArrayList childrenURIs = new ArrayList();

	private IArtifactRepositoryManager getManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	/*
	 * This is only called by the parser when loading a repository.
	 */
	public CompositeArtifactRepository(CompositeRepositoryState state) {
		super(state.Name, state.Type, state.Version, null, state.Description, state.Provider, state.Properties);
		for (int i = 0; i < state.Children.length; i++) {
			//duplicate checking
			if (!childrenURIs.contains(state.Children[i]))
				childrenURIs.add(state.Children[i]);
		}
	}

	public CompositeArtifactRepository(String repositoryName, URI location, Map properties) {
		super(repositoryName, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties);
		initializeAfterLoad(location);
		save();
	}

	public static URI getActualLocation(URI base, boolean compress) {
		return getActualLocation(base, compress ? JAR_EXTENSION : XML_EXTENSION);
	}

	private static URI getActualLocation(URI base, String extension) {
		final String name = CONTENT_FILENAME + extension;
		String spec = base.toString();
		if (spec.endsWith(name))
			return base;
		if (spec.endsWith("/")) //$NON-NLS-1$
			spec += name;
		else
			spec += "/" + name; //$NON-NLS-1$
		try {
			return new URI(spec);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	private boolean isLocal() {
		return "file".equalsIgnoreCase(location.getScheme()); //$NON-NLS-1$
	}

	public boolean isModifiable() {
		return isLocal();
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URI location) {
		this.location = location;
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

	public ArrayList getChildren() {
		return childrenURIs;
	}

	/**
	 * Composite repositories should be unable to directly modify their sub repositories
	 */
	public synchronized void addDescriptor(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException("Cannot add descriptors to a composite repository");
	}

	/**
	 * Composite repositories should be unable to directly modify their sub repositories
	 */
	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		throw new UnsupportedOperationException("Cannot add descriptors to a composite repository");
	}

	/**
	 * Composite repositories should be unable to directly modify their sub repositories
	 */
	public void removeDescriptor(IArtifactKey key) {
		throw new UnsupportedOperationException("Cannot remove descriptors from a composite repository");
	}

	/**
	 * Composite repositories should be unable to directly modify their sub repositories
	 */
	public void removeDescriptor(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException("Cannot remove descriptors from a composite repository");
	}

	/**
	 * Composite repositories should be unable to directly modify their sub repositories
	 */
	public synchronized void removeAll() {
		throw new UnsupportedOperationException("Cannot remove descriptors from a composite repository");
	}

	public boolean contains(IArtifactKey key) {
		boolean contains = false;
		for (Iterator repositoryIterator = childrenURIs.iterator(); repositoryIterator.hasNext() && !contains;) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				boolean currentLoaded = getManager().contains(currentURI);
				IArtifactRepository current = getManager().loadRepository(currentURI, null);
				if (!currentLoaded) {
					//set enabled to false so repositories do not polled twice
					getManager().setEnabled(currentURI, false);
					//set repository to system to hide from users
					current.setProperty(IRepository.PROP_SYSTEM, "true");
				}
				contains = current.contains(key);
			} catch (ProvisionException e) {
				//repository failed to load. fall through
				LogHelper.log(e);
			}
		}
		return contains;
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		boolean contains = false;
		for (Iterator repositoryIterator = childrenURIs.iterator(); repositoryIterator.hasNext() && !contains;) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				boolean currentLoaded = getManager().contains(currentURI);
				IArtifactRepository current = getManager().loadRepository(currentURI, null);
				if (!currentLoaded) {
					//set enabled to false so repositories do not polled twice
					getManager().setEnabled(currentURI, false);
					//set repository to system to hide from users
					current.setProperty(IRepository.PROP_SYSTEM, "true");
				}
				contains = current.contains(descriptor);
			} catch (ProvisionException e) {
				//repository failed to load. fall through
				LogHelper.log(e);
			}
		}
		return contains;
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		ArrayList result = new ArrayList();
		for (Iterator repositoryIterator = childrenURIs.iterator(); repositoryIterator.hasNext();) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				boolean currentLoaded = getManager().contains(currentURI);
				IArtifactRepository current = getManager().loadRepository(currentURI, null);
				if (!currentLoaded) {
					//set enabled to false so repositories do not polled twice
					getManager().setEnabled(currentURI, false);
					//set repository to system to hide from users
					current.setProperty(IRepository.PROP_SYSTEM, "true");
				}
				IArtifactDescriptor[] tempResult = current.getArtifactDescriptors(key);
				for (int i = 0; i < tempResult.length; i++)
					//duplicate checking
					if (!result.contains(tempResult[i]))
						result.add(tempResult[i]);
			} catch (ProvisionException e) {
				//repository failed to load. fall through
				LogHelper.log(e);
			}
		}
		return (IArtifactDescriptor[]) result.toArray(new IArtifactDescriptor[result.size()]);
	}

	public IArtifactKey[] getArtifactKeys() {
		ArrayList result = new ArrayList();
		for (Iterator repositoryIterator = childrenURIs.iterator(); repositoryIterator.hasNext();) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				boolean currentLoaded = getManager().contains(currentURI);
				IArtifactRepository current = getManager().loadRepository(currentURI, null);
				if (!currentLoaded) {
					//set enabled to false so repositories do not polled twice
					getManager().setEnabled(currentURI, false);
					//set repository to system to hide from users
					current.setProperty(IRepository.PROP_SYSTEM, "true");
				}
				IArtifactKey[] tempResult = current.getArtifactKeys();
				for (int i = 0; i < tempResult.length; i++)
					//duplicate checking
					if (!result.contains(tempResult[i]))
						result.add(tempResult[i]);
			} catch (ProvisionException e) {
				//repository failed to load. fall through
				LogHelper.log(e);
			}
		}
		return (IArtifactKey[]) result.toArray(new IArtifactKey[result.size()]);
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, "Messages while trying children repositories.", null);
		for (Iterator repositoryIterator = childrenURIs.iterator(); repositoryIterator.hasNext() && requests.length > 0;) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				boolean currentLoaded = getManager().contains(currentURI);
				IArtifactRepository current = getManager().loadRepository(currentURI, null);
				if (!currentLoaded) {
					//set enabled to false so repositories do not polled twice
					getManager().setEnabled(currentURI, false);
					//set repository to system to hide from users
					current.setProperty(IRepository.PROP_SYSTEM, "true");
				}
				IArtifactRequest[] applicable = getRequestsForRepository(current, requests);
				IStatus dlStatus = current.getArtifacts(applicable, subMonitor.newChild(requests.length));
				multiStatus.add(dlStatus);
				if (dlStatus.getSeverity() == IStatus.CANCEL)
					return multiStatus;
				requests = filterUnfetched(requests);
				subMonitor.setWorkRemaining(requests.length);
			} catch (ProvisionException e) {
				//repository failed the load. Fall through.
				LogHelper.log(e);
			}
		}
		return multiStatus;
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, childrenURIs.size());
		Iterator repositoryIterator = childrenURIs.iterator();
		MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, "Messages while trying children repositories.", null);
		while (repositoryIterator.hasNext()) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				boolean currentLoaded = getManager().contains(currentURI);
				IArtifactRepository current = getManager().loadRepository(currentURI, null);
				if (!currentLoaded) {
					//set enabled to false so repositories do not polled twice
					getManager().setEnabled(currentURI, false);
					//set repository to system to hide from users
					current.setProperty(IRepository.PROP_SYSTEM, "true");
				}
				IStatus status = current.getArtifact(descriptor, destination, subMonitor.newChild(1));
				if (status.isOK())
					return status;
				//getArtifact failed
				multiStatus.add(status);
			} catch (ProvisionException e) {
				//repository failed the load. Fall through.
				LogHelper.log(e);
			}
		}
		return multiStatus;
	}

	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, childrenURIs.size());
		Iterator repositoryIterator = childrenURIs.iterator();
		MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, "Messages while trying children repositories.", null);
		while (repositoryIterator.hasNext()) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				boolean currentLoaded = getManager().contains(currentURI);
				IArtifactRepository current = getManager().loadRepository(currentURI, null);
				if (!currentLoaded) {
					//set enabled to false so repositories do not polled twice
					getManager().setEnabled(currentURI, false);
					//set repository to system to hide from users
					current.setProperty(IRepository.PROP_SYSTEM, "true");
				}
				IStatus status = current.getRawArtifact(descriptor, destination, subMonitor.newChild(1));
				if (status.isOK())
					return status;
				//getRawArtifact failed
				multiStatus.add(status);
			} catch (ProvisionException e) {
				//repository failed the load. Fall through.
				LogHelper.log(e);
			}
		}
		return multiStatus;
	}

	private IArtifactRequest[] filterUnfetched(IArtifactRequest[] requests) {
		ArrayList filteredRequests = new ArrayList();
		for (int i = 0; i < requests.length; i++) {
			if (requests[i].getResult() == null || !requests[i].getResult().isOK()) {
				filteredRequests.add(requests[i]);
			}
		}

		IArtifactRequest[] filteredArtifactRequests = new IArtifactRequest[filteredRequests.size()];
		filteredRequests.toArray(filteredArtifactRequests);
		return filteredArtifactRequests;
	}

	private IArtifactRequest[] getRequestsForRepository(IArtifactRepository repository, IArtifactRequest[] requests) {
		ArrayList applicable = new ArrayList();
		for (int i = 0; i < requests.length; i++) {
			if (repository.contains(requests[i].getArtifactKey()))
				applicable.add(requests[i]);
		}
		return (IArtifactRequest[]) applicable.toArray(new IArtifactRequest[applicable.size()]);
	}

	public void save() {
		boolean compress = "true".equalsIgnoreCase((String) properties.get(PROP_COMPRESSED)); //$NON-NLS-1$
		save(compress);
	}

	public void save(boolean compress) {
		assertModifiable();
		OutputStream os = null;
		try {
			try {
				URI actualLocation = getActualLocation(location, false);
				File artifactsFile = URIUtil.toFile(actualLocation);
				File jarFile = URIUtil.toFile(getActualLocation(location, true));
				if (!compress) {
					if (jarFile.exists()) {
						jarFile.delete();
					}
					if (!artifactsFile.exists()) {
						// create parent folders
						artifactsFile.getParentFile().mkdirs();
					}
					os = new FileOutputStream(artifactsFile);
				} else {
					if (artifactsFile.exists()) {
						artifactsFile.delete();
					}
					if (!jarFile.exists()) {
						if (!jarFile.getParentFile().exists())
							jarFile.getParentFile().mkdirs();
						jarFile.createNewFile();
					}
					JarOutputStream jOs = new JarOutputStream(new FileOutputStream(jarFile));
					jOs.putNextEntry(new JarEntry(new Path(artifactsFile.getAbsolutePath()).lastSegment()));
					os = jOs;
				}
				super.setProperty(IRepository.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()));
				new CompositeRepositoryIO().write(this, os);
			} catch (IOException e) {
				// TODO proper exception handling
				e.printStackTrace();
			} finally {
				if (os != null)
					os.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
