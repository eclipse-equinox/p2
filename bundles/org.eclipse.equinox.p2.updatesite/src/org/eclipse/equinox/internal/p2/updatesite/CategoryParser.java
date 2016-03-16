/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - 383795 (bundle element), 406902 (nested categories)
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.p2.publisher.eclipse.URLEntry;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses a category.xml file.
 * This class was initially copied from org.eclipse.update.core.model.DefaultSiteParser.
 */
public class CategoryParser extends DefaultHandler {
	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	private static final String PLUGIN_ID = Activator.ID;

	private static final String ARCHIVE = "archive"; //$NON-NLS-1$
	private static final String CATEGORY = "category"; //$NON-NLS-1$
	private static final String CATEGORY_DEF = "category-def"; //$NON-NLS-1$
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String FEATURE = "feature"; //$NON-NLS-1$
	private static final String BUNDLE = "bundle"; //$NON-NLS-1$
	private static final String SITE = "site"; //$NON-NLS-1$
	private static final String IU = "iu"; //$NON-NLS-1$
	private static final String QUERY = "query"; //$NON-NLS-1$
	private static final String EXPRESSION = "expression"; //$NON-NLS-1$
	private static final String PARAM = "param"; //$NON-NLS-1$
	private static final String REPOSITORY_REF = "repository-reference"; //$NON-NLS-1$
	private static final String STATS_URI = "stats"; //$NON-NLS-1$

	private static final int STATE_ARCHIVE = 3;
	private static final int STATE_CATEGORY = 4;
	private static final int STATE_CATEGORY_DEF = 5;
	private static final int STATE_DESCRIPTION_CATEGORY_DEF = 7;
	private static final int STATE_DESCRIPTION_SITE = 6;
	private static final int STATE_FEATURE = 2;
	private static final int STATE_BUNDLE = 12;
	private static final int STATE_IGNORED_ELEMENT = -1;
	private static final int STATE_INITIAL = 0;
	private static final int STATE_IU = 8;
	private static final int STATE_EXPRESSION = 9;
	private static final int STATE_PARAM = 10;
	private static final int STATE_QUERY = 11;
	private static final int STATE_SITE = 1;
	private static final int STATE_REPOSITORY_REF = 13;
	private static final int STATE_STATS = 14;

	private boolean DESCRIPTION_SITE_ALREADY_SEEN = false;
	// Current object stack (used to hold the current object we are
	// populating in this plugin descriptor
	Stack<Object> objectStack = new Stack<Object>();

	private SAXParser parser;

	// Current State Information
	Stack<Integer> stateStack = new Stack<Integer>();

	// List of string keys for translated strings
	private final List<String> messageKeys = new ArrayList<String>(4);

	private MultiStatus status;

