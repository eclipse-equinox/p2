/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Prashant Deva - Bug 194674 [prov] Provide write access to metadata repository
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.RepositoryReference;

/**
 * A metadata repository that resides in the local file system.  If the repository
 * location is a directory, this implementation will traverse the directory structure
 * and combine any metadata repository files that are found.
 */
public class LocalMetadataRepository extends AbstractMetadataRepository {

	static final private String CONTENT_FILENAME = "content"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = LocalMetadataRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final private String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	static final private String XML_EXTENSION = ".xml"; //$NON-NLS-1$

	protected HashSet units = new LinkedHashSet();
	protected HashSet repositories = new HashSet();

	private static File getActualLocation(URL location, String extension) {
		String spec = location.getFile();
		if (spec.endsWith(CONTENT_FILENAME + extension))
			return new File(spec + extension);
		if (spec.endsWith("/")) //$NON-NLS-1$
			spec += CONTENT_FILENAME;
		else
			spec += "/" + CONTENT_FILENAME; //$NON-NLS-1$
		return new File(spec + extension);
	}

	public static File getActualLocation(URL location) {
		return getActualLocation(location, XML_EXTENSION);
	}

	/**
	 * This no argument constructor is called when restoring an existing repository.
	 */
	public LocalMetadataRepository() {
		super();
	}

	/**
	 * This constructor is used when creating a new local repository.
	 * @param location The location of the repository
	 * @param name The name of the repository
	 */
	public LocalMetadataRepository(URL location, String name, Map properties) {
		super(name == null ? (location != null ? location.toExternalForm() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties); //$NON-NLS-1$
		if (!location.getProtocol().equals("file")) //$NON-NLS-1$
			throw new IllegalArgumentException("Invalid local repository location: " + location); //$NON-NLS-1$
		//when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
		save();
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnits) {
		if (installableUnits == null || installableUnits.length == 0)
			return;
		units.addAll(Arrays.asList(installableUnits));
		save();
	}

	public void addReference(URL repositoryLocation, int repositoryType, int options) {
		assertModifiable();
		repositories.add(new RepositoryReference(repositoryLocation, repositoryType, options));
	}

	public void initialize(RepositoryState state) {
		this.name = state.Name;
		this.type = state.Type;
		this.version = state.Version.toString();
		this.provider = state.Provider;
		this.description = state.Description;
		this.location = state.Location;
		this.properties = state.Properties;
		this.units.addAll(Arrays.asList(state.Units));
		this.repositories.addAll(Arrays.asList(state.Repositories));
		publishRepositoryReferences();
	}

	/**
	 * Broadcast discovery events for all repositories referenced by this repository.
	 */
	private void publishRepositoryReferences() {
		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(Activator.getContext(), IProvisioningEventBus.SERVICE_NAME);
		if (bus == null)
			return;
		for (Iterator it = repositories.iterator(); it.hasNext();) {
			RepositoryReference reference = (RepositoryReference) it.next();
			boolean isEnabled = (reference.Options & IRepository.ENABLED) != 0;
			bus.publishEvent(new RepositoryEvent(reference.Location, reference.Type, RepositoryEvent.DISCOVERED, isEnabled));
		}
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public void initializeAfterLoad(URL aLocation) {
		this.location = aLocation;
	}

	public boolean isModifiable() {
		return true;
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return query.perform(units.iterator(), collector);
	}

	public void removeAll() {
		units.clear();
		save();
	}

	public boolean removeInstallableUnits(Query query, IProgressMonitor monitor) {
		boolean changed = false;
		for (Iterator it = units.iterator(); it.hasNext();)
			if (query.isMatch(it.next())) {
				it.remove();
				changed = true;
			}
		if (changed)
			save();
		return changed;
	}

	public void revertToBackup(LocalMetadataRepository backup) {
		name = backup.name;
		type = backup.type;
		version = backup.version;
		location = backup.location;
		description = backup.description;
		provider = backup.provider;
		properties = backup.properties;
		units = backup.units;
	}

	private void save() {
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
			new MetadataRepositoryIO().write(this, output);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_WRITE, "Error saving metadata repository: " + location, e)); //$NON-NLS-1$
		}
	}

	public String setProperty(String key, String newValue) {
		String oldValue = super.setProperty(key, newValue);
		if (oldValue == newValue || (oldValue != null && oldValue.equals(newValue)))
			return oldValue;
		save();
		//force repository manager to reload this repository because it caches properties
		MetadataRepositoryManager manager = (MetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager.removeRepository(getLocation()))
			manager.addRepository(this);
		return oldValue;
	}
}
