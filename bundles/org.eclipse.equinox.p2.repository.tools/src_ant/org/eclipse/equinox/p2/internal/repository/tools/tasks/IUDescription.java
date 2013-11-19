/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.*;
import org.apache.tools.ant.types.DataType;
import org.eclipse.equinox.p2.internal.repository.tools.Activator;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @since 1.0
 */
public class IUDescription extends DataType {
	static private final String QUERY_PROPERTY = "property"; //$NON-NLS-1$
	static private final String QUERY_NAME = "name"; //$NON-NLS-1$
	static private final String QUERY_VALUE = "value"; //$NON-NLS-1$
	static private final String ANT_PREFIX = "${"; //$NON-NLS-1$
	private String id;
	private String version;
	private String queryString;
	private boolean required = true;
	private String artifactFilter = null;

	public IUDescription() {
		super();
	}

	public void setId(String value) {
		if (value != null && !value.startsWith(ANT_PREFIX))
			this.id = value;
	}

	public void setVersion(String value) {
		if (value != null && !value.startsWith(ANT_PREFIX))
			this.version = value;
	}

	public void setQuery(String query) {
		if (query != null && !query.startsWith(ANT_PREFIX))
			this.queryString = query;
	}

	public void setArtifacts(String filter) {
		if (filter != null && !filter.startsWith(ANT_PREFIX))
			this.artifactFilter = filter;
	}

	public Filter getArtifactFilter() throws InvalidSyntaxException {
		if (artifactFilter != null)
			return Activator.getBundleContext().createFilter(artifactFilter);
		return null;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isRequired() {
		return required;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer("Installable Unit ["); //$NON-NLS-1$
		if (id != null) {
			buffer.append(" id="); //$NON-NLS-1$
			buffer.append(id);
		}
		if (version != null) {
			buffer.append(" version="); //$NON-NLS-1$
			buffer.append(version);
		}
		if (queryString != null) {
			buffer.append(" query="); //$NON-NLS-1$
			buffer.append(queryString);
		}
		buffer.append(" ]"); //$NON-NLS-1$
		return buffer.toString();
	}

	public IQuery<IInstallableUnit> createQuery() {
		List<IQuery<IInstallableUnit>> queries = new ArrayList<IQuery<IInstallableUnit>>();
		if (id != null) {
			if (version == null || version.length() == 0) {
				// Get the latest version of the iu
				queries.add(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id)));
			} else {
				Version iuVersion = Version.parseVersion(version);
				queries.add(QueryUtil.createIUQuery(id, iuVersion));
			}
		}

		IQuery<IInstallableUnit> iuQuery = processQueryString();
		if (iuQuery != null)
			queries.add(iuQuery);

		if (queries.size() == 1)
			return queries.get(0);

		IQuery<IInstallableUnit> query = QueryUtil.createPipeQuery(queries);
		return query;
	}

	private IQuery<IInstallableUnit> processQueryString() {
		if (queryString == null)
			return null;
		int startIdx = queryString.indexOf('[');
		int endIdx = queryString.lastIndexOf(']');
		if (startIdx == -1 || endIdx == -1 || endIdx < startIdx)
			return null;
		String element = queryString.substring(0, startIdx);
		Map<String, String> attributes = processQueryAttributes(queryString.substring(startIdx + 1, endIdx));
		if (element.equals(QUERY_PROPERTY)) {
			String name = attributes.get(QUERY_NAME);
			String value = attributes.get(QUERY_VALUE);
			if (name == null)
				return null;
			if (value == null)
				value = QueryUtil.ANY;
			return QueryUtil.createIUPropertyQuery(name, value);
		}

		return null;
	}

	private Map<String, String> processQueryAttributes(String attributes) {
		if (attributes == null || attributes.length() == 0)
			return Collections.<String, String> emptyMap();

		Map<String, String> result = new HashMap<String, String>();
		int start = 0;
		int idx = 0;
		while ((idx = attributes.indexOf('@', start)) > -1) {
			int equals = attributes.indexOf('=', idx);
			int startQuote = attributes.indexOf('\'', equals);
			int endQuote = attributes.indexOf('\'', startQuote + 1);
			if (equals == -1 || startQuote <= equals || endQuote <= startQuote)
				break;
			String key = attributes.substring(idx + 1, equals).trim();
			String value = attributes.substring(startQuote + 1, endQuote);
			result.put(key, value);

			start = endQuote + 1;
		}
		return result;
	}
}