	/*
	 *
	 */
	private static void debug(String s) {
		Tracing.debug("CategoryParser: " + s); //$NON-NLS-1$
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
	public CategoryParser(URI siteLocation) {
		super();
		stateStack = new Stack<Integer>();
		objectStack = new Stack<Object>();
		status = null;
		DESCRIPTION_SITE_ALREADY_SEEN = false;
		try {
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

	public int currentState() {
		Integer state = stateStack.peek();
		if (state != null)
			return state.intValue();
		return STATE_IGNORED_ELEMENT;
	}

	/**
	 * Handle character text
	 * @see DefaultHandler#characters(char[], int, int)
	 * @since 2.0
	 */
	public void characters(char[] ch, int start, int length) {
		String text = new String(ch, start, length);
		//only push if description
		int state = currentState();
		switch (state) {
			case STATE_DESCRIPTION_SITE :
			case STATE_DESCRIPTION_CATEGORY_DEF :
				objectStack.push(text);
				break;

			case STATE_EXPRESSION :
			case STATE_PARAM :
				text = text.trim();
				String existing = null;
				if (objectStack.peek() instanceof String)
					existing = (String) objectStack.pop();
				if (existing != null)
					text = existing + text;
				objectStack.push(text);
				break;
			default :
				break; // nothing
		}
	}

	/**
	 * Handle end of element tags
	 * @see DefaultHandler#endElement(String, String, String)
	 * @since 2.0
	 */
	public void endElement(String uri, String localName, String qName) {

		String text = null;
		URLEntry info = null;

		int state = currentState();
		switch (state) {
			case STATE_IGNORED_ELEMENT :
			case STATE_ARCHIVE :
			case STATE_CATEGORY :
			case STATE_QUERY :
				stateStack.pop();
				break;

			case STATE_INITIAL :
				internalError(Messages.DefaultSiteParser_ParsingStackBackToInitialState);
				break;

			case STATE_SITE :
				stateStack.pop();
				if (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop();
					SiteModel site = (SiteModel) objectStack.peek();
					site.getDescription().setAnnotation(text);
				}
				//do not pop the object
				break;

			case STATE_FEATURE :
				stateStack.pop();
				objectStack.pop();
				break;

			case STATE_BUNDLE :
				stateStack.pop();
				objectStack.pop();
				break;

			case STATE_IU :
				stateStack.pop();
				SiteIU completeIU = (SiteIU) objectStack.pop();
				String id = completeIU.getID();
				String expression = completeIU.getQueryExpression();
				if (id == null && expression == null)
					internalError("The IU must specify an id or an expression to match against."); //$NON-NLS-1$
				break;

			case STATE_EXPRESSION :
				stateStack.pop();
				if (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop();
					SiteIU iu = (SiteIU) objectStack.peek();
					iu.setQueryExpression(text);
					if (Tracing.DEBUG_GENERATOR_PARSING)
						debug("Found Expression: " + text); //$NON-NLS-1$
				}
				break;
			case STATE_PARAM :
				stateStack.pop();
				if (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop();
					SiteIU iu = (SiteIU) objectStack.peek();
					iu.addQueryParams(text);
					if (Tracing.DEBUG_GENERATOR_PARSING)
						debug("Found Param: " + text); //$NON-NLS-1$
				}
				break;

			case STATE_CATEGORY_DEF :
				stateStack.pop();
				if (objectStack.peek() instanceof String) {
					text = (String) objectStack.pop();
					SiteCategory category = (SiteCategory) objectStack.peek();
					category.setDescription(text);
				}
				objectStack.pop();
				break;

			case STATE_REPOSITORY_REF :
				stateStack.pop();
				// do not pop object as we did not push the reference
				break;

			case STATE_STATS :
				stateStack.pop();
				// do not pop object stack because we didn't push anything
				break;

			case STATE_DESCRIPTION_SITE :
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
					debug(NLS.bind(Messages.DefaultSiteParser_ElementAlreadySet, (new String[] {getState(state)})));
				siteModel.setDescription(info);
				DESCRIPTION_SITE_ALREADY_SEEN = true;
				break;

			case STATE_DESCRIPTION_CATEGORY_DEF :
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
					internalError(NLS.bind(Messages.DefaultSiteParser_ElementAlreadySet, (new String[] {getState(state), category.getLabel()})));
				else {
					checkTranslated(info.getAnnotation());
					category.setDescription(info.getAnnotation());
				}
				break;

			default :
				internalError(NLS.bind(Messages.DefaultSiteParser_UnknownEndState, (new String[] {getState(state)})));
				break;
		}

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End Element:" + uri + ":" + localName + ":" + qName);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/*
	 * Handles an error state specified by the status.  The collection of all logged status
	 * objects can be accessed using <code>getStatus()</code>.
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
	 * @see DefaultHandler#error(SAXParseException)
	 * @since 2.0
	 */
	public void error(SAXParseException ex) {
		logStatus(ex);
	}

	/**
	 * Handle fatal errors
	 * @see DefaultHandler#fatalError(SAXParseException)
	 * @exception SAXException
	 * @since 2.0
	 */
	public void fatalError(SAXParseException ex) throws SAXException {
		logStatus(ex);
		throw ex;
	}

	/*
	 * return the state as String
	 */
	private String getState(int state) {

		switch (state) {
			case STATE_IGNORED_ELEMENT :
				return "Ignored"; //$NON-NLS-1$

			case STATE_INITIAL :
				return "Initial"; //$NON-NLS-1$

			case STATE_SITE :
				return "Site"; //$NON-NLS-1$

			case STATE_FEATURE :
				return "Feature"; //$NON-NLS-1$

			case STATE_BUNDLE :
				return "Bundle"; //$NON-NLS-1$

			case STATE_IU :
				return "IU"; //$NON-NLS-1$

			case STATE_ARCHIVE :
				return "Archive"; //$NON-NLS-1$

			case STATE_CATEGORY :
				return "Category"; //$NON-NLS-1$

			case STATE_CATEGORY_DEF :
				return "Category Def"; //$NON-NLS-1$

			case STATE_DESCRIPTION_CATEGORY_DEF :
				return "Description / Category Def"; //$NON-NLS-1$

			case STATE_DESCRIPTION_SITE :
				return "Description / Site"; //$NON-NLS-1$

			case STATE_REPOSITORY_REF :
				return "Repository Reference"; //$NON-NLS-1$

			case STATE_STATS :
				return "Stats Repository"; //$NON-NLS-1$

			default :
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
		if (elementName.equals(DESCRIPTION)) {
			stateStack.push(Integer.valueOf(STATE_DESCRIPTION_CATEGORY_DEF));
			processInfo(attributes);
		} else if (elementName.equals(CATEGORY)) {
			stateStack.push(Integer.valueOf(STATE_CATEGORY));
			processCategory(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
	}

	private void handleCategoryState(String elementName, Attributes attributes) {
		internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
	}

	private void handleFeatureState(String elementName, Attributes attributes) {
		if (elementName.equals(CATEGORY)) {
			stateStack.push(Integer.valueOf(STATE_CATEGORY));
			processCategory(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
	}

	private void handleBundleState(String elementName, Attributes attributes) {
		if (elementName.equals(CATEGORY)) {
			stateStack.push(Integer.valueOf(STATE_CATEGORY));
			processCategory(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
	}

	private void handleInitialState(String elementName, Attributes attributes) throws SAXException {
		if (elementName.equals(SITE)) {
			stateStack.push(Integer.valueOf(STATE_SITE));
			processSite(attributes);
		} else {
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
			// what we received was not a site.xml, no need to continue
			throw new SAXException(Messages.DefaultSiteParser_InvalidXMLStream);
		}

	}

	private void handleSiteState(String elementName, Attributes attributes) {
		if (elementName.equals(DESCRIPTION)) {
			stateStack.push(Integer.valueOf(STATE_DESCRIPTION_SITE));
			processInfo(attributes);
		} else if (elementName.equals(FEATURE)) {
			stateStack.push(Integer.valueOf(STATE_FEATURE));
			processFeature(attributes);
		} else if (elementName.equals(BUNDLE)) {
			stateStack.push(Integer.valueOf(STATE_BUNDLE));
			processBundle(attributes);
		} else if (elementName.equals(IU)) {
			stateStack.push(Integer.valueOf(STATE_IU));
			processIU(attributes);
		} else if (elementName.equals(ARCHIVE)) {
			stateStack.push(Integer.valueOf(STATE_ARCHIVE));
			processArchive(attributes);
		} else if (elementName.equals(CATEGORY_DEF)) {
			stateStack.push(Integer.valueOf(STATE_CATEGORY_DEF));
			processCategoryDef(attributes);
		} else if (elementName.equals(REPOSITORY_REF)) {
			stateStack.push(Integer.valueOf(STATE_REPOSITORY_REF));
			processRepositoryReference(attributes);
		} else if (elementName.equals(STATS_URI)) {
			stateStack.push(Integer.valueOf(STATE_STATS));
			processStatsInfo(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
	}

	private void handleStatsState(String elementName, Attributes attributes) {
		if (elementName.equals(FEATURE)) {
			stateStack.push(STATE_FEATURE);
			processStatsFeature(attributes);
		} else if (elementName.equals(BUNDLE)) {
			stateStack.push(STATE_BUNDLE);
			processStatsBundle(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
	}

	private void handleIUState(String elementName, Attributes attributes) {
		if (elementName.equals(QUERY)) {
			stateStack.push(Integer.valueOf(STATE_QUERY));
			processQuery(attributes);
		} else if (elementName.equals(CATEGORY)) {
			stateStack.push(Integer.valueOf(STATE_CATEGORY));
			processCategory(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
	}

	private void handleQueryState(String elementName, Attributes attributes) {
		if (elementName.equals(EXPRESSION)) {
			stateStack.push(Integer.valueOf(STATE_EXPRESSION));
			processExpression(attributes);
		} else if (elementName.equals(PARAM)) {
			stateStack.push(Integer.valueOf(STATE_PARAM));
			processParam(attributes);
		} else
			internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
	}

	private void handleExpression(String elementName, Attributes attributes) {
		internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
	}

	private void handleParamState(String elementName, Attributes attributes) {
		internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {elementName, getState(currentState())})));
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
			msg = NLS.bind(Messages.DefaultSiteParser_ErrorParsing, (new String[] {ex.getMessage()}));
		else {
			String[] values = new String[] {name, Integer.toString(ex.getLineNumber()), Integer.toString(ex.getColumnNumber()), ex.getMessage()};
			msg = NLS.bind(Messages.DefaultSiteParser_ErrorlineColumnMessage, values);
		}
		error(new Status(IStatus.ERROR, PLUGIN_ID, msg, ex));
	}

	/**
	 * Parses the specified input steam and constructs a site model.
	 * The input stream is not closed as part of this operation.
	 *
	 * @param in input stream
	 * @return site model
	 * @exception SAXException
	 * @exception IOException
	 * @since 2.0
	 */
	public SiteModel parse(InputStream in) throws SAXException, IOException {
		stateStack.push(Integer.valueOf(STATE_INITIAL));
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
		throw new SAXException(NLS.bind(Messages.DefaultSiteParser_WrongParsingStack, (new String[] {stack})));
	}

	/*
	 * process archive info
	 */
	private void processArchive(Attributes attributes) {
		// don't care about archives in category xml
		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End processing Archive"); //$NON-NLS-1$
	}

	/*
	 * process the Category  info
	 */
	private void processCategory(Attributes attributes) {
		String category = attributes.getValue("name"); //$NON-NLS-1$
		Object obj = objectStack.peek();
		// TODO could create common class/interface for adding categories
		if (obj instanceof SiteFeature) {
			((SiteFeature) obj).addCategoryName(category);
		} else if (obj instanceof SiteBundle) {
			((SiteBundle) obj).addCategoryName(category);
		} else if (obj instanceof SiteIU) {
			((SiteIU) obj).addCategoryName(category);
		} else if (obj instanceof SiteCategory) {
			((SiteCategory) obj).addCategoryName(category);
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
	 * process repository reference info
	 */
	private void processRepositoryReference(Attributes attributes) {
		String location = attributes.getValue("location"); //$NON-NLS-1$
		String nickname = attributes.getValue("name"); //$NON-NLS-1$
		URI uri;
		try {
			uri = URIUtil.fromString(location);
			boolean enabled = Boolean.parseBoolean(attributes.getValue("enabled")); //$NON-NLS-1$
			// First add a metadata repository
			RepositoryReference metadata = new RepositoryReference(uri, nickname, IRepository.TYPE_METADATA, enabled ? IRepository.ENABLED : IRepository.NONE);
			// Now a colocated artifact repository
			RepositoryReference artifact = new RepositoryReference(uri, nickname, IRepository.TYPE_ARTIFACT, enabled ? IRepository.ENABLED : IRepository.NONE);

			SiteModel site = (SiteModel) objectStack.peek();
			site.addRepositoryReference(metadata);
			site.addRepositoryReference(artifact);
			// we do not push the references onto the object stack as we do not go deeper, and
			// references are not SiteModel objects.
		} catch (URISyntaxException e) {
			// UI should have already caught this
		}
	}

	/*
	 * process stats top level element
	 */
	private void processStatsInfo(Attributes attributes) {
		String location = attributes.getValue("location"); //$NON-NLS-1$
		try {
			// One final validation but UI should have already done this.
			URIUtil.fromString(location);
			SiteModel site = (SiteModel) objectStack.peek();
			site.setStatsURIString(location);
		} catch (URISyntaxException e) {
			// Ignore if not valid.
		}

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End processing Repository Reference: location:" + location); //$NON-NLS-1$
	}

	/*
	 * process stats feature artifact
	 */
	private void processStatsFeature(Attributes attributes) {
		SiteFeature feature = new SiteFeature();

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		boolean noId = (id == null || id.trim().equals("")); //$NON-NLS-1$

		// We need to have id and version, or the url, or both.
		if (noId)
			internalError(NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] {"url", getState(currentState())}))); //$NON-NLS-1$

		feature.setFeatureIdentifier(id);
		feature.setFeatureVersion(ver);

		SiteModel site = (SiteModel) objectStack.peek();
		site.addStatsFeature(feature);
		objectStack.push(feature);
		feature.setSiteModel(site);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End Processing Stats Feature Tag: id:" + id + " version:" + ver); //$NON-NLS-1$ //$NON-NLS-2$	}
	}

	/*
	 * process stats bundle artifact info
	 */
	private void processStatsBundle(Attributes attributes) {
		SiteBundle bundle = new SiteBundle();

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		boolean noId = (id == null || id.trim().equals("")); //$NON-NLS-1$

		// We need to have id and version, or the url, or both.
		if (noId)
			internalError(NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] {"url", getState(currentState())}))); //$NON-NLS-1$

		bundle.setBundleIdentifier(id);
		bundle.setBundleVersion(ver);

		SiteModel site = (SiteModel) objectStack.peek();
		site.addStatsBundle(bundle);
		objectStack.push(bundle);
		bundle.setSiteModel(site);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End Processing Stats Bundle Tag: id:" + id + " version:" + ver); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * process feature info
	 */
	private void processFeature(Attributes attributes) {
		SiteFeature feature = new SiteFeature();

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		boolean noId = (id == null || id.trim().equals("")); //$NON-NLS-1$

		// We need to have id and version, or the url, or both.
		if (noId)
			internalError(NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] {"url", getState(currentState())}))); //$NON-NLS-1$

		feature.setFeatureIdentifier(id);
		feature.setFeatureVersion(ver);

		SiteModel site = (SiteModel) objectStack.peek();
		site.addFeature(feature);
		feature.setSiteModel(site);

		objectStack.push(feature);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End Processing Feature Tag: id:" + id + " version:" + ver); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * process bundle info
	 */
	private void processBundle(Attributes attributes) {
		SiteBundle bundle = new SiteBundle();

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String ver = attributes.getValue("version"); //$NON-NLS-1$

		boolean noId = (id == null || id.trim().equals("")); //$NON-NLS-1$

		// We need to have id and version, or the url, or both.
		if (noId)
			internalError(NLS.bind(Messages.DefaultSiteParser_Missing, (new String[] {"url", getState(currentState())}))); //$NON-NLS-1$

		bundle.setBundleIdentifier(id);
		bundle.setBundleVersion(ver);

		SiteModel site = (SiteModel) objectStack.peek();
		site.addBundle(bundle);
		bundle.setSiteModel(site);

		objectStack.push(bundle);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End Processing Bundle Tag: id:" + id + " version:" + ver); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * process IU info
	 */
	private void processIU(Attributes attributes) {
		SiteIU iu = new SiteIU();
		SiteModel site = (SiteModel) objectStack.peek();

		// identifier and version
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String range = attributes.getValue("range"); //$NON-NLS-1$
		id = id == null ? null : id.trim();
		range = range == null ? null : range.trim();

		iu.setID(id);
		iu.setRange(range);

		site.addIU(iu);
		objectStack.push(iu);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End processing iu."); //$NON-NLS-1$
	}

	/*
	 * process expression info
	 */
	private void processExpression(Attributes attributes) {
		SiteIU iu = (SiteIU) objectStack.peek();
		iu.setQueryType(attributes.getValue("type")); //$NON-NLS-1$

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End processing Expression: " + iu.getQueryType()); //$NON-NLS-1$
	}

	/*
	 * process query info
	 */
	private void processQuery(Attributes attributes) {
		// TODO may have simple attriutes for id and range
		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End processing Query."); //$NON-NLS-1$
	}

	/*
	 * process param info
	 */
	private void processParam(Attributes attributes) {
		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End processing Param."); //$NON-NLS-1$
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
		objectStack.push(site);

		if (Tracing.DEBUG_GENERATOR_PARSING)
			debug("End process Site tag."); //$NON-NLS-1$

	}

	/**
	 * Handle start of element tags
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 * @since 2.0
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		if (Tracing.DEBUG_GENERATOR_PARSING) {
			debug("State: " + currentState()); //$NON-NLS-1$
			debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		switch (currentState()) {
			case STATE_IGNORED_ELEMENT :
				internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownElement, (new String[] {localName, getState(currentState())})));
				break;
			case STATE_INITIAL :
				handleInitialState(localName, attributes);
				break;

			case STATE_SITE :
				handleSiteState(localName, attributes);
				break;

			case STATE_FEATURE :
				handleFeatureState(localName, attributes);
				break;

			case STATE_BUNDLE :
				handleBundleState(localName, attributes);
				break;

			case STATE_IU :
				handleIUState(localName, attributes);
				break;

			case STATE_EXPRESSION :
				handleExpression(localName, attributes);
				break;

			case STATE_QUERY :
				handleQueryState(localName, attributes);
				break;

			case STATE_PARAM :
				handleParamState(localName, attributes);
				break;

			case STATE_ARCHIVE :
				handleSiteState(localName, attributes);
				break;

			case STATE_CATEGORY :
				handleCategoryState(localName, attributes);
				break;

			case STATE_CATEGORY_DEF :
				handleCategoryDefState(localName, attributes);
				break;

			case STATE_DESCRIPTION_SITE :
				handleSiteState(localName, attributes);
				break;

			case STATE_DESCRIPTION_CATEGORY_DEF :
				handleSiteState(localName, attributes);
				break;

			case STATE_STATS :
				handleStatsState(localName, attributes);
				break;

			default :
				internalErrorUnknownTag(NLS.bind(Messages.DefaultSiteParser_UnknownStartState, (new String[] {getState(currentState())})));
				break;
		}

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
