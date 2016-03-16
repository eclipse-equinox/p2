/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype Inc - ongoing development
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
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.osgi.util.NLS;

public class CompositeArtifactRepository extends AbstractArtifactRepository implements ICompositeRepository<IArtifactKey> {

	static final public String REPOSITORY_TYPE = CompositeArtifactRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = 1;
	static final public String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	static final public String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	static final public String CONTENT_FILENAME = "compositeArtifacts"; //$NON-NLS-1$
	public static final String PI_REPOSITORY_TYPE = "compositeArtifactRepository"; //$NON-NLS-1$
	static final public String PROP_ATOMIC_LOADING = "p2.atomic.composite.loading"; //$NON-NLS-1$

	static final public boolean ATOMIC_LOADING_DEFAULT = Boolean.parseBoolean(Activator.getContext().getProperty("eclipse.p2.atomic.composite.loading.default")); //$NON-NLS-1$

	// keep a list of the child URIs. they can be absolute or relative. they may or may not point
	// to a valid reachable repo
	private List<URI> childrenURIs = new ArrayList<URI>();
	// keep a list of the repositories that we have successfully loaded
	private List<ChildInfo> loadedRepos = new ArrayList<ChildInfo>();
	private IArtifactRepositoryManager manager;
	private boolean disableSave;

