/*******************************************************************************
 *  Copyright (c) 2014, 2017 SAP AG and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import java.util.*;
import java.util.regex.Pattern;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Formats a set of {@link IInstallableUnit}s to a string specified
 * by a format string.  The format string's syntax is
 * <code>${&lt;property&gt;}</code>, where <code>&lt;property&gt;</code>
 * is one of the unit's {@link IInstallableUnit#getProperties() properties constants}.
 * Two special properties are supported:
 * <ul>
 * <li>{@link IInstallableUnit#getId() IU id}: <code>${id}</code></li>
 * <li>{@link IInstallableUnit#getVersion() IU version}: <code>${version}</code></li>
 * </ul>
 */
public class IUListFormatter {

	private static final String PREFIX = "${"; //$NON-NLS-1$
	private static final String LINE_SEP = System.lineSeparator();

	private final String formatString;
	private final Collection<String> properties;

	public IUListFormatter(String formatString) {
		this.formatString = formatString;
		this.properties = parse(formatString);
	}

	public String format(Collection<IInstallableUnit> ius) {
		StringBuilder result = new StringBuilder();
		for (IInstallableUnit iu : ius) {
			format(iu, result);
		}

		if (result.length() > 0)
			result.setLength(result.length() - LINE_SEP.length()); //remove trailing newline
		return result.toString();
	}

	private void format(IInstallableUnit iu, StringBuilder result) {
		String s = formatString;
		for (String property : properties) {
			Pattern pattern = Pattern.compile(String.format("\\$\\{%s\\}", property)); //$NON-NLS-1$
			if (null == property) {
				String value = iu.getProperty(property, "df_LT"); //$NON-NLS-1$
				if (value == null)
					value = ""; //$NON-NLS-1$ unknown property
				s = insert(value, pattern, s);
			} else switch (property) {
				case "id": //$NON-NLS-1$
					s = insert(iu.getId(), pattern, s);
					break;
				case "version": //$NON-NLS-1$
					s = insert(iu.getVersion().toString(), pattern, s);
					break;
				default:
					String value = iu.getProperty(property, "df_LT"); //$NON-NLS-1$
					if (value == null)
						value = ""; //$NON-NLS-1$ unknown property
					s = insert(value, pattern, s);
					break;
			}
		}

		result.append(s);
		result.append(LINE_SEP);
	}

	private static String insert(String replacement, Pattern template, String s) {
		return template.matcher(s).replaceAll(replacement);
	}

	/*
	 * Finds all IU properties in the format string
	 */
	private static Collection<String> parse(String string) {
		Set<String> properties = new HashSet<>(5);
		int start = 0;
		while (start < string.length() && (start = string.indexOf(PREFIX, start)) > -1) {
			int end = string.indexOf('}', start + PREFIX.length());
			if (end > start) {
				String property = string.substring(start + PREFIX.length(), end);
				properties.add(property);
				start = end + 1;
			} else {
				// malformed input, be permissive and go on
				start++;
			}
		}
		return properties;
	}

}
