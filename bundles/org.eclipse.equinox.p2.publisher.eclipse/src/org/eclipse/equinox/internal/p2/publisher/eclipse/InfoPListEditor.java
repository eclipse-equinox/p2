/*******************************************************************************
 *  Copyright (c) 2015 Rapicorp, Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 * 	Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class InfoPListEditor {
	public static final String MARKER_KEY = "CFBundleExecutable"; //$NON-NLS-1$
	public static final String BUNDLE_KEY = "CFBundleName"; //$NON-NLS-1$
	public static final String BUNDLE_ID_KEY = "CFBundleIdentifier"; //$NON-NLS-1$
	public static final String BUNDLE_DISPLAYNAME_KEY = "CFBundleDisplayName"; //$NON-NLS-1$
	public static final String BUNDLE_INFO_KEY = "CFBundleGetInfoString"; //$NON-NLS-1$
	public static final String BUNDLE_VERSION_KEY = "CFBundleVersion"; //$NON-NLS-1$
	public static final String BUNDLE_SHORT_VERSION_KEY = "CFBundleShortVersionString"; //$NON-NLS-1$
	public static final String ICON_KEY = "CFBundleIconFile"; //$NON-NLS-1$

	private final Element infoPList;
	private XPath xPathTool;
	private final Document document;

	private InfoPListEditor(Document doc) {
		document = doc;
		infoPList = document.getDocumentElement();
	}

	public static InfoPListEditor loadPListEditor(File file) throws IOException {
		if (!file.exists()) {
			throw new IOException("No file at " + file.getAbsoluteFile()); //$NON-NLS-1$
		}
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Exception exception;
		try {
			factory.setValidating(false);
			factory.setNamespaceAware(true);
			//These setFeature calls are necessary to disable the validation of the DTD in the info.plist
			factory.setFeature("http://xml.org/sax/features/namespaces", false); //$NON-NLS-1$
			factory.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false); //$NON-NLS-1$
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
			builder = factory.newDocumentBuilder();
			return new InfoPListEditor(builder.parse(file));
		} catch (ParserConfigurationException e) {
			exception = e;
		} catch (SAXException e) {
			exception = e;
		} catch (IOException e) {
			exception = e;
		}
		throw new IOException("Problem parsing " + file.getAbsolutePath(), exception); //$NON-NLS-1$
	}

	public void save(File file) throws TransformerException {
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		final DOMSource toSerialize = new DOMSource(document);

		final StreamResult output = new StreamResult(file);
		transformer.setOutputProperty(OutputKeys.VERSION, "1.0"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.STANDALONE, "yes"); //$NON-NLS-1$

		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); //$NON-NLS-1$//$NON-NLS-2$

		transformer.transform(toSerialize, output);
	}

	private Node getNode(Node node, String expression) throws XPathExpressionException {
		return (Node) getXPathTool().evaluate(expression, node, XPathConstants.NODE);
	}

	public void setKey(String key, String value) {
		if (key == null)
			throw new IllegalArgumentException("Key can't be null"); //$NON-NLS-1$
		if (value == null)
			throw new IllegalArgumentException("Value can't be null"); //$NON-NLS-1$

		String expression = String.format("/plist/dict/key[text() = '%s']/following-sibling::string[1]", key); //$NON-NLS-1$
		Node node;
		try {
			node = getNode(infoPList, expression);
			if (node != null)
				node.getFirstChild().setNodeValue(value);
			else
				addKey(key, value);
		} catch (XPathExpressionException e) {
			//Can't happen since we craft the expression carefully
		}
	}

	private void addKey(String key, String value) throws DOMException, XPathExpressionException {
		Element keyNode = document.createElement("key"); //$NON-NLS-1$
		Text keyName = document.createTextNode(key);
		keyNode.appendChild(keyName);

		Element stringNode = document.createElement("string"); //$NON-NLS-1$
		Text stringValue = document.createTextNode(value);
		stringNode.appendChild(stringValue);
		getNode(infoPList, "/plist/dict").appendChild(keyNode); //$NON-NLS-1$
		getNode(infoPList, "/plist/dict").appendChild(stringNode); //$NON-NLS-1$
	}

	private XPath getXPathTool() {
		if (xPathTool == null) {
			xPathTool = XPathFactory.newInstance().newXPath();
		}
		return xPathTool;
	}

	public void setEclipseArgument(String key, String value) {
		if (key == null)
			throw new IllegalArgumentException("Key can't be null"); //$NON-NLS-1$

		String expression = "/plist/dict/key[text() = 'Eclipse']/following-sibling::array[1]/string[text() = -keyring'"; //$NON-NLS-1$
		Node node;
		try {
			node = getNode(infoPList, expression);
			if (node != null)
				node.getFirstChild().setNodeValue(value);
			else
				addKey(key, value);
		} catch (XPathExpressionException e) {
			//Can't happen since we craft the expression carefully
		}

	}

	private List<String> getValues(Object startingPoint, String expression) throws XPathExpressionException {
		NodeList nodeList = (NodeList) getXPathTool().evaluate(expression, startingPoint, XPathConstants.NODESET);

		List<String> result = new ArrayList<String>(nodeList.getLength());
		for (int ix = 0; ix < nodeList.getLength(); ++ix) {
			result.add(nodeList.item(ix).getNodeValue());
		}
		return result;
	}

	private List<Node> removeNodes(Object startingPoint, String expression) throws XPathExpressionException {
		NodeList nodeList = (NodeList) getXPathTool().evaluate(expression, startingPoint, XPathConstants.NODESET);

		List<Node> result = new ArrayList<Node>(nodeList.getLength());
		for (int ix = 0; ix < nodeList.getLength(); ++ix) {
			result.add(nodeList.item(ix).getParentNode().removeChild(nodeList.item(ix)));
		}
		return result;
	}

	public List<String> getEclipseArguments() {
		try {
			return getValues(infoPList, "/plist/dict/key[text() = 'Eclipse']/following-sibling::array[1]/string/text()"); //$NON-NLS-1$
		} catch (XPathExpressionException e) {
			//Can't happen the expression is carefully crafted
			return null;
		}
	}

	public void setEclipseArguments(List<String> arguments) {
		try {
			removeNodes(infoPList, "/plist/dict/key[text() = 'Eclipse']/following-sibling::array[1]/string"); //$NON-NLS-1$
			for (String arg : arguments) {
				Element stringNode = document.createElement("string"); //$NON-NLS-1$
				Text stringName = document.createTextNode(arg);
				stringNode.appendChild(stringName);
				Node toAppendTo = getNode(infoPList, "/plist/dict/key[text() = 'Eclipse']/following-sibling::array[1]"); //$NON-NLS-1$
				toAppendTo.appendChild(stringNode);
			}
		} catch (XPathExpressionException e) {
			//can't happen
		}
	}
}