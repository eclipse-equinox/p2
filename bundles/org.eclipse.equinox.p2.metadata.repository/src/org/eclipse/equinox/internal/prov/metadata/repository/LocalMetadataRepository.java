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
package org.eclipse.equinox.internal.prov.metadata.repository;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.prov.core.helpers.OrderedProperties;
import org.eclipse.equinox.prov.core.repository.IWritableRepositoryInfo;
import org.eclipse.equinox.prov.core.repository.RepositoryCreationException;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.RequiredCapability;
import org.eclipse.equinox.prov.metadata.repository.IWritableMetadataRepository;
import org.eclipse.equinox.prov.query.CompoundIterator;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;

/**
 * A metadata repository that resides in the local file system.  If the repository
 * location is a directory, this implementation will traverse the directory structure
 * and combine any metadata repository files that are found.
 */
public class LocalMetadataRepository extends AbstractMetadataRepository implements IWritableMetadataRepository {

	static final private String REPOSITORY_TYPE = LocalMetadataRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final private String CONTENT_FILENAME = "content.xml"; //$NON-NLS-1$

	transient private URL location;

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

	public LocalMetadataRepository(URL location, String name) throws RepositoryCreationException {
		super(name == null ? (location != null ? location.toExternalForm() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString());
		if (!location.getProtocol().equals("file")) //$NON-NLS-1$
			throw new IllegalArgumentException("Invalid local repository location: " + location);
		this.location = location;
	}

	public IInstallableUnit[] getInstallableUnits(IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(NLS.bind(Messages.REPO_LOADING, location.toExternalForm()), 5);
		IInstallableUnit[] result = query(null, null, null, false, monitor);
		monitor.done();
		return result;
	}

	public Iterator getIterator(String id, VersionRange range, RequiredCapability[] requirements, boolean and) {
		return new CompoundIterator(new Iterator[] {units.iterator()}, id, range, requirements, and);
	}

	public URL getLocation() {
		return location;
	}

	public IInstallableUnit[] query(String id, VersionRange range, RequiredCapability[] requirements, boolean and, IProgressMonitor monitor) {
		return CompoundIterator.asArray(new CompoundIterator(new Iterator[] {units.iterator()}, id, range, requirements, and), null);
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnits) {
		units.addAll(Arrays.asList(installableUnits));
		save();
	}

	private void save() {
		File file = getActualLocation(location);
		try {
			if (!file.exists()) {
				if (!file.getParentFile().exists())
					file.getParentFile().mkdirs();
				file.createNewFile();
			}
			MetadataRepositoryIO.write(this, new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void removeInstallableUnits(IInstallableUnit[] installableUnits) {
		units.remove(Arrays.asList(installableUnits));
		save();
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("The type of a local metadata repository cannot be changed."); //$NON-NLS-1$
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public OrderedProperties getModifiableProperties() {
		return properties;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == LocalMetadataRepository.class || adapter == IWritableMetadataRepository.class || adapter == IWritableRepositoryInfo.class)
			return this;
		return super.getAdapter(adapter);
	}

	public void removeAll() {
		units.clear();
		save();
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public void initializeAfterLoad(URL location) {
		this.location = location;
	}
}
