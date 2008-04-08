/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;

/**
 * @since 1.0
 */
public class ConfigurationWriter implements ConfigurationConstants {

	/*
	 * Save the given configuration to the specified location.
	 */
	public static void save(Configuration configuration, File location, URL osgiInstallArea) throws ProvisionException {
		XMLWriter writer = null;
		try {
			OutputStream output = new BufferedOutputStream(new FileOutputStream(location));
			writer = new XMLWriter(output);
			Map args = new HashMap();

			String value = configuration.getDate();
			if (value != null)
				args.put(ATTRIBUTE_DATE, value);

			value = configuration.getSharedUR();
			if (value != null)
				args.put(ATTRIBUTE_SHARED_UR, value);

			value = configuration.getVersion();
			if (value != null)
				args.put(ATTRIBUTE_VERSION, value);

			args.put(ATTRIBUTE_TRANSIENT, Boolean.toString(configuration.isTransient()));

			writer.startTag(ELEMENT_CONFIG, args);

			for (Iterator iter = configuration.getSites().iterator(); iter.hasNext();) {
				Site site = (Site) iter.next();
				write(writer, site, osgiInstallArea);
			}

			writer.endTag(ELEMENT_CONFIG);
		} catch (UnsupportedEncodingException e) {
			throw new ProvisionException(NLS.bind(Messages.error_saving_config, location), e);
		} catch (FileNotFoundException e) {
			throw new ProvisionException(NLS.bind(Messages.error_saving_config, location), e);
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}

	/*
	 * Write out the given site.
	 */
	private static void write(XMLWriter writer, Site site, URL osgiInstallArea) {
		Map args = new HashMap();

		String value = site.getLinkFile();
		if (value != null)
			args.put(ATTRIBUTE_LINKFILE, value);

		value = site.getPolicy();
		if (value != null)
			args.put(ATTRIBUTE_POLICY, value);

		value = site.getUrl();
		if (value != null) {
			if (osgiInstallArea == null)
				args.put(ATTRIBUTE_URL, value);
			else
				args.put(ATTRIBUTE_URL, Utils.makeRelative(value, osgiInstallArea));
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
	 * Convert the given list to a comma-separated string.
	 */
	private static String toString(String[] list) {
		if (list == null || list.length == 0)
			return null;
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < list.length; i++) {
			buffer.append(list[i]);
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
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			Map args = new HashMap();
			String value = feature.getId();
			if (value != null)
				args.put(ATTRIBUTE_ID, value);
			value = feature.getUrl();
			if (value != null)
				args.put(ATTRIBUTE_URL, value);
			value = feature.getVersion();
			if (value != null)
				args.put(ATTRIBUTE_VERSION, value);
			writer.startTag(ELEMENT_FEATURE, args);
			writer.endTag(ELEMENT_FEATURE);
		}
	}
}
