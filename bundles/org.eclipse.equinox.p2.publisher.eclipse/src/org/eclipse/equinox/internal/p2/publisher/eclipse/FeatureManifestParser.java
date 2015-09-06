/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - split into FeatureParser and FeatureManifestParser
 *     SAP AG - consolidation of publishers for PDE formats
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.publishing.Activator;
import org.eclipse.pde.internal.publishing.Constants;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses a feature manifest from a provided stream.
 */
public class FeatureManifestParser extends DefaultHandler {

	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	private SAXParser parser;
	protected Feature result;
	private URL url;
	private StringBuffer characters = null;
	private MultiStatus status = null;
	private boolean hasImports = false;

	private final List<String> messageKeys = new ArrayList<String>();

	public FeatureManifestParser() {
		this(true);
	}

	public FeatureManifestParser(boolean createParser) {
		super();
		if (!createParser)
			return;
		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			System.out.println(e);
		} catch (SAXException e) {
			System.out.println(e);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		if (characters == null)
			return;
		characters.append(ch, start, length);
	}

	protected Feature createFeature(String id, String version) {
		return new Feature(id, version);
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if ("requires".equals(localName) && !hasImports) { //$NON-NLS-1$
			error(Messages.feature_parse_emptyRequires);
		}
		if (characters == null)
			return;
		if ("description".equals(localName)) { //$NON-NLS-1$
			result.setDescription(localize(characters.toString().trim()));
		} else if ("license".equals(localName)) { //$NON-NLS-1$
			result.setLicense(localize(characters.toString().trim()));
		} else if ("copyright".equals(localName)) { //$NON-NLS-1$
			result.setCopyright(localize(characters.toString().trim()));
		}
		characters = null;
	}

	private void error(String message) {
		if (status == null) {
			String msg = NLS.bind(Messages.exception_featureParse, url.toExternalForm());
			status = new MultiStatus(Activator.ID, Constants.EXCEPTION_FEATURE_PARSE, msg, null);
		}
		status.add(new Status(IStatus.ERROR, Activator.ID, Constants.EXCEPTION_FEATURE_PARSE, message, null));
	}

	public MultiStatus getStatus() {
		return status;
	}

	public List<String> getMessageKeys() {
		return messageKeys;
	}

	public Feature getResult() {
		return result;
	}

	private String localize(String value) {
		if (value != null && value.startsWith("%")) { //$NON-NLS-1$
			String key = value.substring(1);
			messageKeys.add(key);
		}
		return value;
	}

	/**
	 * Parse the given input stream and return a feature object
	 * or null.
	 */
	public Feature parse(InputStream in, URL featureURL) throws SAXException, IOException {
		result = null;
		url = featureURL;
		parser.parse(new InputSource(in), this);
		return result;
	}

	private void processCopyright(Attributes attributes) {
		result.setCopyrightURL(attributes.getValue("url")); //$NON-NLS-1$
		characters = new StringBuffer();
	}

	private void processDescription(Attributes attributes) {
		result.setDescriptionURL(attributes.getValue("url")); //$NON-NLS-1$
		characters = new StringBuffer();
	}

