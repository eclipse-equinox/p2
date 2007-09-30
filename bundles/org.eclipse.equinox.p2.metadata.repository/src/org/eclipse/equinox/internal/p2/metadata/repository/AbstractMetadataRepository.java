/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.util.HashSet;
import org.eclipse.equinox.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.core.helpers.UnmodifiableProperties;
import org.eclipse.equinox.p2.core.repository.IRepositoryInfo;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;

public abstract class AbstractMetadataRepository implements IMetadataRepository, IRepositoryInfo {

	protected String name;
	protected String type;
	protected String version;
	protected String description;
	protected String provider;
	protected OrderedProperties properties = new OrderedProperties();
	protected HashSet units = new HashSet();

	protected AbstractMetadataRepository(String name, String type, String version) {
		super();
		this.name = name;
		this.type = type;
		this.version = version;
		this.description = ""; //$NON-NLS-1$
		this.provider = ""; //$NON-NLS-1$
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getVersion() {
		return version;
	}

	public String getDescription() {
		return description;
	}

	public String getProvider() {
		return provider;
	}

	public UnmodifiableProperties getProperties() {
		return new UnmodifiableProperties(properties);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == AbstractMetadataRepository.class || adapter == IMetadataRepository.class || adapter == IRepositoryInfo.class)
			return this;
		else
			return null;
	}

}
