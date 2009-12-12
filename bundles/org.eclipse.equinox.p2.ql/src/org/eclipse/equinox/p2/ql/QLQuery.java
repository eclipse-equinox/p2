/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ql;

import java.util.Locale;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.QueryHelpers;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * An IQuery 'context query' implementation that is based on the p2 query language.
 */
public abstract class QLQuery implements IQuery {
	static final IExpressionParser parser = QL.newParser();
	static final Object[] noParameters = new Object[0];

	final Class elementClass;
	final Object[] parameters;
	private Locale locale;

	protected QLQuery(Class elementClass, Object[] parameters) {
		this.elementClass = elementClass;
		this.parameters = parameters;
	}

	public Object getProperty(String key) {
		if (IArtifactRepository.QUERY_EXCLUDE_KEYS.equals(key))
			return Boolean.valueOf(!elementClass.isAssignableFrom(IArtifactKey.class));

		if (IArtifactRepository.QUERY_EXCLUDE_DESCRIPTORS.equals(key))
			return Boolean.valueOf(!elementClass.isAssignableFrom(IArtifactDescriptor.class));

		return QueryHelpers.getProperty(this, key);
	}

	public Locale getLocale() {
		return locale == null ? Locale.getDefault() : locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * Gets the ID for this Query. 
	 */
	public String getId() {
		return QueryHelpers.getId(this);
	}
}