	private void processDiscoverySite(Attributes attributes) {
		//ignore discovery sites of type 'web'
		if ("web".equals(attributes.getValue("type"))) //$NON-NLS-1$ //$NON-NLS-2$
			return;
		result.addDiscoverySite(attributes.getValue("label"), attributes.getValue("url")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void processFeature(Attributes attributes) {
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		if (id == null || id.trim().equals("") //$NON-NLS-1$
				|| ver == null || ver.trim().equals("")) { //$NON-NLS-1$
			error(NLS.bind(Messages.feature_parse_invalidIdOrVersion, (new String[] {id, ver})));
		} else {
			result = createFeature(id, ver);

			String os = attributes.getValue("os"); //$NON-NLS-1$
			String ws = attributes.getValue("ws"); //$NON-NLS-1$
			String nl = attributes.getValue("nl"); //$NON-NLS-1$
			String arch = attributes.getValue("arch"); //$NON-NLS-1$
			result.setEnvironment(os, ws, arch, nl);

			result.setApplication(attributes.getValue("application")); //$NON-NLS-1$
			result.setBrandingPlugin(attributes.getValue("plugin")); //$NON-NLS-1$
			result.setExclusive(Boolean.parseBoolean(attributes.getValue("exclusive"))); //$NON-NLS-1$
			result.setPrimary(Boolean.parseBoolean(attributes.getValue("primary"))); //$NON-NLS-1$
			result.setColocationAffinity(attributes.getValue("colocation-affinity")); //$NON-NLS-1$

			result.setProviderName(localize(attributes.getValue("provider-name"))); //$NON-NLS-1$
			result.setLabel(localize(attributes.getValue("label"))); //$NON-NLS-1$
			result.setImage(attributes.getValue("image")); //$NON-NLS-1$
			result.setLicenseFeature(attributes.getValue("license-feature")); //$NON-NLS-1$
			result.setLicenseFeatureVersion(attributes.getValue("license-feature-version")); //$NON-NLS-1$

			//			Utils.debug("End process DefaultFeature tag: id:" +id + " ver:" +ver + " url:" + feature.getURL()); 	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private void processImport(Attributes attributes) {
		String id = attributes.getValue("feature"); //$NON-NLS-1$
		boolean isPlugin = false;
		if (id == null) {
			id = attributes.getValue("plugin"); //$NON-NLS-1$
			if (id == null)
				throw new IllegalStateException();
			isPlugin = true;
		}
		String versionStr = attributes.getValue("version"); //$NON-NLS-1$
		FeatureEntry entry = null;
		if ("versionRange".equals(attributes.getValue("match"))) { //$NON-NLS-1$//$NON-NLS-2$
			VersionRange versionRange = new VersionRange(versionStr);
			entry = FeatureEntry.createRequires(id, versionRange, attributes.getValue("match"), attributes.getValue("filter"), isPlugin); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			entry = FeatureEntry.createRequires(id, versionStr, attributes.getValue("match"), attributes.getValue("filter"), isPlugin); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!isPlugin && "true".equalsIgnoreCase(attributes.getValue("patch"))) { //$NON-NLS-1$ //$NON-NLS-2$
			entry.setPatch(true);
		}
		hasImports = true;
		result.addEntry(entry);
	}

	private void processIncludes(Attributes attributes) {
		FeatureEntry entry = new FeatureEntry(attributes.getValue("id"), attributes.getValue("version"), false); //$NON-NLS-1$ //$NON-NLS-2$
		String unpack = attributes.getValue("unpack"); //$NON-NLS-1$
		if (unpack != null)
			entry.setUnpack(Boolean.parseBoolean(unpack));
		String optional = attributes.getValue("optional"); //$NON-NLS-1$
		if (optional != null)
			entry.setOptional(Boolean.parseBoolean(optional));
		setEnvironment(attributes, entry);
		String filter = attributes.getValue("filter"); //$NON-NLS-1$
		if (filter != null)
			entry.setFilter(filter);
		result.addEntry(entry);
	}

	private void processInstallHandler(Attributes attributes) {
		result.setInstallHandler(attributes.getValue("handler")); //$NON-NLS-1$
		result.setInstallHandlerLibrary(attributes.getValue("library")); //$NON-NLS-1$
		result.setInstallHandlerURL(attributes.getValue("url")); //$NON-NLS-1$
	}

	private void processLicense(Attributes attributes) {
		result.setLicenseURL(attributes.getValue("url")); //$NON-NLS-1$
		characters = new StringBuffer();
	}

	private void processPlugin(Attributes attributes) {
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String version = attributes.getValue("version"); //$NON-NLS-1$

		if (id == null || id.trim().equals("") || version == null || version.trim().equals("")) { //$NON-NLS-1$ //$NON-NLS-2$
			error(NLS.bind(Messages.feature_parse_invalidIdOrVersion, (new String[] {id, version})));
		} else {
			FeatureEntry plugin = new FeatureEntry(id, version, true);
			setEnvironment(attributes, plugin);
			String unpack = attributes.getValue("unpack"); //$NON-NLS-1$
			if (unpack != null)
				plugin.setUnpack(Boolean.parseBoolean(unpack));
			String fragment = attributes.getValue("fragment"); //$NON-NLS-1$
			if (fragment != null)
				plugin.setFragment(Boolean.parseBoolean(fragment));
			String filter = attributes.getValue("filter"); //$NON-NLS-1$
			if (filter != null)
				plugin.setFilter(filter);
			result.addEntry(plugin);

			//			Utils.debug("End process DefaultFeature tag: id:" + id + " ver:" + ver + " url:" + feature.getURL()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private void processUpdateSite(Attributes attributes) {
		result.setUpdateSiteLabel(attributes.getValue("label")); //$NON-NLS-1$
		result.setUpdateSiteURL(attributes.getValue("url")); //$NON-NLS-1$
	}

	private void setEnvironment(Attributes attributes, FeatureEntry entry) {
		String os = attributes.getValue("os"); //$NON-NLS-1$
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		String nl = attributes.getValue("nl"); //$NON-NLS-1$
		String arch = attributes.getValue("arch"); //$NON-NLS-1$
		entry.setEnvironment(os, ws, arch, nl);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		//		Utils.debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if ("plugin".equals(localName)) { //$NON-NLS-1$
			processPlugin(attributes);
		} else if ("description".equals(localName)) { //$NON-NLS-1$
			processDescription(attributes);
		} else if ("license".equals(localName)) { //$NON-NLS-1$
			processLicense(attributes);
		} else if ("copyright".equals(localName)) { //$NON-NLS-1$
			processCopyright(attributes);
		} else if ("feature".equals(localName)) { //$NON-NLS-1$
			processFeature(attributes);
		} else if ("import".equals(localName)) { //$NON-NLS-1$
			processImport(attributes);
		} else if ("includes".equals(localName)) { //$NON-NLS-1$
			processIncludes(attributes);
		} else if ("install-handler".equals(localName)) { //$NON-NLS-1$
			processInstallHandler(attributes);
		} else if ("update".equals(localName)) { //$NON-NLS-1$
			processUpdateSite(attributes);
		} else if ("discovery".equals(localName)) { //$NON-NLS-1$
			processDiscoverySite(attributes);
		}
	}

}
