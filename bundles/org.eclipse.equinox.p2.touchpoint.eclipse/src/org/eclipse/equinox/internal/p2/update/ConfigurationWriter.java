/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Activator;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;

/**
 * @since 1.0
 */
public class ConfigurationWriter implements ConfigurationConstants {

	/*
	 * Save the given configuration to the specified location.
	 */
	static void save(Configuration configuration, File location, URL osgiInstallArea) throws ProvisionException {
		try (XMLWriter writer = new XMLWriter(new BufferedOutputStream(new FileOutputStream(location)))) {
			Map<String, String> args = new HashMap<>();

			// always write out an up-to-date timestamp
			args.put(ATTRIBUTE_DATE, Long.toString(new Date().getTime()));

			String value = configuration.getSharedUR();
			if (value != null)
				args.put(ATTRIBUTE_SHARED_UR, value);

			value = configuration.getVersion();
			if (value != null)
				args.put(ATTRIBUTE_VERSION, value);

			args.put(ATTRIBUTE_TRANSIENT, Boolean.toString(configuration.isTransient()));

			writer.startTag(ELEMENT_CONFIG, args);

			for (Site site : configuration.internalGetSites(false)) {
				write(writer, site, osgiInstallArea);
			}

			writer.endTag(ELEMENT_CONFIG);
		} catch (FileNotFoundException e) {
			throw new ProvisionException(NLS.bind(Messages.error_saving_config, location), e);
		}
		// put the config in the cache in case someone in the same session wants to read it
		ConfigurationCache.put(location, configuration);
	}

	/*
	 * Write out the given site.
	 */
	private static void write(XMLWriter writer, Site site, URL osgiInstallArea) {
		Map<String, String> args = new HashMap<>();

		String value = site.getLinkFile();
		if (value != null)
			args.put(ATTRIBUTE_LINKFILE, value);

		value = site.getPolicy();
		if (value != null)
			args.put(ATTRIBUTE_POLICY, value);

		value = site.getUrl();
		if (value != null) {
			try {
				value = URIUtil.toUnencodedString(new URI(value));
			} catch (URISyntaxException e) {
				// ignore this error and just use the value straight as-is
			}
			args.put(ATTRIBUTE_URL, getLocation(value, osgiInstallArea));
		}

		value = toString(site.getList());
		if (value != null)
			args.put(ATTRIBUTE_LIST, value);

		args.put(ATTRIBUTE_UPDATEABLE, Boolean.toString(site.isUpdateable()));
		args.put(ATTRIBUTE_ENABLED, Boolean.toString(site.isEnabled()));

		writer.startTag(ELEMENT_SITE, args);
		write(writer, site.getFeatures());
		writer.endTag(ELEMENT_SITE);
	}

	/*
	 * Return the location for the given location which is a url string. Take into account 
	 * the specified osgi install area. This method should make the path relative if 
	 * possible and could potentially return platform:/base/.
	 */
	private static String getLocation(String value, URL osgiInstallArea) {
		if (osgiInstallArea == null || !value.startsWith("file:")) //$NON-NLS-1$
			return value;
		try {
			// if our site represents the osgi install area, then write out platform:/base/
			File installArea = URLUtil.toFile(osgiInstallArea);
			File path = URLUtil.toFile(new URL(value));
			if (installArea.getAbsoluteFile().equals(path.getAbsoluteFile()))
				return ConfigurationParser.PLATFORM_BASE;
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while writing configuration.", e)); //$NON-NLS-1$
		}
		return PathUtil.makeRelative(value, osgiInstallArea);
	}

	/*
	 * Convert the given list to a comma-separated string.
	 */
	private static String toString(Object[] list) {
		if (list == null || list.length == 0)
			return null;
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < list.length; i++) {
			buffer.append(list[i].toString());
			if (i + 1 < list.length)
				buffer.append(',');
		}
		return buffer.toString();
	}

	/*
	 * Write out the given list of features.
	 */
	private static void write(XMLWriter writer, Feature[] features) {
		if (features == null || features.length == 0)
			return;
		for (Feature feature : features) {
			Map<String, String> args = new HashMap<>();
			String value = feature.getId();
			if (value != null)
				args.put(ATTRIBUTE_ID, value);
			value = feature.getUrl();
			if (value != null)
				args.put(ATTRIBUTE_URL, value);
			value = feature.getVersion();
			if (value != null)
				args.put(ATTRIBUTE_VERSION, value);
			value = feature.getPluginIdentifier();
			// only write out the plug-in identifier if it is different from the feature id
			if (value != null && !value.equals(feature.getId()))
				args.put(ATTRIBUTE_PLUGIN_IDENTIFIER, value);
			value = feature.getPluginVersion();
			// only write out the plug-in version if it is different from the feature version
			if (value != null && !value.equals(feature.getVersion()))
				args.put(ATTRIBUTE_PLUGIN_VERSION, value);
			if (feature.isPrimary())
				args.put(ATTRIBUTE_PRIMARY, "true"); //$NON-NLS-1$
			value = feature.getApplication();
			if (value != null)
				args.put(ATTRIBUTE_APPLICATION, value);

			// collect the roots
			URL[] roots = feature.getRoots();
			if (roots != null && roots.length > 0)
				args.put(ATTRIBUTE_ROOT, toString(roots));

			writer.startTag(ELEMENT_FEATURE, args);
			writer.endTag(ELEMENT_FEATURE);
		}
	}
}
