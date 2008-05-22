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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;

/**
 * A metadata repository backed by an arbitrary URL.
 */
public class URLMetadataRepository extends AbstractMetadataRepository {

	public static final String CONTENT_FILENAME = "content"; //$NON-NLS-1$
	public static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String REPOSITORY_TYPE = URLMetadataRepository.class.getName();
	private static final Integer REPOSITORY_VERSION = new Integer(1);

	transient protected URL content;
	protected HashSet units = new LinkedHashSet();

	public static URL getActualLocation(URL base) {
		return getActualLocation(base, XML_EXTENSION);
	}

	public static URL getActualLocation(URL base, String extension) {
		if (extension == null) {
			extension = XML_EXTENSION;
		}
		String spec = base.toExternalForm();
		if (spec.endsWith(CONTENT_FILENAME + extension))
			try {
				return new URL(spec + extension);
			} catch (MalformedURLException e1) {
				return null;
			}
		if (spec.endsWith("/")) //$NON-NLS-1$
			spec += CONTENT_FILENAME;
		else
			spec += "/" + CONTENT_FILENAME; //$NON-NLS-1$
		try {
			return new URL(spec + extension);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public URLMetadataRepository() {
		super();
	}

	public URLMetadataRepository(URL location, String name, Map properties) {
		super(name == null ? (location != null ? location.toExternalForm() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties); //$NON-NLS-1$
		content = getActualLocation(location);
	}

	// this is synchronized because content can be initialized in initializeAfterLoad
	protected synchronized URL getContentURL() {
		return content;
	}

	public synchronized void initialize(RepositoryState state) {
		this.name = state.Name;
		this.type = state.Type;
		this.version = state.Version.toString();
		this.provider = state.Provider;
		this.description = state.Description;
		this.location = state.Location;
		this.properties = state.Properties;
		this.units.addAll(Arrays.asList(state.Units));
	}

	// Use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URL repoLocation) {
		this.location = repoLocation;
		content = getActualLocation(location);
	}

	public boolean isModifiable() {
		return false;
	}

	public synchronized Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return query.perform(units.iterator(), collector);
	}
}
