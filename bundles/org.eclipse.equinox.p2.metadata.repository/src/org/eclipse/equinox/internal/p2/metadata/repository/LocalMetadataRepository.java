/*******************************************************************************
 *  Copyright (c) 2007, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Prashant Deva - Bug 194674 [prov] Provide write access to metadata repository
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.internal.p2.metadata.index.*;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.KeyWithLocale;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;

/**
 * A metadata repository that resides in the local file system.  If the repository
 * location is a directory, this implementation will traverse the directory structure
 * and combine any metadata repository files that are found.
 */
public class LocalMetadataRepository extends AbstractMetadataRepository implements IIndexProvider<IInstallableUnit> {

	static final private String CONTENT_FILENAME = "content"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = LocalMetadataRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = 1;
	static final private String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	static final private String XML_EXTENSION = ".xml"; //$NON-NLS-1$

	protected IUMap units = new IUMap();
	protected HashSet<IRepositoryReference> repositories = new HashSet<IRepositoryReference>();
	private IIndex<IInstallableUnit> idIndex;
	private IIndex<IInstallableUnit> capabilityIndex;
	private TranslationSupport translationSupport;
	private boolean snapshotNeeded = false;
	private boolean disableSave = false;

	private static File getActualLocation(URI location, String extension) {
		File spec = URIUtil.toFile(location);
		String path = spec.getAbsolutePath();
		if (path.endsWith(CONTENT_FILENAME + extension)) {
			//todo this is the old code that doesn't look right
			//			return new File(spec + extension);
			return spec;
		}
		if (path.endsWith("/")) //$NON-NLS-1$
			path += CONTENT_FILENAME;
		else
			path += "/" + CONTENT_FILENAME; //$NON-NLS-1$
		return new File(path + extension);
	}

	public static File getActualLocation(URI location) {
		return getActualLocation(location, XML_EXTENSION);
	}

	/**
	 * This no argument constructor is called when restoring an existing repository.
	 */
	public LocalMetadataRepository(IProvisioningAgent agent) {
		super(agent);
	}

