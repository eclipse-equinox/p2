/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *		compeople AG (Stefan Liebig) - various ongoing maintenance
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;
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

	public URI map(URI repositoryLocation, String classifier, String id, String version, String format) {
		String locationString = URIUtil.toUnencodedString(repositoryLocation);
		Dictionary<String, Object> values = new Hashtable<String, Object>(5);
		if (repositoryLocation != null)
			values.put(REPOURL, locationString);

		if (classifier != null)
			values.put(CLASSIFIER, classifier);

		if (id != null)
			values.put(ID, id);

		if (version != null)
			values.put(VERSION, version);

		if (format != null)
			values.put(FORMAT, format);

		for (int i = 0; i < filters.length; i++) {
			if (filters[i].match(values))
				return doReplacement(outputStrings[i], locationString, classifier, id, version, format);
		}
		return null;
	}

	private URI doReplacement(String pattern, String repoLocation, String classifier, String id, String version, String format) {
		try {
			// currently our mapping rules assume the repo URL is not "/" terminated. 
			// This may be the case for repoURLs in the root of a URL space e.g. root of a jar file or file:/c:/
			if (repoLocation.endsWith("/")) //$NON-NLS-1$
				repoLocation = repoLocation.substring(0, repoLocation.length() - 1);

			StringBuffer output = new StringBuffer(pattern);
			int index = 0;
			while (index < output.length()) {
				int beginning = output.indexOf("${", index); //$NON-NLS-1$
				if (beginning == -1)
					return URIUtil.fromString(output.toString());

				int end = output.indexOf("}", beginning); //$NON-NLS-1$
				if (end == -1)
					return URIUtil.fromString(pattern);

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
				}
				if (varValue == null)
					varValue = ""; //$NON-NLS-1$

				output.replace(beginning, end + 1, varValue);
				index = beginning + varValue.length();
			}
			return URIUtil.fromString(output.toString());
		} catch (URISyntaxException e) {
			return null;
		}
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
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