	/**
	 * Create a Composite repository in memory.
	 * @return the repository or null if unable to create one
	 */
	public static CompositeArtifactRepository createMemoryComposite(IProvisioningAgent agent) {
		if (agent == null)
			return null;
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			return null;
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
			LogHelper.log(e);
			// just return null
		} catch (URISyntaxException e) {
			// just return null
		}
		return null;
	}

	private IArtifactRepositoryManager getManager() {
		return manager;
	}

	/**
	 * This is only called by the parser when loading a repository.
	 */
	CompositeArtifactRepository(IArtifactRepositoryManager manager, CompositeRepositoryState state, IProgressMonitor monitor) throws ProvisionException {
		super(manager.getAgent(), state.getName(), state.getType(), state.getVersion(), state.getLocation(), state.getDescription(), state.getProvider(), state.getProperties());
		this.manager = manager;
		SubMonitor sub = SubMonitor.convert(monitor, 100 * state.getChildren().length);
		List<URI> repositoriesToBeRemovedOnFailure = new ArrayList<URI>();
		boolean failOnChildFailure = shouldFailOnChildFailure(state);
		for (URI child : state.getChildren())
			addChild(child, false, sub.newChild(100), failOnChildFailure, repositoriesToBeRemovedOnFailure);
	}

	/**
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	protected CompositeArtifactRepository(IArtifactRepositoryManager manager, URI location, String repositoryName, Map<String, String> properties) {
		super(manager.getAgent(), repositoryName, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties);
		this.manager = manager;
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
		result.setChildren(childrenURIs.toArray(new URI[childrenURIs.size()]));
		return result;
	}

	/*
	 * Add the given object to the specified list if it doesn't already exist
	 * in it. Return a boolean value indicating whether or not the object was 
	 * actually added.
	 */
	private static <T> boolean add(List<T> list, T obj) {
		return list.contains(obj) ? false : list.add(obj);
	}

	public static URI getActualLocation(URI base, boolean compress) {
		return getActualLocation(base, compress ? JAR_EXTENSION : XML_EXTENSION);
	}

	private static URI getActualLocation(URI base, String extension) {
		return URIUtil.append(base, CONTENT_FILENAME + extension);
	}

	private boolean isLocal() {
		return "file".equalsIgnoreCase(getLocation().getScheme()); //$NON-NLS-1$
	}

	public boolean isModifiable() {
		return isLocal();
	}

	public void addChild(URI childURI) {
		try {
			addChild(childURI, true, null, false, null);
		} catch (ProvisionException e) {
			//already logged
		}
	}

	//successfully loaded repo will be added to the list repositoriesToBeRemovedOnFailure if the list is not null and the repo wasn't previously loaded
	private void addChild(URI childURI, boolean save, IProgressMonitor monitor, boolean propagateException, List<URI> repositoriesToBeRemovedOnFailure) throws ProvisionException {
		SubMonitor sub = SubMonitor.convert(monitor);
		URI absolute = URIUtil.makeAbsolute(childURI, getLocation());
		if (childrenURIs.contains(childURI) || childrenURIs.contains(absolute)) {
			sub.done();
			return;
		}
		childrenURIs.add(childURI);
		if (save)
			save();
		try {
			boolean currentLoaded = getManager().contains(absolute);
			IArtifactRepository repo = load(childURI, sub);
			if (!currentLoaded && propagateException)
				repositoriesToBeRemovedOnFailure.add(absolute);
			loadedRepos.add(new ChildInfo(repo));
		} catch (ProvisionException e) {
			//repository failed to load. fall through
			LogHelper.log(e);
			if (propagateException) {
				removeFromRepoManager(repositoriesToBeRemovedOnFailure);
				String msg = NLS.bind(Messages.io_failedRead, getLocation());
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
			}
		}
	}

	//	public boolean addChild(URI childURI, String comparatorID) {
	//		try {
	//			IArtifactRepository repo = load(childURI);
	//			if (isSane(repo, comparatorID)) {
	//				addChild(childURI);
	//				//Add was successful
	//				return true;
	//			}
	//		} catch (ProvisionException e) {
	//			LogHelper.log(e);
	//		}
	//
	//		//Add was not successful
	//		return false;
	//	}

	public void removeChild(URI childURI) {
		boolean removed = childrenURIs.remove(childURI);
		// if the child wasn't there make sure and try the other permutation
		// (absolute/relative) to see if it really is in the list.
		URI other = childURI.isAbsolute() ? URIUtil.makeRelative(childURI, getLocation()) : URIUtil.makeAbsolute(childURI, getLocation());
		if (!removed)
			removed = childrenURIs.remove(other);

		if (removed) {
			// we removed the child from the list so remove the associated repo object as well
			ChildInfo found = null;
			for (ChildInfo current : loadedRepos) {
				URI repoLocation = current.repo.getLocation();
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

	public void removeAllChildren() {
		childrenURIs.clear();
		loadedRepos.clear();
		save();
	}

	public List<URI> getChildren() {
		List<URI> result = new ArrayList<URI>();
		for (URI uri : childrenURIs)
			result.add(URIUtil.makeAbsolute(uri, getLocation()));
		return result;
	}

	public List<IArtifactRepository> getLoadedChildren() {
		List<IArtifactRepository> result = new ArrayList<IArtifactRepository>(loadedRepos.size());
		for (ChildInfo info : loadedRepos) {
			result.add(info.repo);
		}
		return result;
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public synchronized void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedAddToComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedAddToComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedRemoveFromComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedRemoveFromComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedRemoveFromComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.exception_unsupportedRemoveFromComposite);
	}

	/**
	 * Composite repositories should be unable to directly modify their child repositories
	 */
	public synchronized void removeAll(IProgressMonitor monitor) {
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
		for (ChildInfo current : loadedRepos) {
			if (current.isGood() && current.repo.contains(key))
				return true;
		}
		return false;
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		for (ChildInfo current : loadedRepos) {
			if (current.isGood() && current.repo.contains(descriptor))
				return true;
		}
		return false;
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		ArrayList<IArtifactDescriptor> result = new ArrayList<IArtifactDescriptor>();
		for (ChildInfo current : loadedRepos) {
			if (current.isGood()) {
				IArtifactDescriptor[] tempResult = current.repo.getArtifactDescriptors(key);
				for (int i = 0; i < tempResult.length; i++)
					add(result, tempResult[i]);
			}
		}
		return result.toArray(new IArtifactDescriptor[result.size()]);
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, Messages.message_artifactsFromChildRepos, null);
		for (ChildInfo childInfo : loadedRepos) {
			if (requests.length == 0)
				break;
			IArtifactRepository current = childInfo.repo;
			IArtifactRequest[] applicable = getRequestsForRepository(current, requests);
			IStatus dlStatus = current.getArtifacts(applicable, subMonitor.newChild(requests.length));
			multiStatus.add(dlStatus);
			if (dlStatus.getSeverity() == IStatus.CANCEL)
				return multiStatus;
			requests = filterUnfetched(requests);
			subMonitor.setWorkRemaining(requests.length);

			if (subMonitor.isCanceled())
				return Status.CANCEL_STATUS;
		}
		return multiStatus;
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return getRawOrNormalArtifact(descriptor, destination, monitor, false);
	}

	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return getRawOrNormalArtifact(descriptor, destination, monitor, true);
	}

	private IStatus getRawOrNormalArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor, boolean raw) {
		for (Iterator<ChildInfo> childIterator = loadedRepos.iterator(); childIterator.hasNext();) {
			ChildInfo current = childIterator.next();
			if (current.isGood() && current.repo.contains(descriptor)) {
				// Child hasn't failed & contains descriptor
				IStatus status = raw ? current.repo.getRawArtifact(descriptor, destination, monitor) : current.repo.getArtifact(descriptor, destination, monitor);
				if (status.isOK()) {
					//we are done with this artifact so forgive bad children so they can try again on next artifact
					resetChildFailures();
					return Status.OK_STATUS;
				}
				// Download failed
				if (status.getCode() == CODE_RETRY || status.getCode() == IStatus.CANCEL)
					// Child has mirrors & wants to be retried, or we were canceled
					return status;
				// Child has failed us, mark it bad
				current.setBad(true);
				// If more children are available, set retry
				if (childIterator.hasNext())
					return new MultiStatus(Activator.ID, CODE_RETRY, new IStatus[] {status}, NLS.bind(Messages.retryRequest, descriptor.getArtifactKey(), current.repo.getLocation()), null);
				// Nothing that can be done, pass child's failure on
				resetChildFailures();
				return status;
			}
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
		}
		return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.artifact_not_found, descriptor));
	}

	/**
	 * Rests the failure state on all children to 'good'. This is done after a successful
	 * download to ensure that children who failed to obtain one artifact get a chance
	 * on the next artifact.
	 */
	private void resetChildFailures() {
		for (ChildInfo current : loadedRepos)
			current.setBad(false);
	}

	private IArtifactRequest[] filterUnfetched(IArtifactRequest[] requests) {
		ArrayList<IArtifactRequest> filteredRequests = new ArrayList<IArtifactRequest>();
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
		ArrayList<IArtifactRequest> applicable = new ArrayList<IArtifactRequest>();
		for (int i = 0; i < requests.length; i++) {
			if (repository.contains(requests[i].getArtifactKey()))
				applicable.add(requests[i]);
		}
		return applicable.toArray(new IArtifactRequest[applicable.size()]);
	}

	/**
	 * This method is only protected for testing purposes
	 * 
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void save() {
		if (disableSave)
			return;
		if (!isModifiable())
			return;
		boolean compress = "true".equalsIgnoreCase(getProperty(PROP_COMPRESSED)); //$NON-NLS-1$
		OutputStream os = null;
		try {
			URI actualLocation = getActualLocation(getLocation(), false);
			File artifactsFile = URIUtil.toFile(actualLocation);
			File jarFile = URIUtil.toFile(getActualLocation(getLocation(), true));
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
				os = new JarOutputStream(new FileOutputStream(jarFile));
				((JarOutputStream) os).putNextEntry(new JarEntry(new Path(artifactsFile.getAbsolutePath()).lastSegment()));
			}
			super.setProperty(IRepository.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()));
			new CompositeRepositoryIO().write(toState(), os, PI_REPOSITORY_TYPE);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_WRITE, NLS.bind(Messages.io_failedWrite, getLocation()), e));
		}
	}

	private IArtifactRepository load(URI repoURI, IProgressMonitor monitor) throws ProvisionException {
		// make sure we are dealing with an absolute location
		repoURI = URIUtil.makeAbsolute(repoURI, getLocation());
		boolean loaded = getManager().contains(repoURI);
		IArtifactRepository repo = getManager().loadRepository(repoURI, monitor);
		if (!loaded) {
			//set enabled to false so repositories do not get polled twice
			getManager().setEnabled(repoURI, false);
			//set repository to system to hide from users
			getManager().setRepositoryProperty(repoURI, IRepository.PROP_SYSTEM, String.valueOf(true));
		}
		return repo;
	}

	//	/**
	//	 * A method to check if the content of a repository is consistent with the other children by
	//	 * comparing content using the artifactComparator specified by the comparatorID
	//	 * @param toCheckRepo the repository to check
	//	 * @param comparatorID
	//	 * @return <code>true</code> if toCheckRepo is consistent, <code>false</code> if toCheckRepo 
	//	 * contains an equal descriptor to that of a child and they refer to different artifacts on disk.
	//	 */
	//	private boolean isSane(IArtifactRepository toCheckRepo, String comparatorID) {
	//		IArtifactComparator comparator = ArtifactComparatorFactory.getArtifactComparator(comparatorID);
	//		for (ChildInfo childInfo : loadedRepos) {
	//			IArtifactRepository current = childInfo.repo;
	//			if (!current.equals(toCheckRepo)) {
	//				if (!isSane(toCheckRepo, current, comparator))
	//					return false;
	//			}
	//		}
	//		return true;
	//	}
	//
	//	/*
	//	 * Check the two given repositories against each other using the given comparator.
	//	 */
	//	private boolean isSane(IArtifactRepository one, IArtifactRepository two, IArtifactComparator comparator) {
	//		IQueryResult<IArtifactKey> toCheckKeys = one.query(ArtifactKeyQuery.ALL_KEYS, null);
	//		for (Iterator<IArtifactKey> iterator = toCheckKeys.iterator(); iterator.hasNext();) {
	//			IArtifactKey key = iterator.next();
	//			if (!two.contains(key))
	//				continue;
	//			IArtifactDescriptor[] toCheckDescriptors = one.getArtifactDescriptors(key);
	//			IArtifactDescriptor[] currentDescriptors = two.getArtifactDescriptors(key);
	//			for (int j = 0; j < toCheckDescriptors.length; j++) {
	//				if (!two.contains(toCheckDescriptors[j]))
	//					continue;
	//				for (int k = 0; k < currentDescriptors.length; k++) {
	//					if (currentDescriptors[k].equals(toCheckDescriptors[j])) {
	//						IStatus compareResult = comparator.compare(two, currentDescriptors[k], two, toCheckDescriptors[j]);
	//						if (!compareResult.isOK()) {
	//							LogHelper.log(compareResult);
	//							return false;
	//						}
	//						break;
	//					}
	//				}
	//			}
	//		}
	//		return true;
	//	}
	//
	//	/**
	//	 * A method that verifies that all children with matching artifact descriptors contain the same set of bytes
	//	 * The verification is done using the artifactComparator specified by comparatorID
	//	 * Assumes more valuable logging and output is the responsibility of the artifactComparator implementation.
	//	 * @param comparatorID
	//	 * @returns true if the repository is consistent, false if two equal descriptors refer to different artifacts on disk.
	//	 */
	//	private boolean validate(String comparatorID) {
	//		IArtifactComparator comparator = ArtifactComparatorFactory.getArtifactComparator(comparatorID);
	//		ChildInfo[] repos = loadedRepos.toArray(new ChildInfo[loadedRepos.size()]);
	//		for (int outer = 0; outer < repos.length; outer++) {
	//			for (int inner = outer + 1; inner < repos.length; inner++) {
	//				if (!isSane(repos[outer].repo, repos[inner].repo, comparator))
	//					return false;
	//			}
	//		}
	//		return true;
	//	}

	private static class ChildInfo {
		IArtifactRepository repo;
		boolean good = true;

		ChildInfo(IArtifactRepository IArtifactRepository) {
			this.repo = IArtifactRepository;
		}

		void setBad(boolean bad) {
			good = !bad;
		}

		boolean isGood() {
			return good;
		}
	}

	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		// Query all the all the repositories this composite repo contains
		List<IArtifactRepository> repos = new ArrayList<IArtifactRepository>();
		for (ChildInfo info : loadedRepos) {
			if (info.isGood())
				repos.add(info.repo);
		}
		IQueryable<IArtifactKey> queryable = QueryUtil.compoundQueryable(repos);
		return queryable.query(query, monitor);
	}

	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		// Query all the all the repositories this composite repo contains
		List<IQueryable<IArtifactDescriptor>> repos = new ArrayList<IQueryable<IArtifactDescriptor>>();
		for (ChildInfo info : loadedRepos) {
			if (info.isGood())
				repos.add(info.repo.descriptorQueryable());
		}
		return QueryUtil.compoundQueryable(repos);
	}

	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		IStatus result = null;
		synchronized (this) {
			try {
				disableSave = true;
				runnable.run(monitor);
			} catch (OperationCanceledException oce) {
				return new Status(IStatus.CANCEL, Activator.ID, oce.getMessage(), oce);
			} catch (Throwable e) {
				result = new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e);
			} finally {
				disableSave = false;
				try {
					save();
				} catch (Exception e) {
					if (result != null)
						result = new MultiStatus(Activator.ID, IStatus.ERROR, new IStatus[] {result}, e.getMessage(), e);
					else
						result = new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e);
				}
			}
		}
		if (result == null)
			result = Status.OK_STATUS;
		return result;
	}

	private void removeFromRepoManager(List<URI> currentLoadedRepositories) {
		if (currentLoadedRepositories == null)
			return;
		for (URI loadedChild : currentLoadedRepositories) {
			manager.removeRepository(loadedChild);
		}
	}

	private boolean shouldFailOnChildFailure(CompositeRepositoryState state) {
		Map<String, String> repoProperties = state.getProperties();
		boolean failOnChildFailure = ATOMIC_LOADING_DEFAULT;
		if (repoProperties != null) {
			String value = repoProperties.get(PROP_ATOMIC_LOADING);
			if (value != null) {
				failOnChildFailure = Boolean.parseBoolean(value);
			}
		}
		return failOnChildFailure;
	}

	@Override
	public String toString() {
		return getChildren().toString();
	}
}
