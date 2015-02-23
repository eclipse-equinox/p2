/*******************************************************************************
 *  Copyright (c) 2008, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#addInstallableUnits(java.util.Collection)
	 */
	public void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#addReferences(java.util.Collection)
	 */
	public void addReferences(Collection<? extends IRepositoryReference> references) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#getReferences()
	 */
	public Collection<IRepositoryReference> getReferences() {
		return delegate.getReferences();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#removeAll()
	 */
	public void removeAll() {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	// TODO remove
	public boolean removeInstallableUnits(IInstallableUnit[] installableUnits, IProgressMonitor monitor) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#removeInstallableUnits(java.util.Collection)
	 */
	public boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#getDescription()
	 */
	public String getDescription() {
		return delegate.getDescription();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#getLocation()
	 */
	public URI getLocation() {
		return location;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#getName()
	 */
	public String getName() {
		return delegate.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#getProperties()
	 */
	public Map<String, String> getProperties() {
		return delegate.getProperties();
	}

	public String getProperty(String key) {
		return delegate.getProperty(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#getProvider()
	 */
	public String getProvider() {
		return delegate.getProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#getProvisioningAgent()
	 */
	public IProvisioningAgent getProvisioningAgent() {
		return delegate.getProvisioningAgent();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#getVersion()
	 */
	public String getVersion() {
		return VERSION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#isModifiable()
	 */
	public boolean isModifiable() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#setName(java.lang.String)
	 */
	public void setName(String name) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#setProperty(java.lang.String, java.lang.String)
	 */
	public String setProperty(String key, String value, IProgressMonitor monitor) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#setProperty(java.lang.String, java.lang.String)
	 */
	public String setProperty(String key, String value) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#setProvider(java.lang.String)
	 */
	public void setProvider(String provider) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	public <T> T getAdapter(Class<T> adapter) {
		return delegate.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query.IQueryable#query(org.eclipse.equinox.p2.query.IQuery, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		return delegate.query(query, monitor);
	}

	/**
	 * {@inheritDoc}
	 */
	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		return delegate.executeBatch(runnable, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#compress(IPool<IInstallableUnit> iuPool)
	 */
	public void compress(IPool<IInstallableUnit> iuPool) {
		delegate.compress(iuPool);
	}
}