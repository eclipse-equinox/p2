/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Parser for platform.xml files. 
 * 
 * @since 1.0
 */
public class ConfigurationParser implements ConfigurationConstants {
	static final String PLATFORM_BASE = "platform:/base/"; //$NON-NLS-1$
	private URL osgiInstallArea;

	/*
	 * Parse the given file handle which points to a platform.xml file and a configuration object.
	 * Returns null if the file doesn't exist.
	 */
	static Configuration parse(File file, URL osgiInstallArea) throws ProvisionException {
		return new ConfigurationParser(osgiInstallArea).internalParse(file);
	}

	private ConfigurationParser(URL osgiInstallArea) {
		this.osgiInstallArea = osgiInstallArea;
	}

	/*
	 * Create a feature object based on the given DOM node. 
	 * Return the new feature.
	 */
	private Feature createFeature(Node node, Site site) {
		Feature result = new Feature(site);
		String id = getAttribute(node, ATTRIBUTE_ID);
		if (id != null)
			result.setId(id);
		String url = getAttribute(node, ATTRIBUTE_URL);
		if (url != null)
			result.setUrl(url);
		String version = getAttribute(node, ATTRIBUTE_VERSION);
		if (version != null)
			result.setVersion(version);
		String pluginIdentifier = getAttribute(node, ATTRIBUTE_PLUGIN_IDENTIFIER);
		if (pluginIdentifier != null)
			result.setPluginIdentifier(pluginIdentifier);
		String pluginVersion = getAttribute(node, ATTRIBUTE_PLUGIN_VERSION);
		// plug-in version is the same as the feature version if it is missing
		if (pluginVersion == null)
			pluginVersion = version;
		if (pluginVersion != null)
			result.setPluginVersion(pluginVersion);
		String application = getAttribute(node, ATTRIBUTE_APPLICATION);
		if (application != null)
			result.setApplication(application);

		// get primary flag
		String flag = getAttribute(node, ATTRIBUTE_PRIMARY);
		if (flag != null && Boolean.parseBoolean(flag))
			result.setPrimary(true);

		// get install locations
		String locations = getAttribute(node, ATTRIBUTE_ROOT);
		if (locations != null) {
			StringTokenizer tokenizer = new StringTokenizer(locations, ","); //$NON-NLS-1$
			ArrayList<URL> rootList = new ArrayList<URL>();
			while (tokenizer.hasMoreTokens()) {
				try {
					URL rootEntry = new URL(tokenizer.nextToken().trim());
					rootList.add(rootEntry);
				} catch (MalformedURLException e) {
					// skip bad entries ...
				}
			}
			URL[] roots = rootList.toArray(new URL[rootList.size()]);
			result.setRoots(roots);
		}

		return result;
	}

