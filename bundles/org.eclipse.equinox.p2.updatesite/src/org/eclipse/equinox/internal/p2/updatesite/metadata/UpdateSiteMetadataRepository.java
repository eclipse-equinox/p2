/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
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
	 * @see org.eclipse.equinox.p2.repository.metadata.IMetadataRepository#addReference(java.net.URI, java.lang.String, int, int)
	 */
	public void addReference(URI location, String nickname, int type, int options) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
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
	public String setProperty(String key, String value) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.IRepository#setProvider(java.lang.String)
	 */
	public void setProvider(String provider) {
		throw new UnsupportedOperationException("Repository not modifiable: " + location); //$NON-NLS-1$
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return delegate.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query.IQueryable#query(org.eclipse.equinox.p2.query.IQuery, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		return delegate.query(query, monitor);
	}
}