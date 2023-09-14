/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.FrameworkUtil;

public class CompositeMetadataRepository extends AbstractMetadataRepository implements ICompositeRepository<IInstallableUnit>, IIndexProvider<IInstallableUnit> {

	static final public String REPOSITORY_TYPE = CompositeMetadataRepository.class.getName();
	static final public String PI_REPOSITORY_TYPE = "compositeMetadataRepository"; //$NON-NLS-1$
	static final public String PROP_ATOMIC_LOADING = "p2.atomic.composite.loading"; //$NON-NLS-1$

	static final public boolean ATOMIC_LOADING_DEFAULT = Boolean
			.parseBoolean(FrameworkUtil.getBundle(CompositeMetadataRepository.class).getBundleContext()
					.getProperty("eclipse.p2.atomic.composite.loading.default")); //$NON-NLS-1$

	static final private Integer REPOSITORY_VERSION = 1;
	static final public String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	static final private String JAR_EXTENSION = ".jar"; //$NON-NLS-1$

	// keep a list of the child URIs. they can be absolute or relative. they may or may not point
	// to a valid reachable repo
	private List<URI> childrenURIs = new ArrayList<>();
	// keep a list of the repositories that we have successfully loaded
	private List<IMetadataRepository> loadedRepos = new ArrayList<>();
	private IMetadataRepositoryManager manager;
	private IPool<IInstallableUnit> iuPool = new WeakPool<>();

	/**
	 * Create a Composite repository in memory.
	 * @return the repository or null if unable to create one
	 */
	public static CompositeMetadataRepository createMemoryComposite(IProvisioningAgent agent) {
		return (CompositeMetadataRepository) RepositoryHelper.createMemoryComposite(agent,
				IMetadataRepositoryManager.class, IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY);
	}

	private IMetadataRepositoryManager getManager() {
		return manager;
	}

	private boolean isLocal() {
		return "file".equalsIgnoreCase(getLocation().getScheme()); //$NON-NLS-1$
	}

	@Override
	public boolean isModifiable() {
		return isLocal();
	}

	/*
	 * This is only called by the parser when loading a repository.
	 */
	CompositeMetadataRepository(IMetadataRepositoryManager manager, CompositeRepositoryState state, IProgressMonitor monitor) throws ProvisionException {
		super(manager.getAgent(), state.getName(), state.getType(), state.getVersion(), state.getLocation(), state.getDescription(), state.getProvider(), state.getProperties());
		this.manager = manager;
		SubMonitor sub = SubMonitor.convert(monitor, 100 * state.getChildren().length);
		List<URI> repositoriesToBeRemovedOnFailure = new ArrayList<>();
		boolean failOnChildFailure = shouldFailOnChildFailure(state);
		for (URI child : state.getChildren())
			addChild(child, false, sub.newChild(100), failOnChildFailure, repositoriesToBeRemovedOnFailure);

	}

