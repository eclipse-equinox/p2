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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.query.CompoundIterator;
import org.eclipse.equinox.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;

/**
 * A metadata repository backed by an arbitrary URL.
 */
public class URLMetadataRepository extends AbstractMetadataRepository implements IMetadataRepository {

	static final private String REPOSITORY_TYPE = URLMetadataRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final protected String CONTENT_FILENAME = "content.xml"; //$NON-NLS-1$

	transient protected URL content;
	protected HashSet units = new HashSet();

	public static URL getActualLocation(URL base) {
		String spec = base.toExternalForm();
		if (spec.endsWith(CONTENT_FILENAME))
			return base;
		if (spec.endsWith("/")) //$NON-NLS-1$
			spec += CONTENT_FILENAME;
		else
			spec += "/" + CONTENT_FILENAME; //$NON-NLS-1$
		try {
			return new URL(spec);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	protected URLMetadataRepository(String name, String type, String version, URL location, String description, String provider) {
		super(name, type, version, location, description, provider);
	}

	public URLMetadataRepository(URL location, String name) {
		super(name == null ? (location != null ? location.toExternalForm() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null);
		content = getActualLocation(location);
	}

	protected boolean load() throws RepositoryCreationException {
		return new SimpleMetadataRepositoryFactory().load(location) != null;
	}

	public IInstallableUnit[] getInstallableUnits(IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(NLS.bind(Messages.REPO_LOADING, location.toExternalForm()), 5);
		IInstallableUnit[] result = (IInstallableUnit[]) units.toArray(new IInstallableUnit[units.size()]);
		monitor.done();
		return result;
	}

	protected URL getContentURL() {
		return content;
	}

	public Iterator getIterator(String id, VersionRange range, RequiredCapability[] requirements, boolean and) {
		return new CompoundIterator(new Iterator[] {units.iterator()}, id, range, requirements, and);
	}

	public IInstallableUnit[] query(String id, VersionRange range, RequiredCapability[] requirements, boolean and, IProgressMonitor progress) {
		return CompoundIterator.asArray(new CompoundIterator(new Iterator[] {units.iterator()}, id, range, requirements, and), null);
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public void initializeAfterLoad(URL location) {
		this.location = location;
	}

	public void initializeAfterLoad(URLMetadataRepository source) {
		name = source.name;
		type = source.type;
		version = source.version;
		location = source.location;
		description = source.description;
		provider = source.provider;
		properties = source.properties;
		units = source.units;
	}

	public boolean isModifiable() {
		return false;
	}
}
