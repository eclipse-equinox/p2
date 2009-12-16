/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import org.eclipse.equinox.p2.metadata.query.IQueryResult;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

public class UpdateSiteMetadataRepository implements IMetadataRepository {

	public static final String TYPE = "org.eclipse.equinox.p2.updatesite.metadataRepository"; //$NON-NLS-1$
	public static final String VERSION = Integer.toString(1);

	private URI location;
	private IMetadataRepository delegate;

	public UpdateSiteMetadataRepository(URI location, IMetadataRepository repository) {
		this.location = location;
		this.delegate = repository;
	}

	public void addInstallableUnits(IInstallableUnit[] installableUnits) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public void addReference(URI location, String nickname, int type, int options) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public void removeAll() {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public boolean removeInstallableUnits(IInstallableUnit[] installableUnits, IProgressMonitor monitor) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public String getDescription() {
		return delegate.getDescription();
	}

	public URI getLocation() {
		return location;
	}

	public String getName() {
		return delegate.getName();
	}

	public Map getProperties() {
		return delegate.getProperties();
	}

	public String getProvider() {
		return delegate.getProvider();
	}

	public String getType() {
		return TYPE;
	}

	public String getVersion() {
		return VERSION;
	}

	public boolean isModifiable() {
		return false;
	}

	public void setDescription(String description) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public void setName(String name) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public String setProperty(String key, String value) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public void setProvider(String provider) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public Object getAdapter(Class adapter) {
		return delegate.getAdapter(adapter);
	}

	public IQueryResult query(IQuery query, IProgressMonitor monitor) {
		return delegate.query(query, monitor);
	}
}