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
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.*;

public class Mapper {
	private Filter[] filters;
	private String[] outputStrings;

	private static final String REPOURL = "repoUrl";
	private static final String NAMESPACE = "namespace";
	private static final String CLASSIFIER = "classifier";
	private static final String FORMAT = "format";
	private static final String ID = "id";
	private static final String VERSION = "version";

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

	public String map(String repoUrl, String namespace, String classifier, String id, String version, String format) {
		Dictionary values = new Hashtable(5);
		if (repoUrl != null)
			values.put(REPOURL, repoUrl);

		if (namespace != null)
			values.put(NAMESPACE, namespace);

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
				return doReplacement(outputStrings[i], repoUrl, namespace, classifier, id, version, format);
		}
		return null;
	}

	private String doReplacement(String pattern, String repoUrl, String namespace, String classifier, String id, String version, String format) {
		// currently our mapping rules assume the repo URL is not "/" terminated. 
		// This may be the case for repoURLs in the root of a URL space e.g. root of a jar file or file:/c:/
		if (repoUrl.endsWith("/"))
			repoUrl = repoUrl.substring(0, repoUrl.length() - 1);

		StringBuffer output = new StringBuffer(pattern);
		int index = 0;
		while (index < output.length()) {
			int beginning = output.indexOf("${", index);
			if (beginning == -1)
				return output.toString();

			int end = output.indexOf("}", beginning);
			if (end == -1)
				return pattern;

			String varName = output.substring(beginning + 2, end);
			String varValue = null;
			if (varName.equalsIgnoreCase(NAMESPACE)) {
				varValue = namespace;
			} else if (varName.equalsIgnoreCase(CLASSIFIER)) {
				varValue = classifier;
			} else if (varName.equalsIgnoreCase(ID)) {
				varValue = id;
			} else if (varName.equalsIgnoreCase(VERSION)) {
				varValue = version;
			} else if (varName.equalsIgnoreCase(REPOURL)) {
				varValue = repoUrl;
			} else if (varName.equalsIgnoreCase(FORMAT)) {
				varValue = format;
			}
			if (varValue == null)
				varValue = "";

			output.replace(beginning, end + 1, varValue);
			index = beginning + varValue.length();
		}
		return output.toString();
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