	/*
	 * Create the features from the given DOM node.
	 */
	private void createFeatures(Node node, Site site) {
		NodeList children = node.getChildNodes();
		int size = children.getLength();
		for (int i = 0; i < size; i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE)
				continue;
			if (!ELEMENT_FEATURE.equalsIgnoreCase(child.getNodeName()))
				continue;
			Feature feature = createFeature(child, site);
			if (feature != null)
				site.addFeature(feature);
		}
	}

	/*
	 * Create a site based on the given DOM node.
	 */
	private Site createSite(Node node) {
		Site result = new Site();
		String policy = getAttribute(node, ATTRIBUTE_POLICY);
		if (policy != null)
			result.setPolicy(policy);
		String enabled = getAttribute(node, ATTRIBUTE_ENABLED);
		if (enabled != null)
			result.setEnabled(Boolean.parseBoolean(enabled));
		String updateable = getAttribute(node, ATTRIBUTE_UPDATEABLE);
		if (updateable != null)
			result.setUpdateable(Boolean.parseBoolean(updateable));
		String url = getAttribute(node, ATTRIBUTE_URL);
		if (url != null) {
			try {
				// do this to ensure the location is an encoded URI
				URI uri = URIUtil.fromString(url);
				URI osgiURI = osgiInstallArea != null ? URIUtil.toURI(osgiInstallArea) : null;
				result.setUrl(getLocation(uri, osgiURI).toString());
			} catch (URISyntaxException e) {
				result.setUrl(url);
			}
		}
		String linkFile = getAttribute(node, ATTRIBUTE_LINKFILE);
		if (linkFile != null)
			result.setLinkFile(linkFile);
		String list = getAttribute(node, ATTRIBUTE_LIST);
		if (list != null)
			for (StringTokenizer tokenizer = new StringTokenizer(list, ","); tokenizer.hasMoreTokens();) //$NON-NLS-1$
				result.addPlugin(tokenizer.nextToken());
		createFeatures(node, result);
		return result;
	}

	/*
	 * Convert the given url string to an absolute url. If the string is 
	 * platform:/base/ then return a string which represents the osgi
	 * install area.
	 */
	private URI getLocation(URI location, URI osgiArea) {
		if (osgiArea == null)
			return location;
		if (PLATFORM_BASE.equals(location.toString()))
			return osgiArea;
		return URIUtil.makeAbsolute(location, osgiArea);
	}

	/*
	 * Return the attribute with the given name, or null if it does
	 * not exist.
	 */
	private String getAttribute(Node node, String name) {
		NamedNodeMap attributes = node.getAttributes();
		Node temp = attributes.getNamedItem(name);
		return temp == null ? null : temp.getNodeValue();
	}

	/*
	 * Load the given file into a DOM document.
	 */
	private Document load(InputStream input) throws ParserConfigurationException, IOException, SAXException {
		// load the feature xml
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		input = new BufferedInputStream(input);
		try {
			return builder.parse(input);
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	/*
	 * Parse the given file handle which points to a platform.xml file and a configuration object.
	 * Returns null if the file doesn't exist.
	 */
	private Configuration internalParse(File file) throws ProvisionException {
		if (!file.exists()) {
			// remove from cache since it doesn't exist anymore on disk
			ConfigurationCache.put(file, null);
			return null;
		}
		// have we read this before?
		Configuration result = ConfigurationCache.get(file);
		if (result != null)
			return result;
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(file));
			Document document = load(input);
			result = process(document);
			// save for future use
			ConfigurationCache.put(file, result);
			return result;
		} catch (IOException e) {
			throw new ProvisionException(NLS.bind(Messages.error_reading_config, file), e);
		} catch (ParserConfigurationException e) {
			throw new ProvisionException(Messages.error_parsing_config, e);
		} catch (SAXException e) {
			throw new ProvisionException(Messages.error_parsing_config, e);
		}
	}

	/*
	 * Process the given DOM document and create the appropriate
	 * site objects.
	 */
	private Configuration process(Document document) {
		Node node = getConfigElement(document);
		if (node == null)
			return null;
		Configuration configuration = createConfiguration(node);
		NodeList children = node.getChildNodes();
		int size = children.getLength();
		for (int i = 0; i < size; i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE)
				continue;
			if (!ELEMENT_SITE.equalsIgnoreCase(child.getNodeName()))
				continue;
			Site site = createSite(child);
			if (site != null)
				configuration.add(site);
		}
		return configuration;
	}

	private Configuration createConfiguration(Node node) {
		Configuration result = new Configuration();
		String value = getAttribute(node, ATTRIBUTE_DATE);
		if (value != null)
			result.setDate(value);
		value = getAttribute(node, ATTRIBUTE_TRANSIENT);
		if (value != null)
			result.setTransient(Boolean.parseBoolean(value));
		value = getAttribute(node, ATTRIBUTE_SHARED_UR);
		if (value != null)
			result.setSharedUR(value);
		value = getAttribute(node, ATTRIBUTE_VERSION);
		if (value != null)
			result.setVersion(value);
		return result;
	}

	private Node getConfigElement(Document doc) {
		NodeList children = doc.getChildNodes();
		int size = children.getLength();
		for (int i = 0; i < size; i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE)
				continue;
			if (ELEMENT_CONFIG.equalsIgnoreCase(child.getNodeName()))
				return child;
		}
		return null;
	}
}
