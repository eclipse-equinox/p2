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
package org.eclipse.equinox.internal.p2.persistence;

import java.util.List;
import java.util.StringTokenizer;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.MultiStatus;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public abstract class XMLParser extends DefaultHandler implements XMLConstants {

	// TODO: support logging
	// Get the logger associated with the class.
	// protected abstract Logger getLogger();

	// Get the root object that is being parsed.
	protected abstract Object getRootObject();

	// Get a generic parser error message for inclusion in an error status
	protected abstract String getErrorMessage();

	protected BundleContext context; // parser class bundle context
	protected String bundleId; // parser class bundle id

	protected XMLReader xmlReader; // the XML reader for the parser
	protected ErrorHandler errorHandler; // the error handler for the parser

	protected MultiStatus status = null; // accumulation of non-fatal errors
	protected Locator locator = null; // document locator, if supported by the parser

	private static ServiceTracker xmlTracker = null;

	public XMLParser(BundleContext context, String pluginId) {
		super();
		this.context = context;
		this.bundleId = pluginId;
	}

	/**
	 *  Non-fatal errors accumulated during parsing.
	 */
	public IStatus getStatus() {
		return (status != null ? status : Status.OK_STATUS);
	}

	public boolean isValidXML() {
		return (status == null || !status.isErrorOrCancel());
	}

	private static SAXParserFactory acquireXMLParsing(BundleContext context) {
		if (xmlTracker == null) {
			xmlTracker = new ServiceTracker(context, SAXParserFactory.class.getName(), null);
			xmlTracker.open();
		}
		return (SAXParserFactory) xmlTracker.getService();
	}

	protected static void releaseXMLParsing() {
		if (xmlTracker != null) {
			xmlTracker.close();
		}
	}

	protected SAXParser getParser() throws ParserConfigurationException, SAXException {
		SAXParserFactory factory = acquireXMLParsing(this.context);
		if (factory == null) {
			throw new SAXException(Messages.XMLParser_No_SAX_Parser);
		}
		factory.setNamespaceAware(true);
		factory.setValidating(false);
		try {
			factory.setFeature("http://xml.org/sax/features/string-interning", true); //$NON-NLS-1$
		} catch (SAXException se) {
			// some parsers may not support string interning
		}
		SAXParser theParser = factory.newSAXParser();
		if (theParser == null) {
			throw new SAXException(Messages.XMLParser_No_SAX_Parser);
		}
		xmlReader = theParser.getXMLReader();
		return theParser;
	}

	public static String makeSimpleName(String localName, String qualifiedName) {
		if (localName != null && localName.length() > 0) {
			return localName;
		}
		int nameSpaceIndex = qualifiedName.indexOf(":"); //$NON-NLS-1$
		return (nameSpaceIndex == -1 ? qualifiedName : qualifiedName.substring(nameSpaceIndex + 1));
	}

	/**
	 * Set the document locator for the parser
	 * 
	 * @see org.xml.sax.ContentHandler#setDocumentLocator
	 */
	public void setDocumentLocator(Locator docLocator) {
		locator = docLocator;
	}

	/**
	 * Abstract base class for content handlers
	 */
	protected abstract class AbstractHandler extends DefaultHandler {

		protected ContentHandler parentHandler = null;
		protected String elementHandled = null;

		protected StringBuffer characters = null; // character data inside an element

		public AbstractHandler() {
			// Empty constructor for a root handler
		}

		public AbstractHandler(ContentHandler parentHandler) {
			this.parentHandler = parentHandler;
			xmlReader.setContentHandler(this);
		}

		public AbstractHandler(ContentHandler parentHandler, String elementHandled) {
			this.parentHandler = parentHandler;
			xmlReader.setContentHandler(this);
			this.elementHandled = elementHandled;
		}

		/**
		 * Set the document locator for the parser
		 * 
		 * @see org.xml.sax.ContentHandler#setDocumentLocator
		 */
		public void setDocumentLocator(Locator docLocator) {
			locator = docLocator;
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			finishCharacters();
			String name = makeSimpleName(localName, qName);
			trace(name, attributes);
			startElement(name, attributes);
		}

		public abstract void startElement(String name, Attributes attributes) throws SAXException;

		public void invalidElement(String name, Attributes attributes) {
			unexpectedElement(this, name, attributes);
			new IgnoringHandler(this);
		}

		public void endElement(String namespaceURI, String localName, String qName) {
			// TODO: throw a bad state error if makeSimpleName(localName, qName) != elementHandled
			finishCharacters();
			finished();
			// Restore the parent content handler
			xmlReader.setContentHandler(parentHandler);
		}

		/**
		 * 	An implementation for startElement when there are no sub-elements
		 */
		protected void noSubElements(String name, Attributes attributes) {
			unexpectedElement(this, name, attributes);
			// Create a new handler to ignore subsequent nested elements			
			new IgnoringHandler(this);
		}

		/*
		 * Save up character data until endElement or nested startElement
		 * 
		 * @see org.xml.sax.ContentHandler#characters
		 */
		public void characters(char[] chars, int start, int length) {
			if (this.characters == null) {
				this.characters = new StringBuffer();
			}
			this.characters.append(chars, start, length);
		}

		// Consume the characters accumulated in this.characters.
		// Called before startElement or endElement
		private String finishCharacters() {
			// common case -- no characters or only whitespace
			if (this.characters == null || this.characters.length() == 0) {
				return null;
			}
			if (allWhiteSpace(this.characters)) {
				this.characters.setLength(0);
				return null;
			}

			// process the characters
			try {
				String trimmedChars = this.characters.toString().trim();
				if (trimmedChars.length() == 0) {
					// this shouldn't happen due to the test for allWhiteSpace above
					System.err.println("Unexpected non-whitespace characters: " //$NON-NLS-1$
							+ trimmedChars);
					return null;
				}
				processCharacters(trimmedChars);
				return trimmedChars;
			} finally {
				this.characters.setLength(0);
			}
		}

		// Method to override in the handler of an element with CDATA. 
		protected void processCharacters(String data) {
			if (data.length() > 0) {
				unexpectedCharacterData(this, data);
			}
		}

		private boolean allWhiteSpace(StringBuffer sb) {
			int length = sb.length();
			for (int i = 0; i < length; i += 1) {
				if (!Character.isWhitespace(sb.charAt(i))) {
					return false;
				}
			}
			return true;
		}

		/**
		 * 	Called when this element and all elements nested into it have been
		 * 	handled.
		 */
		protected void finished() {
			// Do nothing by default
		}

		/*
		 * 	A name used to identify the handler.
		 */
		public String getName() {
			return (elementHandled != null ? elementHandled : "NoName"); //$NON-NLS-1$
		}

		/**
		 * Parse the attributes of an element with a single required attribute.
		 */
		protected String parseRequiredAttribute(Attributes attributes, String name) {
			return parseRequiredAttributes(attributes, new String[] {name})[0];
		}

		/**
		 * Parse the attributes of an element with two required attributes.
		 */
		protected String[] parseRequiredAttributes(Attributes attributes, String name1, String name2) {
			return parseRequiredAttributes(attributes, new String[] {name1, name2});
		}

		/**
		 * Parse the attributes of an element with only required attributes.
		 */
		protected String[] parseRequiredAttributes(Attributes attributes, String[] required) {
			return parseAttributes(attributes, required, noAttributes);
		}

		/**
		 * Parse the attributes of an element with a single optional attribute.
		 */
		protected String parseOptionalAttribute(Attributes attributes, String name) {
			return parseAttributes(attributes, noAttributes, new String[] {name})[0];
		}

		/**
		 * Parse the attributes of an element, given the list of required and optional ones.
		 * Return values in same order, null for those not present.
		 * Report errors for missing required attributes or extra ones.
		 */
		protected String[] parseAttributes(Attributes attributes, String[] required, String[] optional) {
			String[] result = new String[required.length + optional.length];
			for (int i = 0; i < attributes.getLength(); i += 1) {
				String name = attributes.getLocalName(i);
				String value = attributes.getValue(i).trim();
				int j;
				if ((j = indexOf(required, name)) >= 0) {
					result[j] = value;
				} else if ((j = indexOf(optional, name)) >= 0) {
					result[required.length + j] = value;
				} else {
					unexpectedAttribute(elementHandled, name, value);
				}
			}
			for (int i = 0; i < required.length; i += 1) {
				checkRequiredAttribute(elementHandled, required[i], result[i]);
			}
			return result;
		}

	}

	/**
	 *	Handler for an XML document.
	 *
	 *	Using the inelegant name 'DocHandler' to clearly distinguish
	 *	this class from the deprecated org.xml.sax.DocumentHandler.
	 */
	protected class DocHandler extends AbstractHandler {

		RootHandler rootHandler;

		public DocHandler(String rootName, RootHandler rootHandler) {
			super(null, rootName);
			this.rootHandler = rootHandler;
		}

		public void ProcessingInstruction(String target, String data) throws SAXException {
			// Do nothing by default (except suppress warning)
			if (false) {
				throw new SAXException(""); //$NON-NLS-1$
			}
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equalsIgnoreCase(elementHandled)) {
				rootHandler.initialize(this, name, attributes);
				xmlReader.setContentHandler(rootHandler);
			} else {
				this.noSubElements(name, attributes);
			}
		}

	}

	/**
	 * 	Abstract handler for the root element.
	 */
	protected abstract class RootHandler extends AbstractHandler {

		public RootHandler() {
			super();
		}

		public void initialize(DocHandler document, String rootName, Attributes attributes) {
			this.parentHandler = document;
			this.elementHandled = rootName;
			handleRootAttributes(attributes);
		}

		protected abstract void handleRootAttributes(Attributes attributes);

	}

	/**
	 * 	Handler for an ordered properties collection.
	 */
	protected class PropertiesHandler extends AbstractHandler {

		private OrderedProperties properties;

		public PropertiesHandler(ContentHandler parentHandler, Attributes attributes) {
			super(parentHandler, PROPERTIES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			properties = (size != null ? new OrderedProperties(new Integer(size).intValue()) : new OrderedProperties());
		}

		public OrderedProperties getProperties() {
			return properties;
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equalsIgnoreCase(PROPERTY_ELEMENT)) {
				new PropertyHandler(this, attributes, properties);
			} else {
				invalidElement(name, attributes);
			}
		}

	}

	/**
	 * 	Handler for a property in an ordered properties collection.
	 */
	protected class PropertyHandler extends AbstractHandler {

		public PropertyHandler(ContentHandler parentHandler, Attributes attributes, OrderedProperties properties) {
			super(parentHandler, PROPERTY_ELEMENT);
			String[] property = parseProperty(attributes);
			if (isValidProperty(property)) {
				properties.setProperty(property[0], property[1]);
			}
		}

		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}

		private String[] parseProperty(Attributes attributes) {
			return parseRequiredAttributes(attributes, PROPERTY_NAME_ATTRIBUTE, PROPERTY_VALUE_ATTRIBUTE);
		}

		private boolean isValidProperty(String[] property) {
			return (property.length == 2 && property[0] != null && property[1] != null);
		}
	}

	/**
	 * 	Handler for an element with only cdata and no sub-elements.
	 */
	protected class TextHandler extends AbstractHandler {

		private String text = null;

		private List texts = null;

		// Constructor for a subclass that processes the attributes
		public TextHandler(AbstractHandler parent, String elementName) {
			super(parent, elementName);
		}

		// Constructor for a subclass with no attributes
		public TextHandler(AbstractHandler parent, String elementName, Attributes attributes) {
			super(parent, elementName);
			parseAttributes(attributes, noAttributes, noAttributes);
		}

		public TextHandler(AbstractHandler parent, String elementName, Attributes attributes, List texts) {
			super(parent, elementName);
			parseAttributes(attributes, noAttributes, noAttributes);
			this.texts = texts;
		}

		public String getText() {
			return (text != null ? text : ""); //$NON-NLS-1$
		}

		public void startElement(String name, Attributes attributes) {
			invalidElement(name, attributes);
		}

		protected void processCharacters(String data) {
			this.text = data;
			if (texts != null) {
				texts.add(getText());
			}
		}

	}

	/**
	 * 	Handler for ignoring content.
	 */
	protected class IgnoringHandler extends AbstractHandler {

		public IgnoringHandler(AbstractHandler parent) {
			super(parent);
			this.elementHandled = "IgnoringAll"; //$NON-NLS-1$
		}

		public void startElement(String name, Attributes attributes) {
			noSubElements(name, attributes);
		}

	}

	// Helpers for processing instructions that include a Class and/or a Version.

	public String extractPIClass(String data) {
		return extractPIAttribute(data, PI_CLASS_ATTRIBUTE);
	}

	public Version extractPIVersion(String target, String data) {
		return checkVersion(target, PI_VERSION_ATTRIBUTE, extractPIAttribute(data, PI_VERSION_ATTRIBUTE));
	}

	private String extractPIAttribute(String data, String key) {
		StringTokenizer piTokenizer = new StringTokenizer(data, " \'\""); //$NON-NLS-1$
		String[] tokens = new String[piTokenizer.countTokens()];
		int index = 0;
		int valueIndex = -1;
		while (piTokenizer.hasMoreTokens() && index < tokens.length) {
			tokens[index] = piTokenizer.nextToken();
			if (tokens[index].equals(key + '=') && index < tokens.length) {
				valueIndex = index + 1;
			}
			index++;
		}
		return (valueIndex >= 0 ? tokens[valueIndex] : ""); //$NON-NLS-1$
	}

	public class ParserStatus extends Status implements IStatus {

		int lineNumber = 0;
		int columnNumber = 0;

		public ParserStatus(int severity, int code, String message) {
			super(severity, MultiStatus.bundleId, code, message, null);
		}

		public ParserStatus(int severity, int code, String message, int lineNumber, int columnNumber) {
			super(severity, MultiStatus.bundleId, code, message, null);
			setLineNumber(lineNumber);
			setColumnNumber(columnNumber);
		}

		public int getColumnNumber() {
			return columnNumber;
		}

		public void setColumnNumber(int columnNumber) {
			this.columnNumber = columnNumber;
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public void setLineNumber(int lineNumber) {
			this.lineNumber = lineNumber;
		}

	}

	public void error(SAXParseException ex) {
		addError(ex);
	}

	public void fatalError(SAXParseException ex) throws SAXException {
		addError(ex);
		throw ex;
	}

	protected void addError(SAXParseException ex) {
		addError(ex.getMessage());
	}

	protected String getErrorPrefix() {
		return null;
	}

	protected String getErrorSuffix() {
		return null;
	}

	// Log an error message and add it to the current status.
	//
	// TODO: should we have a flag to throw an exception on error,
	//		 so parsing can be aborted?
	// TODO: flag to produce warnings instead of errors? etc. 
	public final void addError(String msg) {
		int line = 0;
		int column = 0;
		String key = msg;
		Object[] args = new Object[] {};
		String root = (getRootObject() == null ? "" //$NON-NLS-1$
				: " (" + getRootObject() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.locator != null) {
			String name = this.locator.getSystemId();
			line = this.locator.getLineNumber();
			column = this.locator.getColumnNumber();
			if (line > 0) {
				args = new Object[] {msg, root, name, new Integer(line), new Integer(column)};
				if (column > 0) {
					key = (name != null ? Messages.XMLParser_Error_At_Name_Line_Column //
							: Messages.XMLParser_Error_At_Line_Column);
				} else {
					key = (name != null ? Messages.XMLParser_Error_At_Name_Line //
							: Messages.XMLParser_Error_At_Line);
				}
			}
		}
		String errMsg = NLS.bind(key, args);
		String prefix = getErrorPrefix();
		String suffix = getErrorSuffix();
		if (prefix != null) {
			errMsg = prefix + errMsg;
		}
		if (suffix != null) {
			errMsg = errMsg + suffix;
		}
		// TODO: support logging
		// getLogger().warning(errMsg);
		IStatus currStatus = new ParserStatus(IStatus.ERROR, IStatus.OK, errMsg, line, column);
		if (this.status == null) {
			this.status = new MultiStatus(bundleId, IStatus.OK, new IStatus[] {currStatus}, getErrorMessage(), null);
		} else {
			this.status.add(currStatus);
		}
	}

	public void trace(String element, Attributes attributes) {
		// TODO: support logging
		//		if (!getLogger().isDebugLoggable()) {
		//			return;
		//		}
		//		int indentSize = (this.stateStack != null ? this.stateStack.size() - 1 : 1);
		//		if (attributes == null) {
		//			indentSize -= 1;
		//		}
		//		char[] indent = new char[2 * indentSize];
		//		Arrays.fill(indent, ' ');
		//		StringBuffer sb = new StringBuffer();
		//		sb.append(indent);
		//		sb.append('<');
		//		if (attributes != null) {
		//			sb.append(element);
		//			toString(sb, attributes);
		//		} else {
		//			sb.append('/').append(element);
		//		}
		//		sb.append('>');
		//		getLogger().debug(sb.toString());
	}

	private static String toString(Attributes attributes) {
		StringBuffer result = new StringBuffer();
		toString(result, attributes);
		return result.toString();
	}

	private static void toString(StringBuffer sb, Attributes attributes) {
		for (int i = 0; i < attributes.getLength(); i += 1) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i).trim();
			sb.append(' ').append(name);
			sb.append('=').append('"');
			sb.append(value);
			sb.append('"');
		}
	}

	public void error(String msg) {
		addError(msg);
	}

	public void checkRequiredAttribute(String element, String name, Object value) {
		if (value == null) {
			addError(NLS.bind(Messages.XMLParser_Missing_Required_Attribute, element, name));
		}
	}

	// Check the format of a required boolean attribute
	public Boolean checkBoolean(String element, String attribute, String value) {
		try {
			return Boolean.valueOf(value);
		} catch (IllegalArgumentException iae) {
			invalidAttributeValue(element, attribute, value);
		} catch (NullPointerException npe) {
			invalidAttributeValue(element, attribute, null);
		}
		return Boolean.FALSE;
	}

	// Check the format of an optional boolean attribute
	public Boolean checkBoolean(String element, String attribute, String value, boolean defaultValue) {
		Boolean result = (defaultValue ? Boolean.TRUE : Boolean.FALSE);
		if (value != null) {
			try {
				return Boolean.valueOf(value);
			} catch (IllegalArgumentException iae) {
				invalidAttributeValue(element, attribute, value);
			}
		}
		return result;
	}

	public Version checkVersion(String element, String attribute, String value) {
		try {
			return new Version(value);
		} catch (IllegalArgumentException iae) {
			invalidAttributeValue(element, attribute, value);
		} catch (NullPointerException npe) {
			invalidAttributeValue(element, attribute, null);
		}
		return Version.emptyVersion;
	}

	public VersionRange checkVersionRange(String element, String attribute, String value) {
		try {
			return new VersionRange(value);
		} catch (IllegalArgumentException iae) {
			invalidAttributeValue(element, attribute, value);
		} catch (NullPointerException npe) {
			invalidAttributeValue(element, attribute, null);
		}
		return VersionRange.emptyRange;
	}

	public void unexpectedAttribute(String element, String attribute, String value) {
		addError(NLS.bind(Messages.XMLParser_Unexpected_Attribute, new Object[] {element, attribute, value}));
	}

	public void invalidAttributeValue(String element, String attribute, String value) {
		addError(NLS.bind(Messages.XMLParser_Illegal_Value_For_Attribute, new Object[] {attribute, element, value}));
	}

	public void unexpectedElement(AbstractHandler handler, String element, Attributes attributes) {
		addError(NLS.bind(Messages.XMLParser_Unexpected_Element, new Object[] {handler.getName(), element, toString(attributes)}));
	}

	public void duplicateElement(AbstractHandler handler, String element, Attributes attributes) {
		addError(NLS.bind(Messages.XMLParser_Duplicate_Element, new Object[] {handler.getName(), element, toString(attributes)}));
	}

	public void unexpectedCharacterData(AbstractHandler handler, String cdata) {
		addError(NLS.bind(Messages.XMLParser_Unexpected_Character_Data, handler.getName(), cdata.trim()));
	}

	/**
	 * Find the index of the first occurrence of object in array, or -1.
	 * Use Arrays.binarySearch if array is big and sorted.
	 */
	protected static int indexOf(String[] array, String value) {
		for (int i = 0; i < array.length; i += 1) {
			if (value == null ? array[i] == null : value.equals(array[i])) {
				return i;
			}
		}
		return -1;
	}

	//	public class BadStateError extends AssertionError {
	//		private static final long serialVersionUID = 1L; // not serialized
	//
	//		public BadStateError() {
	//			super("unexpected state" + //$NON-NLS-1$
	//					(XMLParser.this.stateStack != null ? ": " + XMLParser.this.stateStack //$NON-NLS-1$
	//					: "")); //$NON-NLS-1$
	//		}
	//
	//		public BadStateError(String element) {
	//			super("unexpected state for " + element + //$NON-NLS-1$
	//					(XMLParser.this.stateStack != null ? ": " + XMLParser.this.stateStack //$NON-NLS-1$
	//					: "")); //$NON-NLS-1$
	//		}
	//	}

}