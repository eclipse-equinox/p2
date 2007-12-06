/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.Query;
import org.eclipse.equinox.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.osgi.util.NLS;

/**
 * A metadata repository that resides in the local file system.  If the repository
 * location is a directory, this implementation will traverse the directory structure
 * and combine any metadata repository files that are found.
 */
public class LocalMetadataRepository extends AbstractMetadataRepository {
	static final private String CONTENT_FILENAME = "content.xml"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = LocalMetadataRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);

	protected HashSet units = new LinkedHashSet();

	public static File getActualLocation(URL location) {
		String spec = location.getFile();
		if (spec.endsWith(CONTENT_FILENAME))
			return new File(spec);
		if (spec.endsWith("/")) //$NON-NLS-1$
			spec += CONTENT_FILENAME;
		else
			spec += "/" + CONTENT_FILENAME; //$NON-NLS-1$
		return new File(spec);
	}

	public LocalMetadataRepository() {
		super();
	}

	protected LocalMetadataRepository(String name, String type, String version, URL location, String description, String provider) {
		super(name, type, version, location, description, provider);
	}

	public LocalMetadataRepository(URL location, String name) {
		super(name == null ? (location != null ? location.toExternalForm() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null); //$NON-NLS-1$
		if (!location.getProtocol().equals("file")) //$NON-NLS-1$
			throw new IllegalArgumentException("Invalid local repository location: " + location);
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnits) {
		units.addAll(Arrays.asList(installableUnits));
		save();
	}

	// Get a non-modifiable collection of the installable units
	// from the repository.
	public Set getInstallableUnits() {
		return Collections.unmodifiableSet(units);
	}

	public IInstallableUnit[] getInstallableUnits(IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(NLS.bind(Messages.REPO_LOADING, location.toExternalForm()), 5);
		IInstallableUnit[] result = (IInstallableUnit[]) units.toArray(new IInstallableUnit[0]);
		monitor.done();
		return result;
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
		int sizeBefore = units.size();
		for (Iterator it = units.iterator(); it.hasNext();)
			if (query.isMatch(it.next()))
				it.remove();
		save();
		return units.size() != sizeBefore;
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
		try {
			if (!file.exists()) {
				if (!file.getParentFile().exists())
					file.getParentFile().mkdirs();
				file.createNewFile();
			}
			new MetadataRepositoryIO().write(this, new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