	CompositeMetadataRepository(IMetadataRepositoryManager manager, URI location, String name, Map<String, String> properties) {
		super(manager.getAgent(), name == null ? (location != null ? location.toString() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties); //$NON-NLS-1$
		this.manager = manager;
		//when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
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

	@Override
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

	@Override
	public boolean contains(IInstallableUnit element) {
		for (IMetadataRepository repository : loadedRepos) {
			if (repository.contains(element)) {
				return true;
			}
		}
		return false;
	}

	//successfully loaded repo will be added to the list repositoriesToBeRemovedOnFailure if the list is not null and the repo wasn't previously loaded
	private void addChild(URI childURI, boolean save, IProgressMonitor monitor, boolean propagateException, List<URI> repositoriesToBeRemovedOnFailure) throws ProvisionException {
		SubMonitor sub = SubMonitor.convert(monitor);
		URI absolute = URIUtil.makeAbsolute(childURI, getLocation());
		if (childrenURIs.contains(childURI) || childrenURIs.contains(absolute)) {
			sub.done();
			return;

		}
		// always add the URI to the list of child URIs (even if we can't load it later)
		childrenURIs.add(childURI);
		if (save)
			save();
		try {
			boolean currentLoaded = getManager().contains(absolute);
			IMetadataRepository currentRepo = getManager().loadRepository(absolute, sub);
			if (!currentLoaded) {
				//set enabled to false so repositories do not polled twice
				getManager().setEnabled(absolute, false);
				//set repository to system to hide from users
				getManager().setRepositoryProperty(absolute, IRepository.PROP_SYSTEM, String.valueOf(true));
				if (propagateException)
					repositoriesToBeRemovedOnFailure.add(absolute);
			}
			currentRepo.compress(iuPool); // Share IUs across this CompositeMetadataRepository
			// we successfully loaded the repo so remember it
			loadedRepos.add(currentRepo);

		} catch (ProvisionException e) {
			//repository failed to load. fall through
			LogHelper.log(e);
			if (propagateException) {
				removeFromRepoManager(repositoriesToBeRemovedOnFailure);
				String msg = NLS.bind(Messages.io_failedRead, getLocation());
				throw new ProvisionException(new Status(IStatus.ERROR, Constants.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, e));
			}
		}
	}

	@Override
	public void addChild(URI childURI) {
		try {
			addChild(childURI, true, null, false, null);
		} catch (ProvisionException e) {
			//already logged
		}
	}

	@Override
	public void removeChild(URI childURI) {
		boolean removed = childrenURIs.remove(childURI);
		// if the child wasn't there make sure and try the other permutation
		// (absolute/relative) to see if it really is in the list.
		URI other = childURI.isAbsolute() ? URIUtil.makeRelative(childURI, getLocation()) : URIUtil.makeAbsolute(childURI, getLocation());
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

	@Override
	public void removeAllChildren() {
		childrenURIs.clear();
		loadedRepos.clear();
		save();
	}

	@Override
	public void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		throw new UnsupportedOperationException("Cannot add IUs to a composite repository"); //$NON-NLS-1$
	}

	@Override
	public synchronized void removeAll() {
		throw new UnsupportedOperationException("Cannot remove IUs from a composite repository"); //$NON-NLS-1$
	}

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
		return new File(path, CompositeMetadataRepositoryFactory.CONTENT_FILENAME + extension);
	}

	public static File getActualLocation(URI location) {
		return getActualLocation(location, XML_EXTENSION);
	}

	@Override
	public synchronized void addReferences(Collection<? extends IRepositoryReference> references) {
		throw new UnsupportedOperationException("Cannot add References to a composite repository"); //$NON-NLS-1$
	}

	@Override
	public synchronized boolean removeReferences(Collection<? extends IRepositoryReference> references) {
		throw new UnsupportedOperationException("Cannot remove References to a composite repository"); //$NON-NLS-1$
	}

	@Override
	public Collection<IRepositoryReference> getReferences() {
		Set<IRepositoryReference> allRefs = new HashSet<>();
		for (IMetadataRepository child : loadedRepos) {
			allRefs.addAll(child.getReferences());
		}
		return allRefs;
	}

	// caller should be synchronized
	private void save() {
		if (!isModifiable())
			return;
		File file = getActualLocation(getLocation());
		File jarFile = getActualLocation(getLocation(), JAR_EXTENSION);
		boolean compress = "true".equalsIgnoreCase(getProperty(PROP_COMPRESSED)); //$NON-NLS-1$
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
				output = new JarOutputStream(new FileOutputStream(jarFile));
				((JarOutputStream) output).putNextEntry(jarEntry);
			}
			super.setProperty(IRepository.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()));
			new CompositeRepositoryIO().write(toState(), output, PI_REPOSITORY_TYPE);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Constants.ID, ProvisionException.REPOSITORY_FAILED_WRITE, NLS.bind(Messages.io_failedWrite, getLocation()), e));
		}
	}

	@Override
	public List<URI> getChildren() {
		List<URI> result = new ArrayList<>();
		for (URI childURI : childrenURIs)
			result.add(URIUtil.makeAbsolute(childURI, getLocation()));
		return result;
	}

	public static URI getActualLocationURI(URI base, String extension) {
		if (extension == null)
			extension = XML_EXTENSION;
		return URIUtil.append(base, CompositeMetadataRepositoryFactory.CONTENT_FILENAME + extension);
	}

	//TODO this should never be called. What do we do?
	@Override
	public void initialize(RepositoryState state) {
		setName(state.Name);
		setType(state.Type);
		setVersion(state.Version.toString());
		setProvider(state.Provider);
		setDescription(state.Description);
		setLocation(state.Location);
		setProperties(state.Properties);
	}

	@Override
	@SuppressWarnings("unchecked")
	public IIndex<IInstallableUnit> getIndex(String memberName) {
		IQueryable<IInstallableUnit> queryable = QueryUtil.compoundQueryable(loadedRepos);
		if (queryable instanceof IIndexProvider<?>) {
			return ((IIndexProvider<IInstallableUnit>) queryable).getIndex(memberName);
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<IInstallableUnit> everything() {
		IQueryable<IInstallableUnit> queryable = QueryUtil.compoundQueryable(loadedRepos);
		if (queryable instanceof IIndexProvider<?>) {
			return ((IIndexProvider<IInstallableUnit>) queryable).everything();
		}
		return Collections.EMPTY_LIST.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getManagedProperty(Object client, String memberName, Object key) {
		IQueryable<IInstallableUnit> queryable = QueryUtil.compoundQueryable(loadedRepos);
		if (queryable instanceof IIndexProvider<?>) {
			return ((IIndexProvider<IInstallableUnit>) queryable).getManagedProperty(client, memberName, key);
		}
		return null;
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
