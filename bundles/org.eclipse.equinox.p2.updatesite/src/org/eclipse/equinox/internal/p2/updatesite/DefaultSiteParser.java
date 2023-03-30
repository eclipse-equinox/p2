/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - 378338: Support for "bundle" element
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.SecureXMLUtil;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.publisher.eclipse.URLEntry;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses a site.xml file. This class was initially copied from
 * org.eclipse.update.core.model.DefaultSiteParser.
 */
public class DefaultSiteParser extends DefaultHandler {

	private static final String ARCHIVE = "archive"; //$NON-NLS-1$
	private static final String CATEGORY = "category"; //$NON-NLS-1$
	private static final String CATEGORY_DEF = "category-def"; //$NON-NLS-1$

	private static final String ASSOCIATE_SITES_URL = "associateSitesURL"; //$NON-NLS-1$
	private static final String ASSOCIATE_SITE = "associateSite"; //$NON-NLS-1$
	private static final String DEFAULT_INFO_URL = "index.html"; //$NON-NLS-1$
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String FEATURE = "feature"; //$NON-NLS-1$
	private static final String BUNDLE = "bundle"; //$NON-NLS-1$
	private static final String FEATURES = "features/"; //$NON-NLS-1$
	private static final String PLUGINS = "plugins/"; //$NON-NLS-1$
	private static final String PLUGIN_ID = Activator.ID;
	private static final String SITE = "site"; //$NON-NLS-1$

	private static final int STATE_ARCHIVE = 3;
	private static final int STATE_CATEGORY = 4;
	private static final int STATE_CATEGORY_DEF = 5;
	private static final int STATE_DESCRIPTION_CATEGORY_DEF = 7;
	private static final int STATE_DESCRIPTION_SITE = 6;
	private static final int STATE_FEATURE = 2;
	private static final int STATE_BUNDLE = 8;
	private static final int STATE_IGNORED_ELEMENT = -1;
	private static final int STATE_INITIAL = 0;
	private static final int STATE_SITE = 1;

	private int currentState;

	private boolean DESCRIPTION_SITE_ALREADY_SEEN = false;
	// Current object stack (used to hold the current object we are
	// populating in this plugin descriptor
	Stack<Object> objectStack = new Stack<>();

	private SAXParser parser;

	// Current State Information
	Stack<Integer> stateStack = new Stack<>();

	// List of string keys for translated strings
	private final List<String> messageKeys = new ArrayList<>(4);

	private MultiStatus status;
	private final URI siteLocation;

	/*
	 * 
	 */
	private static void debug(String s) {
		Tracing.debug("DefaultSiteParser: " + s); //$NON-NLS-1$
	}

