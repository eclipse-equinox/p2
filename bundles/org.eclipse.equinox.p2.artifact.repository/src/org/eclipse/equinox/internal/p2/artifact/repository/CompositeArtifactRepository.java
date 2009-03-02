/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
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
	public static final String PI_REPOSITORY_TYPE = "compositeArtifactRepository"; //$NON-NLS-1$

	// locations of the children repositories... can be absolute or relative.
	private List childrenURIs = new ArrayList();

	/**
	 * Create a Composite repository in memory.
	 * @return the repository or null if unable to create one
	 */
	public static CompositeArtifactRepository createMemoryComposite() {
		IArtifactRepositoryManager manager = getManager();
		if (manager != null) {
			try {
				//create a unique URI
				long time = System.currentTimeMillis();
				URI repositoryURI = new URI("memory:" + String.valueOf(time)); //$NON-NLS-1$
				while (manager.contains(repositoryURI))
					repositoryURI = new URI("memory:" + String.valueOf(++time)); //$NON-NLS-1$

				CompositeArtifactRepository result = (CompositeArtifactRepository) manager.createRepository(repositoryURI, repositoryURI.toString(), IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
				manager.removeRepository(repositoryURI);
				return result;
			} catch (ProvisionException e) {
				// just return null
			} catch (URISyntaxException e) {
				// just return null
			}
		}
		return null;
	}

	static private IArtifactRepositoryManager getManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	/*
	 * This is only called by the parser when loading a repository.
	 */
	public CompositeArtifactRepository(CompositeRepositoryState state) {
		super(state.getName(), state.getType(), state.getVersion(), null, state.getDescription(), state.getProvider(), state.getProperties());
		for (int i = 0; i < state.getChildren().length; i++)
			add(childrenURIs, state.getChildren()[i]);
	}

	public CompositeArtifactRepository(URI location, String repositoryName, Map properties) {
		super(repositoryName, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties);
		initializeAfterLoad(location);
		save();
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

	/*
	 * Add the given object to the specified list if it doesn't already exist
	 * in it. Return a boolean value indicating whether or not the object was 
	 * actually added.
	 */
	private static boolean add(List list, Object obj) {
		return list.contains(obj) ? false : list.add(obj);
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
	public synchronized void initializeAfterLoad(URI repoLocation) {
		this.location = repoLocation;
	}

	public void addChild(URI childURI) {
		if (add(childrenURIs, childURI))
			save();
	}

	public boolean addChild(URI childURI, String comparatorID) {
		if (isSane(childURI, comparatorID)) {
			addChild(childURI);
			//Add was successful
			return true;
		}

		//Add was not successful
		return false;
	}

	public void removeChild(URI childURI) {
		boolean removed = childrenURIs.remove(childURI);
		// if the child wasn't there make sure and try the other permutation
		// (absolute/relative) to see if it really is in the list.
		if (!removed) {
			if (childURI.isAbsolute())
				removed = childrenURIs.remove(URIUtil.makeRelative(childURI, location));
			else
				removed = childrenURIs.remove(URIUtil.makeAbsolute(childURI, location));
		}
		if (removed)
			save();
	}

	public void removeAllChildren() {
		childrenURIs.clear();
		save();
	}

	public List getChildren() {
		List result = new ArrayList();
		for (Iterator iter = childrenURIs.iterator(); iter.hasNext();)
			result.add(URIUtil.makeAbsolute((URI) iter.next(), location));
		return result;
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public synchronized void addDescriptor(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedAddToComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedAddToComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public void removeDescriptor(IArtifactKey key) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedRemoveFromComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public void removeDescriptor(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedRemoveFromComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public synchronized void removeAll() {
		throw new UnsupportedOperationException(Messages.exception_unsupportedRemoveFromComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories,
	 * Composite repositories should not have their own content.
	 * Therefore, they should not be allowed to have OutputStreams
	 */
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedGetOutputStream);
	}

	public boolean contains(IArtifactKey key) {
		for (Iterator repositoryIterator = childrenURIs.iterator(); repositoryIterator.hasNext();) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				IArtifactRepository current = load(currentURI);
				if (current.contains(key))
					return true;
			} catch (ProvisionException e) {
				//repository failed to load. fall through
				LogHelper.log(e);
			}
		}
		return false;
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		for (Iterator repositoryIterator = childrenURIs.iterator(); repositoryIterator.hasNext();) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				IArtifactRepository current = load(currentURI);
				if (current.contains(descriptor))
					return true;
			} catch (ProvisionException e) {
				//repository failed to load. fall through
				LogHelper.log(e);
			}
		}
		return false;
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		ArrayList result = new ArrayList();
		for (Iterator repositoryIterator = childrenURIs.iterator(); repositoryIterator.hasNext();) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				IArtifactRepository current = load(currentURI);
				IArtifactDescriptor[] tempResult = current.getArtifactDescriptors(key);
				for (int i = 0; i < tempResult.length; i++)
					add(result, tempResult[i]);
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
				IArtifactRepository current = load(currentURI);
				IArtifactKey[] tempResult = current.getArtifactKeys();
				for (int i = 0; i < tempResult.length; i++)
					add(result, tempResult[i]);
			} catch (ProvisionException e) {
				//repository failed to load. fall through
				LogHelper.log(e);
			}
		}
		return (IArtifactKey[]) result.toArray(new IArtifactKey[result.size()]);
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, Messages.message_childrenRepos, null);
		for (Iterator repositoryIterator = childrenURIs.iterator(); repositoryIterator.hasNext() && requests.length > 0;) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				IArtifactRepository current = load(currentURI);
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
		MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, Messages.message_childrenRepos, null);
		while (repositoryIterator.hasNext()) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				IArtifactRepository current = load(currentURI);
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
		MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, Messages.message_childrenRepos, null);
		while (repositoryIterator.hasNext()) {
			try {
				URI currentURI = (URI) repositoryIterator.next();
				IArtifactRepository current = load(currentURI);
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

	private void save() {
		if (!isModifiable())
			return;
		boolean compress = "true".equalsIgnoreCase((String) properties.get(PROP_COMPRESSED)); //$NON-NLS-1$
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
				new CompositeRepositoryIO().write(toState(), os, PI_REPOSITORY_TYPE);
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

	private IArtifactRepository load(URI repoURI) throws ProvisionException {
		// make sure we are dealing with an absolute location
		repoURI = URIUtil.makeAbsolute(repoURI, location);
		boolean loaded = getManager().contains(repoURI);
		IArtifactRepository repo = getManager().loadRepository(repoURI, null);
		if (!loaded) {
			//set enabled to false so repositories do not get polled twice
			getManager().setEnabled(repoURI, false);
			//set repository to system to hide from users
			getManager().setRepositoryProperty(repoURI, IRepository.PROP_SYSTEM, String.valueOf(true));
		}
		return repo;
	}

	/**
	 * A wrapper method that ensures a specified repository is compared against all children.
	 * @param toCheckURI
	 * @param comparatorID
	 * @return true if toCheckRepo is consistent, false if toCheckRepo contains an equal descriptor to that of s child and they refer to different artifacts on disk.
	 */
	private boolean isSane(URI toCheckURI, String comparatorID) {
		return isSane(toCheckURI, comparatorID, 0);
	}

	/**
	 * A method to check if the content of a repository is consistent with the other children by
	 * comparing content using the artifactComparator specified by the comparatorID
	 * startingIndex is used for optimization purposes (ensuring no redundant or self checks are made)
	 * @param toCheckURI
	 * @param comparatorID
	 * @param startingIndex
	 * @return true if toCheckRepo is consistent, false if toCheckRepo contains an equal descriptor to that of s child and they refer to different artifacts on disk.
	 */
	private boolean isSane(URI toCheckURI, String comparatorID, int startingIndex) {
		IArtifactRepository toCheckRepo = null;
		try {
			toCheckRepo = load(toCheckURI);
		} catch (ProvisionException e) {
			//repository failed the load.
			LogHelper.log(e);
			return false;
		}

		IArtifactComparator comparator = ArtifactComparatorFactory.getArtifactComparator(comparatorID);
		for (int m = startingIndex; m < childrenURIs.size(); m++) {
			try {
				URI currentURI = (URI) childrenURIs.get(m);
				IArtifactRepository current = load(currentURI);
				if (!current.equals(toCheckRepo)) {
					IArtifactKey[] toCheckKeys = toCheckRepo.getArtifactKeys();
					for (int i = 0; i < toCheckKeys.length; i++) {
						IArtifactKey key = toCheckKeys[i];
						if (!current.contains(key))
							continue;

						IArtifactDescriptor[] toCheckDescriptors = toCheckRepo.getArtifactDescriptors(key);
						IArtifactDescriptor[] currentDescriptors = current.getArtifactDescriptors(key);
						for (int j = 0; j < toCheckDescriptors.length; j++) {
							if (!current.contains(toCheckDescriptors[j]))
								continue;
							for (int k = 0; k < currentDescriptors.length; k++) {
								if (currentDescriptors[k].equals(toCheckDescriptors[j])) {
									IStatus compareResult = comparator.compare(current, currentDescriptors[k], toCheckRepo, toCheckDescriptors[j]);
									if (!compareResult.isOK()) {
										LogHelper.log(compareResult);
										return false;
									}
									break;
								}
							}
						}
					}
				}
			} catch (ProvisionException e) {
				//repository failed the load.
				LogHelper.log(e);
			}
		}
		return true;
	}

	/**
	 * A method that verifies that all children with matching artifact descriptors contain the same set of bytes
	 * The verification is done using the artifactComparator specified by comparatorID
	 * Assumes more valuable logging and output is the responsibility of the artifactComparator implementation.
	 * @param comparatorID
	 * @returns true if the repository is consistent, false if two equal descriptors refer to different artifacts on disk.
	 */
	public boolean validate(String comparatorID) {
		for (int i = 0; i < childrenURIs.size(); i++) {
			if (!isSane((URI) childrenURIs.get(i), comparatorID, i + 1))
				return false;
		}
		return true;
	}
}