	/**
	 * This constructor is used when creating a new local repository.
	 * @param location The location of the repository
	 * @param name The name of the repository
	 */
	public LocalMetadataRepository(IProvisioningAgent agent, URI location, String name, Map<String, String> properties) {
		super(agent, name == null ? (location != null ? location.toString() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties); //$NON-NLS-1$
		if (!location.getScheme().equals("file")) //$NON-NLS-1$
			throw new IllegalArgumentException("Invalid local repository location: " + location); //$NON-NLS-1$
		//when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
		save();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository#addInstallableUnits(java.util.Collection)
	 */
	@Override
	public synchronized void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		if (installableUnits == null || installableUnits.isEmpty())
			return;
		if (snapshotNeeded) {
			units = units.clone();
			idIndex = null; // Backed by units
			snapshotNeeded = false;
		}
		units.addAll(installableUnits);
		capabilityIndex = null; // Generated, not backed by units
		save();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository#addReferences(java.util.Collection)
	 */
	@Override
	public void addReferences(Collection<? extends IRepositoryReference> references) {
		assertModifiable();
		// only write out the repository if we made changes
		if (repositories.addAll(references))
			save();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#getReferences()
	 */
	public Collection<IRepositoryReference> getReferences() {
		return Collections.unmodifiableCollection(repositories);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.metadata.index.IIndexProvider#getIndex(java.lang.String)
	 */
	public synchronized IIndex<IInstallableUnit> getIndex(String memberName) {
		if (InstallableUnit.MEMBER_ID.equals(memberName)) {
			snapshotNeeded = true;
			if (idIndex == null)
				idIndex = new IdIndex(units);
			return idIndex;
		}

		if (InstallableUnit.MEMBER_PROVIDED_CAPABILITIES.equals(memberName)) {
			snapshotNeeded = true;
			if (capabilityIndex == null)
				capabilityIndex = new CapabilityIndex(units.iterator());
			return capabilityIndex;
		}
		return null;
	}

	public synchronized Object getManagedProperty(Object client, String memberName, Object key) {
		if (!(client instanceof IInstallableUnit))
			return null;
		IInstallableUnit iu = (IInstallableUnit) client;
		if (InstallableUnit.MEMBER_TRANSLATED_PROPERTIES.equals(memberName)) {
			if (translationSupport == null)
				translationSupport = new TranslationSupport(this);
			return key instanceof KeyWithLocale ? translationSupport.getIUProperty(iu, (KeyWithLocale) key) : translationSupport.getIUProperty(iu, key.toString());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository#initialize(org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository.RepositoryState)
	 */
	@Override
	public void initialize(RepositoryState state) {
		synchronized (this) {
			setName(state.Name);
			setType(state.Type);
			setVersion(state.Version.toString());
			setProvider(state.Provider);
			setDescription(state.Description);
			setLocation(state.Location);
			setProperties(state.Properties);
			this.units.addAll(state.Units);
			this.repositories.addAll(Arrays.asList(state.Repositories));
		}
		publishRepositoryReferences();
	}

	/**
	 * Broadcast discovery events for all repositories referenced by this repository.
	 */
	public void publishRepositoryReferences() {
		IProvisioningEventBus bus = (IProvisioningEventBus) getProvisioningAgent().getService(IProvisioningEventBus.SERVICE_NAME);
		if (bus == null)
			return;

		List<IRepositoryReference> repositoriesSnapshot = createRepositoriesSnapshot();
		for (IRepositoryReference reference : repositoriesSnapshot) {
			boolean isEnabled = (reference.getOptions() & IRepository.ENABLED) != 0;
			bus.publishEvent(new RepositoryEvent(reference.getLocation(), reference.getType(), RepositoryEvent.DISCOVERED, isEnabled));
		}
	}

	private synchronized List<IRepositoryReference> createRepositoriesSnapshot() {
		if (repositories.isEmpty())
			return Collections.<IRepositoryReference> emptyList();
		return new ArrayList<IRepositoryReference>(repositories);
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URI aLocation) {
		setLocation(aLocation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.spi.AbstractRepository#isModifiable()
	 */
	@Override
	public boolean isModifiable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query.IQueryable#query(org.eclipse.equinox.p2.query.IQuery, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		return IndexProvider.query(this, query, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.metadata.index.IIndexProvider#everything()
	 */
	public synchronized Iterator<IInstallableUnit> everything() {
		snapshotNeeded = true;
		return units.iterator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository#removeAll()
	 */
	@Override
	public synchronized void removeAll() {
		if (snapshotNeeded) {
			units = new IUMap();
			idIndex = null; // Backed by units
			snapshotNeeded = false;
		} else
			units.clear();
		capabilityIndex = null; // Generated, not backed by units.
		save();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository#removeInstallableUnits(java.util.Collection)
	 */
	@Override
	public synchronized boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		boolean changed = false;
		if (installableUnits != null && !installableUnits.isEmpty()) {
			changed = true;
			if (snapshotNeeded) {
				units = units.clone();
				idIndex = null; // Backed by units
				snapshotNeeded = false;
			}
			units.removeAll(installableUnits);
			capabilityIndex = null; // Generated, not backed by units.
		}
		if (changed)
			save();
		return changed;
	}

	// caller should be synchronized
	/**
	 * Marking protected so we can test.  This is internal, so it shouldn't matter, but I'll
	 * mark it as no override just to be clear.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	protected void save() {
		if (disableSave)
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
			super.setProperty(IRepository.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()), new NullProgressMonitor());
			new MetadataRepositoryIO(getProvisioningAgent()).write(this, output);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_WRITE, "Error saving metadata repository: " + getLocation(), e)); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.spi.AbstractRepository#setProperty(java.lang.String, java.lang.String)
	 */
	@Override
	public String setProperty(String key, String newValue, IProgressMonitor monitor) {
		try {
			String oldValue = null;
			synchronized (this) {
				oldValue = super.setProperty(key, newValue, monitor);
				if (oldValue == newValue || (oldValue != null && oldValue.equals(newValue)))
					return oldValue;
				save();
			}
			//force repository manager to reload this repository because it caches properties
			MetadataRepositoryManager manager = (MetadataRepositoryManager) getProvisioningAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
			if (manager.removeRepository(getLocation()))
				manager.addRepository(this);
			return oldValue;
		} finally {
			if (monitor != null)
				monitor.done();
		}
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#compress(IPool<IInstallableUnit> iuPool)
	 */
	public void compress(IPool<IInstallableUnit> iuPool) {
		units.compress(iuPool);
	}
}