	private static URLEntry[] getAssociateSites(String associateSitesURL) {

		try {
			DocumentBuilderFactory domFactory = SecureXMLUtil.newSecureDocumentBuilderFactory();
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document document = builder.parse(associateSitesURL);
			if (document == null)
				return null;
			NodeList mirrorNodes = document.getElementsByTagName(ASSOCIATE_SITE);
			URLEntry[] mirrors = new URLEntry[mirrorNodes.getLength()];
			for (int i = 0; i < mirrorNodes.getLength(); i++) {
				Element mirrorNode = (Element) mirrorNodes.item(i);
				mirrors[i] = new URLEntry();
				String infoURL = mirrorNode.getAttribute("url"); //$NON-NLS-1$
				String label = mirrorNode.getAttribute("label"); //$NON-NLS-1$
				mirrors[i].setURL(infoURL);
				mirrors[i].setAnnotation(label);

				if (Tracing.DEBUG_GENERATOR_PARSING)
					debug("Processed mirror: url:" + infoURL + " label:" + label); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return mirrors;
		} catch (Exception e) {
			// log if absolute url
			if (associateSitesURL != null && (associateSitesURL.startsWith("http://") //$NON-NLS-1$
					|| associateSitesURL.startsWith("https://") //$NON-NLS-1$
					|| associateSitesURL.startsWith("file://") //$NON-NLS-1$
					|| associateSitesURL.startsWith("ftp://") //$NON-NLS-1$
					|| associateSitesURL.startsWith("jar://"))) //$NON-NLS-1$
				log(Messages.DefaultSiteParser_mirrors, e);
			return null;
		}
	}

	static void log(Exception e) {
		LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Internal Error", e)); //$NON-NLS-1$
	}

	static void log(String message) {
		LogHelper.log(new Status(IStatus.WARNING, Activator.ID, message, null));
	}

	static void log(String message, Exception e) {
		LogHelper.log(new Status(IStatus.WARNING, Activator.ID, message, e));
	}

	/**
	 * Constructs a site parser.
	 */
	public DefaultSiteParser(URI siteLocation) {
		super();
		this.siteLocation = siteLocation;
		stateStack = new Stack<>();
		objectStack = new Stack<>();
		status = null;
		DESCRIPTION_SITE_ALREADY_SEEN = false;
		try {
			SAXParserFactory parserFactory = SecureXMLUtil.newSecureSAXParserFactory();
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			log(e);
		} catch (SAXException e) {
			log(e);
		}

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("Created"); //$NON-NLS-1$
	}

	/**
	 * Handle character text
	 * 
	 * @see DefaultHandler#characters(char[], int, int)
	 * @since 2.0
	 */
	@Override
	public void characters(char[] ch, int start, int length) {
		String text = new String(ch, start, length);
		// only push if description
		int state = stateStack.peek().intValue();
		if (state == STATE_DESCRIPTION_SITE || state == STATE_DESCRIPTION_CATEGORY_DEF)
			objectStack.push(text);

	}

	/**
	 * Handle end of element tags
	 * 
	 * @see DefaultHandler#endElement(String, String, String)
	 * @since 2.0
	 */
	@Override
	public void endElement(String uri, String localName, String qName) {

		String text = null;
		URLEntry info = null;

		int state = stateStack.peek().intValue();
		switch (state) {
		case STATE_IGNORED_ELEMENT:
		case STATE_ARCHIVE:
		case STATE_CATEGORY:
			stateStack.pop();
			break;

		case STATE_INITIAL:
			internalError(Messages.DefaultSiteParser_ParsingStackBackToInitialState);
			break;

		case STATE_SITE:
			stateStack.pop();
			if (objectStack.peek() instanceof String) {
				text = (String) objectStack.pop();
				SiteModel site = (SiteModel) objectStack.peek();
				site.getDescription().setAnnotation(text);
			}
			// do not pop the object
			break;

		case STATE_FEATURE:
			stateStack.pop();
			objectStack.pop();
			break;

		case STATE_BUNDLE:
			stateStack.pop();
			objectStack.pop();
			break;

		case STATE_CATEGORY_DEF:
			stateStack.pop();
			if (objectStack.peek() instanceof String) {
				text = (String) objectStack.pop();
				SiteCategory category = (SiteCategory) objectStack.peek();
				category.setDescription(text);
			}
			objectStack.pop();
			break;

		case STATE_DESCRIPTION_SITE:
			stateStack.pop();
			text = ""; //$NON-NLS-1$
			while (objectStack.peek() instanceof String) {
				// add text, preserving at most one space between text fragments
				String newText = (String) objectStack.pop();
				if (trailingSpace(newText) && !leadingSpace(text)) {
					text = " " + text; //$NON-NLS-1$
				}
				text = newText.trim() + text;
				if (leadingSpace(newText) && !leadingSpace(text)) {
					text = " " + text; //$NON-NLS-1$
				}
			}
			text = text.trim();

			info = (URLEntry) objectStack.pop();
			if (text != null)
				info.setAnnotation(text);

			SiteModel siteModel = (SiteModel) objectStack.peek();
			// override description.
			// do not raise error as previous description may be default one
			// when parsing site tag
			if (DESCRIPTION_SITE_ALREADY_SEEN)
				debug(NLS.bind(Messages.DefaultSiteParser_ElementAlreadySet, (new String[] { getState(state) })));
			siteModel.setDescription(info);
			DESCRIPTION_SITE_ALREADY_SEEN = true;
			break;

		case STATE_DESCRIPTION_CATEGORY_DEF:
			stateStack.pop();
			text = ""; //$NON-NLS-1$
			while (objectStack.peek() instanceof String) {
				// add text, preserving at most one space between text fragments
				String newText = (String) objectStack.pop();
				if (trailingSpace(newText) && !leadingSpace(text)) {
					text = " " + text; //$NON-NLS-1$
				}
				text = newText.trim() + text;
				if (leadingSpace(newText) && !leadingSpace(text)) {
					text = " " + text; //$NON-NLS-1$
				}
			}
			text = text.trim();

			info = (URLEntry) objectStack.pop();
			if (text != null)
				info.setAnnotation(text);

			SiteCategory category = (SiteCategory) objectStack.peek();
			if (category.getDescription() != null)
				internalError(NLS.bind(Messages.DefaultSiteParser_ElementAlreadySet,
						(new String[] { getState(state), category.getLabel() })));
			else {
				checkTranslated(info.getAnnotation());
				category.setDescription(info.getAnnotation());
			}
			break;

		default:
			internalError(NLS.bind(Messages.DefaultSiteParser_UnknownEndState, (new String[] { getState(state) })));
			break;
		}

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End Element:" + uri + ":" + localName + ":" + qName);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/*
	 * Handles an error state specified by the status. The collection of all logged
	 * status objects can be accessed using <code>getStatus()</code>.
	 *
	 * @param error a status detailing the error condition
	 */
	private void error(IStatus error) {

		if (status == null) {
			status = new MultiStatus(PLUGIN_ID, 0, Messages.DefaultSiteParser_ErrorParsingSite, null);
		}

		status.add(error);
		if (Tracing.DEBUG_GENERATOR_PARSING)
			LogHelper.log(error);
	}

	/**
	 * Handle errors
	 * 
	 * @see DefaultHandler#error(SAXParseException)
	 * @since 2.0
	 */
	@Override
	public void error(SAXParseException ex) {
		logStatus(ex);
	}

	/**
	 * Handle fatal errors
	 * 
	 * @see DefaultHandler#fatalError(SAXParseException)
	 * @exception SAXException
	 * @since 2.0
	 */
	@Override
	public void fatalError(SAXParseException ex) throws SAXException {
		logStatus(ex);
		throw ex;
	}

	/*
	 * return the state as String
	 */
	private String getState(int state) {

		switch (state) {
		case STATE_IGNORED_ELEMENT:
			return "Ignored"; //$NON-NLS-1$

		case STATE_INITIAL:
			return "Initial"; //$NON-NLS-1$

		case STATE_SITE:
			return "Site"; //$NON-NLS-1$

		case STATE_FEATURE:
			return "Feature"; //$NON-NLS-1$

		case STATE_BUNDLE:
			return "Bundle"; //$NON-NLS-1$

		case STATE_ARCHIVE:
			return "Archive"; //$NON-NLS-1$

		case STATE_CATEGORY:
			return "Category"; //$NON-NLS-1$

		case STATE_CATEGORY_DEF:
			return "Category Def"; //$NON-NLS-1$

		case STATE_DESCRIPTION_CATEGORY_DEF:
			return "Description / Category Def"; //$NON-NLS-1$

		case STATE_DESCRIPTION_SITE:
			return "Description / Site"; //$NON-NLS-1$

		default:
			return Messages.DefaultSiteParser_UnknownState;
		}
	}

	/**
	 * Returns all status objects accumulated by the parser.
	 *
	 * @return multi-status containing accumulated status, or <code>null</code>.
	 * @since 2.0
	 */
	public MultiStatus getStatus() {
		return status;
	}

	private void handleCategoryDefState(String elementName, Attributes attributes) {
		switch (elementName) {
		case FEATURE:
			stateStack.push(Integer.valueOf(STATE_FEATURE));
			processFeature(attributes);
			break;
		case BUNDLE:
			stateStack.push(Integer.valueOf(STATE_BUNDLE));
			processBundle(attributes);
			break;
		case ARCHIVE:
			stateStack.push(Integer.valueOf(STATE_ARCHIVE));
			processArchive(attributes);
			break;
		case CATEGORY_DEF:
			stateStack.push(Integer.valueOf(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
			break;
		case DESCRIPTION:
			stateStack.push(Integer.valueOf(STATE_DESCRIPTION_CATEGORY_DEF));
			processInfo(attributes);
			break;
		default:
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement,
					(new String[] { elementName, getState(currentState) })));
			break;
		}
	}

	private void handleCategoryState(String elementName, Attributes attributes) {
		switch (elementName) {
		case DESCRIPTION:
			stateStack.push(Integer.valueOf(STATE_DESCRIPTION_SITE));
			processInfo(attributes);
			break;
		case FEATURE:
			stateStack.push(Integer.valueOf(STATE_FEATURE));
			processFeature(attributes);
			break;
		case BUNDLE:
			stateStack.push(Integer.valueOf(STATE_BUNDLE));
			processBundle(attributes);
			break;
		case ARCHIVE:
			stateStack.push(Integer.valueOf(STATE_ARCHIVE));
			processArchive(attributes);
			break;
		case CATEGORY_DEF:
			stateStack.push(Integer.valueOf(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
			break;
		case CATEGORY:
			stateStack.push(Integer.valueOf(STATE_CATEGORY));
			processCategory(attributes);
			break;
		default:
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement,
					(new String[] { elementName, getState(currentState) })));
			break;
		}
	}

	private void handleFeatureState(String elementName, Attributes attributes) {
		switch (elementName) {
		case DESCRIPTION:
			stateStack.push(Integer.valueOf(STATE_DESCRIPTION_SITE));
			processInfo(attributes);
			break;
		case FEATURE:
			stateStack.push(Integer.valueOf(STATE_FEATURE));
			processFeature(attributes);
			break;
		case ARCHIVE:
			stateStack.push(Integer.valueOf(STATE_ARCHIVE));
			processArchive(attributes);
			break;
		case CATEGORY_DEF:
			stateStack.push(Integer.valueOf(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
			break;
		case CATEGORY:
			stateStack.push(Integer.valueOf(STATE_CATEGORY));
			processCategory(attributes);
			break;
		default:
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement,
					(new String[] { elementName, getState(currentState) })));
			break;
		}
	}

	private void handleBundleState(String elementName, Attributes attributes) {
		switch (elementName) {
		case DESCRIPTION:
			stateStack.push(Integer.valueOf(STATE_DESCRIPTION_SITE));
			processInfo(attributes);
			break;
		case ARCHIVE:
			stateStack.push(Integer.valueOf(STATE_ARCHIVE));
			processArchive(attributes);
			break;
		case CATEGORY_DEF:
			stateStack.push(Integer.valueOf(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
			break;
		case CATEGORY:
			stateStack.push(Integer.valueOf(STATE_CATEGORY));
			processCategory(attributes);
			break;
		default:
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement,
					(new String[] { elementName, getState(currentState) })));
			break;
		}
	}

	private void handleInitialState(String elementName, Attributes attributes) throws SAXException {
		if (elementName.equals(SITE)) {
			stateStack.push(Integer.valueOf(STATE_SITE));
			processSite(attributes);
		} else {
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement,
					(new String[] { elementName, getState(currentState) })));
			// what we received was not a site.xml, no need to continue
			throw new SAXException(Messages.DefaultSiteParser_InvalidXMLStream);
		}

	}

