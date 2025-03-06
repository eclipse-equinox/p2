/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *		compeople AG (Stefan Liebig) - various ongoing maintenance
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.URIUtil;
import org.osgi.framework.*;

public class Mapper {
	private Filter[] filters;
	private String[] outputStrings;

	private static final String REPOURL = "repoUrl"; //$NON-NLS-1$
	private static final String CLASSIFIER = "classifier"; //$NON-NLS-1$
	private static final String FORMAT = "format"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String VERSION = "version"; //$NON-NLS-1$

	public Mapper() {
		filters = new Filter[0];
		outputStrings = new String[0];
	}

	/**
	 * mapping rule: LDAP filter --> output value
	 * the more specific filters should be given first.
	 */
	public void initialize(BundleContext ctx, String[][] mappingRules) {
		filters = new Filter[mappingRules.length];
		outputStrings = new String[mappingRules.length];
		for (int i = 0; i < mappingRules.length; i++) {
			try {
				filters[i] = ctx.createFilter(mappingRules[i][0]);
				outputStrings[i] = mappingRules[i][1];
			} catch (InvalidSyntaxException e) {
				//TODO Neeed to process this
				e.printStackTrace();
			}
		}
	}

	public URI map(URI repositoryLocation, String classifier, String id, String version, String format,
			Map<String, String> properties) {
		String locationString = URIUtil.toUnencodedString(repositoryLocation);
		Dictionary<String, String> allProperties = new Hashtable<>(properties.size() + 4);
		if (repositoryLocation != null) {
			allProperties.put(REPOURL, locationString);
		}
		if (classifier != null) {
			allProperties.put(CLASSIFIER, classifier);
		}
		if (id != null) {
			allProperties.put(ID, id);
		}
		if (version != null) {
			allProperties.put(VERSION, version);
		}
		if (format != null) {
			allProperties.put(FORMAT, format);
		}

		for (int i = 0; i < filters.length; i++) {
			if (filters[i].match(allProperties)) {
				return doReplacement(outputStrings[i], locationString, classifier, id, version, format, properties);
			}
		}
		return null;
	}

	private URI doReplacement(String pattern, String repoLocation, String classifier, String id, String version,
			String format, Map<String, String> properties) {
		try {
			// currently our mapping rules assume the repo URL is not "/" terminated.
			// This may be the case for repoURLs in the root of a URL space e.g. root of a jar file or file:/c:/
			if (repoLocation.endsWith("/")) { //$NON-NLS-1$
				repoLocation = repoLocation.substring(0, repoLocation.length() - 1);
			}

			StringBuilder output = new StringBuilder(pattern);
			int index = 0;
			while (index < output.length()) {
				int beginning = output.indexOf("${", index); //$NON-NLS-1$
				if (beginning == -1) {
					return URIUtil.fromString(output.toString());
				}

				int end = output.indexOf("}", beginning); //$NON-NLS-1$
				if (end == -1) {
					return URIUtil.fromString(pattern);
				}

				String varName = output.substring(beginning + 2, end);
				String varValue = null;
				if (varName.equalsIgnoreCase(CLASSIFIER)) {
					varValue = classifier;
				} else if (varName.equalsIgnoreCase(ID)) {
					varValue = id;
				} else if (varName.equalsIgnoreCase(VERSION)) {
					varValue = version;
				} else if (varName.equalsIgnoreCase(REPOURL)) {
					varValue = repoLocation;
				} else if (varName.equalsIgnoreCase(FORMAT)) {
					varValue = format;
				} else if (properties.containsKey(varName)) {
					varValue = properties.get(varName);
				}
				if (varValue == null) {
					varValue = ""; //$NON-NLS-1$
				}

				output.replace(beginning, end + 1, varValue);
				index = beginning + varValue.length();
			}
			return URIUtil.fromString(output.toString());
		} catch (URISyntaxException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < filters.length; i++) {
			result.append(filters[i]).append('-').append('>').append(outputStrings[i]).append('\n');
		}
		return result.toString();
	}

	public String[][] serialize() {
		String[][] result = new String[filters.length][2];
		for (int i = 0; i < filters.length; i++) {
			result[i][0] = filters[i].toString();
			result[i][1] = outputStrings[i];
		}
		return result;
	}
}
