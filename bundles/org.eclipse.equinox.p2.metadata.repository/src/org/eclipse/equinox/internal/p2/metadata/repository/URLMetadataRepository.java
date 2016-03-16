/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Prashant Deva - Bug 194674 [prov] Provide write access to metadata repository
 *     Cloudsmith Inc. - query indexes
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.internal.p2.metadata.index.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.KeyWithLocale;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;

/**
 * A metadata repository backed by an arbitrary URL.
 */
public class URLMetadataRepository extends AbstractMetadataRepository implements IIndexProvider<IInstallableUnit> {

	public static final String CONTENT_FILENAME = "content"; //$NON-NLS-1$
	protected Collection<IRepositoryReference> references;
	public static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String REPOSITORY_TYPE = URLMetadataRepository.class.getName();
	private static final Integer REPOSITORY_VERSION = 1;

	transient protected URI content;
	protected IUMap units = new IUMap();
	private IIndex<IInstallableUnit> idIndex;
	private IIndex<IInstallableUnit> capabilityIndex;
	private TranslationSupport translationSupport;

	public static URI getActualLocation(URI base) {
		return getActualLocation(base, XML_EXTENSION);
	}

	public static URI getActualLocation(URI base, String extension) {
		if (extension == null)
			extension = XML_EXTENSION;
		return URIUtil.append(base, CONTENT_FILENAME + extension);
	}

	public URLMetadataRepository(IProvisioningAgent agent) {
		super(agent);
	}

	public URLMetadataRepository(IProvisioningAgent agent, URI location, String name, Map<String, String> properties) {
		super(agent, name == null ? (location != null ? location.toString() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties); //$NON-NLS-1$
		content = getActualLocation(location);
	}

	// this is synchronized because content can be initialized in initializeAfterLoad
	protected synchronized URI getContentURL() {
		return content;
	}

	public synchronized void initialize(RepositoryState state) {
		setName(state.Name);
		setType(state.Type);
		setVersion(state.Version.toString());
		setProvider(state.Provider);
		setDescription(state.Description);
		setLocation(state.Location);
		setProperties(state.Properties);
		this.units.addAll(state.Units);
		this.references = CollectionUtils.unmodifiableList(state.Repositories);
	}

	// Use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URI repoLocation) {
		setLocation(repoLocation);
		content = getActualLocation(repoLocation);
	}

	public Collection<IRepositoryReference> getReferences() {
		return references;
	}

	public boolean isModifiable() {
		return false;
	}

	public synchronized IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		return IndexProvider.query(this, query, monitor);
	}

	public synchronized IIndex<IInstallableUnit> getIndex(String memberName) {
		if (InstallableUnit.MEMBER_ID.equals(memberName)) {
			if (idIndex == null)
				idIndex = new IdIndex(units);
			return idIndex;
		}

		if (InstallableUnit.MEMBER_PROVIDED_CAPABILITIES.equals(memberName)) {
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

	public Iterator<IInstallableUnit> everything() {
		return units.iterator();
	}
}