	private void handleSiteState(String elementName, Attributes attributes) {
		switch (elementName) {
		case DESCRIPTION:
			stateStack.push(Integer.valueOf(STATE_DESCRIPTION_SITE));
			processInfo(attributes);
			break;
		case FEATURE:
			stateStack.push(Integer.valueOf(STATE_FEATURE));
			processFeature(attributes);
			break;
		case BUNDLE:
			stateStack.push(Integer.valueOf(STATE_BUNDLE));
			processBundle(attributes);
			break;
		case ARCHIVE:
			stateStack.push(Integer.valueOf(STATE_ARCHIVE));
			processArchive(attributes);
			break;
		case CATEGORY_DEF:
			stateStack.push(Integer.valueOf(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
			break;
		default:
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement,
					(new String[] { elementName, getState(currentState) })));
			break;
		}
	}

	/*
	 * 
	 */
	private void internalError(String message) {
		error(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, message, null));
	}

	/*
	 * 
	 */
	private void internalErrorUnknownTag(String msg) {
		stateStack.push(Integer.valueOf(STATE_IGNORED_ELEMENT));
		internalError(msg);
	}

	private boolean leadingSpace(String str) {
		if (str.length() <= 0) {
			return false;
		}
		return Character.isWhitespace(str.charAt(0));
	}

	/*
	 * 
	 */
	private void logStatus(SAXParseException ex) {
		String name = ex.getSystemId();
		if (name == null)
			name = ""; //$NON-NLS-1$
		else
			name = name.substring(1 + name.lastIndexOf("/")); //$NON-NLS-1$

		String msg;
		if (name.equals("")) //$NON-NLS-1$
			name = siteLocation.toString();
		String[] values = new String[] { name, Integer.toString(ex.getLineNumber()),
				Integer.toString(ex.getColumnNumber()), ex.getMessage() };
		msg = NLS.bind(Messages.DefaultSiteParser_ErrorlineColumnMessage, values);
		error(new Status(IStatus.ERROR, PLUGIN_ID, msg, ex));
	}

	/**
	 * Parses the specified input steam and constructs a site model. The input
	 * stream is not closed as part of this operation.
	 * 
	 * @param in input stream
	 * @return site model
	 * @exception SAXException
	 * @exception IOException
	 * @since 2.0
	 */
	public SiteModel parse(InputStream in) throws SAXException, IOException {
		stateStack.push(Integer.valueOf(STATE_INITIAL));
		currentState = stateStack.peek().intValue();
		parser.parse(new InputSource(in), this);
		if (objectStack.isEmpty())
			throw new SAXException(Messages.DefaultSiteParser_NoSiteTag);
		if (objectStack.peek() instanceof SiteModel) {
			SiteModel site = (SiteModel) objectStack.pop();
			site.setMessageKeys(messageKeys);
			return site;
		}
		String stack = ""; //$NON-NLS-1$
		Iterator<Object> iter = objectStack.iterator();
		while (iter.hasNext()) {
			stack = stack + iter.next().toString() + "\r\n"; //$NON-NLS-1$
		}
		throw new SAXException(NLS.bind(Messages.DefaultSiteParser_WrongParsingStack, (new String[] { stack })));
	}

	/*
	 * process archive info
	 */
	private void processArchive(Attributes attributes) {
		URLEntry archive = new URLEntry();
		String id = attributes.getValue("path"); //$NON-NLS-1$
		if (id == null || id.trim().equals("")) { //$NON-NLS-1$
			internalError(
					NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] { "path", getState(currentState) }))); //$NON-NLS-1$
		}

		archive.setAnnotation(id);

		String url = attributes.getValue("url"); //$NON-NLS-1$
		if (url == null || url.trim().equals("")) { //$NON-NLS-1$
			internalError(
					NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] { "archive", getState(currentState) }))); //$NON-NLS-1$
		} else {
			archive.setURL(url);

			SiteModel site = (SiteModel) objectStack.peek();
			site.addArchive(archive);
		}
		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End processing Archive: path:" + id + " url:" + url);//$NON-NLS-1$ //$NON-NLS-2$

	}

	/*
	 * process the Category info
	 */
	private void processCategory(Attributes attributes) {
		String category = attributes.getValue("name"); //$NON-NLS-1$
		Object item = objectStack.peek();
		if (item instanceof SiteFeature) {
			SiteFeature feature = (SiteFeature) item;
			feature.addCategoryName(category);
		} else if (item instanceof SiteBundle) {
			SiteBundle bundle = (SiteBundle) item;
			bundle.addCategoryName(category);
		}

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End processing Category: name:" + category); //$NON-NLS-1$
	}

	/*
	 * process category def info
	 */
	private void processCategoryDef(Attributes attributes) {
		SiteCategory category = new SiteCategory();
		String name = attributes.getValue("name"); //$NON-NLS-1$
		String label = attributes.getValue("label"); //$NON-NLS-1$
		checkTranslated(label);
		category.setName(name);
		category.setLabel(label);

		SiteModel site = (SiteModel) objectStack.peek();
		site.addCategory(category);
		objectStack.push(category);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End processing CategoryDef: name:" + name + " label:" + label); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * process feature info
	 */
	private void processFeature(Attributes attributes) {
		SiteFeature feature = new SiteFeature();

		// feature location on the site
		String urlInfo = attributes.getValue("url"); //$NON-NLS-1$
		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		boolean noURL = (urlInfo == null || urlInfo.trim().equals("")); //$NON-NLS-1$
		boolean noId = (id == null || id.trim().equals("")); //$NON-NLS-1$
		boolean noVersion = (ver == null || ver.trim().equals("")); //$NON-NLS-1$

		// We need to have id and version, or the url, or both.
		if (noURL) {
			if (noId || noVersion)
				internalError(
						NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] { "url", getState(currentState) }))); //$NON-NLS-1$
			else
				// default url
				urlInfo = FEATURES + id + '_' + ver; //
		}

		feature.setURLString(urlInfo);

		String type = attributes.getValue("type"); //$NON-NLS-1$
		feature.setType(type);

		// if one is null, and not the other
		if (noId ^ noVersion) {
			String[] values = new String[] { id, ver, getState(currentState) };
			log(NLS.bind(Messages.DefaultFeatureParser_IdOrVersionInvalid, values));
		} else {
			feature.setFeatureIdentifier(id);
			feature.setFeatureVersion(ver);
		}

		// get label if it exists
		String label = attributes.getValue("label"); //$NON-NLS-1$
		if (label != null) {
			if ("".equals(label.trim())) //$NON-NLS-1$
				label = null;
			checkTranslated(label);
		}
		feature.setLabel(label);

		// OS
		String os = attributes.getValue("os"); //$NON-NLS-1$
		feature.setOS(os);

		// WS
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		feature.setWS(ws);

		// NL
		String nl = attributes.getValue("nl"); //$NON-NLS-1$
		feature.setNL(nl);

		// arch
		String arch = attributes.getValue("arch"); //$NON-NLS-1$
		feature.setArch(arch);

		// patch
		String patch = attributes.getValue("patch"); //$NON-NLS-1$
		feature.setPatch(patch);

		SiteModel site = (SiteModel) objectStack.peek();
		site.addFeature(feature);
		feature.setSiteModel(site);

		objectStack.push(feature);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End Processing DefaultFeature Tag: url:" + urlInfo + " type:" + type); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/*
	 * process feature info
	 */
	private void processBundle(Attributes attributes) {
		SiteBundle bundle = new SiteBundle();

		// feature location on the site
		String urlInfo = attributes.getValue("url"); //$NON-NLS-1$
		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		boolean noURL = (urlInfo == null || urlInfo.trim().equals("")); //$NON-NLS-1$
		boolean noId = (id == null || id.trim().equals("")); //$NON-NLS-1$
		boolean noVersion = (ver == null || ver.trim().equals("")); //$NON-NLS-1$

		// We need to have id and version, or the url, or both.
		if (noURL) {
			if (noId || noVersion)
				internalError(
						NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] { "url", getState(currentState) }))); //$NON-NLS-1$
			else
				// default url
				urlInfo = PLUGINS + id + '_' + ver; //
		}

		bundle.setURLString(urlInfo);

		String type = attributes.getValue("type"); //$NON-NLS-1$
		bundle.setType(type);

		// if one is null, and not the other
		if (noId ^ noVersion) {
			String[] values = new String[] { id, ver, getState(currentState) };
			log(NLS.bind(Messages.DefaultFeatureParser_IdOrVersionInvalid, values));
		} else {
			bundle.setBundleIdentifier(id);
			bundle.setBundleVersion(ver);
		}

		// get label if it exists
		String label = attributes.getValue("label"); //$NON-NLS-1$
		if (label != null) {
			if ("".equals(label.trim())) //$NON-NLS-1$
				label = null;
			checkTranslated(label);
		}
		bundle.setLabel(label);

		// OS
		String os = attributes.getValue("os"); //$NON-NLS-1$
		bundle.setOS(os);

		// WS
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		bundle.setWS(ws);

		// NL
		String nl = attributes.getValue("nl"); //$NON-NLS-1$
		bundle.setNL(nl);

		// arch
		String arch = attributes.getValue("arch"); //$NON-NLS-1$
		bundle.setArch(arch);

		// patch
		String patch = attributes.getValue("patch"); //$NON-NLS-1$
		bundle.setPatch(patch);

		SiteModel site = (SiteModel) objectStack.peek();
		site.addBundle(bundle);
		bundle.setSiteModel(site);

		objectStack.push(bundle);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End Processing DefaultFeature Tag: url:" + urlInfo + " type:" + type); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/*
	 * process URL info with element text
	 */
	private void processInfo(Attributes attributes) {
		URLEntry inf = new URLEntry();
		String infoURL = attributes.getValue("url"); //$NON-NLS-1$
		inf.setURL(infoURL);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("Processed Info: url:" + infoURL); //$NON-NLS-1$

		objectStack.push(inf);
	}

	/*
	 * process site info
	 */
	private void processSite(Attributes attributes) {
		// create site map
		SiteModel site = new SiteModel();

		// if URL is specified, it replaces the URL of the site
		// used to calculate the location of features and archives
		String siteURL = attributes.getValue("url"); //$NON-NLS-1$
		if (siteURL != null && !("".equals(siteURL.trim()))) { //$NON-NLS-1$
			if (!siteURL.endsWith("/") && !siteURL.endsWith(File.separator)) { //$NON-NLS-1$
				siteURL += "/"; //$NON-NLS-1$
			}
			site.setLocationURIString(siteURL);
		}

		// provide default description URL
		// If <description> is specified, for the site, it takes precedence
		URLEntry description = new URLEntry();
		description.setURL(DEFAULT_INFO_URL);
		site.setDescription(description);

		// verify we can parse the site ...if the site has
		// a different type throw an exception to force reparsing
		// with the matching parser
		String type = attributes.getValue("type"); //$NON-NLS-1$
		site.setType(type);

		// get mirrors, if any
		String mirrorsURL = attributes.getValue("mirrorsURL"); //$NON-NLS-1$
		if (mirrorsURL != null && mirrorsURL.trim().length() > 0) {
			// URLEntry[] mirrors = getMirrors(mirrorsURL);
			// if (mirrors != null)
			// site.setMirrors(mirrors);
			// else

			// Since we are parsing the site at p2 generation time and the
			// mirrors may change, there is no point doing the mirror expansion now
			site.setMirrorsURIString(mirrorsURL);
		}

		String digestURL = attributes.getValue("digestURL"); //$NON-NLS-1$
		if (digestURL != null)
			site.setDigestURIString(digestURL);

		// TODO: Digest locales
		// if ((attributes.getValue("availableLocales") != null) &&
		// (!attributes.getValue("availableLocales").trim().equals(""))) {
		// //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		// StringTokenizer locals = new
		// StringTokenizer(attributes.getValue("availableLocales"), ",");
		// //$NON-NLS-1$//$NON-NLS-2$
		// String[] availableLocals = new String[locals.countTokens()];
		// int i = 0;
		// while (locals.hasMoreTokens()) {
		// availableLocals[i++] = locals.nextToken();
		// }
		// extendedSite.setAvailableLocals(availableLocals);
		// }
		// }
		//
		final String associateURL = attributes.getValue(ASSOCIATE_SITES_URL);
		if (associateURL != null) {
			// resolve the URI relative to the site location
			URI resolvedLocation = siteLocation.resolve(associateURL);
			site.setAssociateSites(getAssociateSites(resolvedLocation.toString()));
		}

		objectStack.push(site);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End process Site tag: siteURL:" + siteURL + " type:" + type);//$NON-NLS-1$ //$NON-NLS-2$

	}

	/**
	 * Handle start of element tags
	 * 
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 * @since 2.0
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		if (Tracing.DEBUG_GENERATOR_PARSING) {
			debug("State: " + currentState); //$NON-NLS-1$
			debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		switch (currentState) {
		case STATE_IGNORED_ELEMENT:
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement,
					(new String[] { localName, getState(currentState) })));
			break;
		case STATE_INITIAL:
			handleInitialState(localName, attributes);
			break;

		case STATE_SITE:
			handleSiteState(localName, attributes);
			break;

		case STATE_FEATURE:
			handleFeatureState(localName, attributes);
			break;

		case STATE_BUNDLE:
			handleBundleState(localName, attributes);
			break;

		case STATE_ARCHIVE:
			handleSiteState(localName, attributes);
			break;

		case STATE_CATEGORY:
			handleCategoryState(localName, attributes);
			break;

		case STATE_CATEGORY_DEF:
			handleCategoryDefState(localName, attributes);
			break;

		case STATE_DESCRIPTION_SITE:
			handleSiteState(localName, attributes);
			break;

		case STATE_DESCRIPTION_CATEGORY_DEF:
			handleSiteState(localName, attributes);
			break;

		default:
			internalErrorUnknownTag(
					NLS.bind(Messages.DefaultSiteParser_UnknownStartState, (new String[] { getState(currentState) })));
			break;
		}
		int newState = stateStack.peek().intValue();
		if (newState != STATE_IGNORED_ELEMENT)
			currentState = newState;

	}

	private boolean trailingSpace(String str) {
		if (str.length() <= 0) {
			return false;
		}
		return Character.isWhitespace(str.charAt(str.length() - 1));
	}

	// Add translatable strings from the site.xml
	// to the list of message keys.
	private void checkTranslated(String value) {
		if (value != null && value.length() > 1 && value.startsWith("%")) //$NON-NLS-1$
			messageKeys.add(value.substring(1));
	}
}
