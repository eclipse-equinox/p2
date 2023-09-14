/*******************************************************************************
 *  Copyright (c) 2008, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Fix compiler problems from generified IAdaptable#getAdapter
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IPool;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
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

	// TODO remove
	public void addInstallableUnits(IInstallableUnit[] installableUnits) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@Override
	public void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@Override
	public void addReferences(Collection<? extends IRepositoryReference> references) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@Override
	public boolean removeReferences(Collection<? extends IRepositoryReference> references) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@Override
	public Collection<IRepositoryReference> getReferences() {
		return delegate.getReferences();
	}

	@Override
	public void removeAll() {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	// TODO remove
	public boolean removeInstallableUnits(IInstallableUnit[] installableUnits, IProgressMonitor monitor) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@Override
	public boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public URI getLocation() {
		return location;
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public Map<String, String> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public String getProperty(String key) {
		return delegate.getProperty(key);
	}

	@Override
	public String getProvider() {
		return delegate.getProvider();
	}

	@Override
	public IProvisioningAgent getProvisioningAgent() {
		return delegate.getProvisioningAgent();
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public boolean isModifiable() {
		return false;
	}

	public void setDescription(String description) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public void setName(String name) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@Override
	public String setProperty(String key, String value, IProgressMonitor monitor) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@Override
	public String setProperty(String key, String value) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public void setProvider(String provider) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return delegate.getAdapter(adapter);
	}

	@Override
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		return delegate.query(query, monitor);
	}

	@Override
	public boolean contains(IInstallableUnit element) {
		return delegate.contains(element);
	}

	@Override
	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		return delegate.executeBatch(runnable, monitor);
	}

	@Override
	public void compress(IPool<IInstallableUnit> iuPool) {
		delegate.compress(iuPool);
	}
}