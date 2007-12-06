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
package org.eclipse.equinox.p2.ui;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * SimpleLicenseManager is a license manager that keeps track of 
 * IInstallableUnit licenses by using the IU's license id property.
 * It can read and write its accepted list to a stream.
 * 
 * @since 3.4
 */
public class SimpleLicenseManager extends LicenseManager {
	java.util.Set accepted = new HashSet();

	public boolean acceptLicense(IInstallableUnit iu) {
		String id = getLicenseId(iu);
		if (id != null) {
			accepted.add(id);
			return true;
		}
		return false;
	}

	public boolean rejectLicense(IInstallableUnit iu) {
		String id = getLicenseId(iu);
		if (id != null)
			return accepted.remove(id);
		return false;
	}

	public boolean isAccepted(IInstallableUnit iu) {
		String id = getLicenseId(iu);
		if (id != null)
			return accepted.contains(id);
		return false;
	}

	public boolean hasAcceptedLicenses() {
		return !accepted.isEmpty();
	}

	public void read(InputStream stream) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder parser = factory.newDocumentBuilder();
			Document doc = parser.parse(stream);
			Node root = doc.getDocumentElement();
			processRoot(root, accepted);
		} catch (ParserConfigurationException e) {
			ProvUI.handleException(e, ProvUIMessages.ProvUILicenseManager_ParsingError);
		} catch (SAXException e) {
			ProvUI.handleException(e, ProvUIMessages.ProvUILicenseManager_ParsingError);
		}
	}

	public void write(OutputStream stream) throws IOException {
		OutputStreamWriter osw = null;
		PrintWriter writer = null;
		try {
			osw = new OutputStreamWriter(stream, "UTF8"); //$NON-NLS-1$
			writer = new PrintWriter(osw);
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			writer.println("<licenses>"); //$NON-NLS-1$
			String[] licenses = (String[]) accepted.toArray(new String[accepted.size()]);
			for (int i = 0; i < accepted.size(); i++) {
				writer.print("    " + "<license id=\"" + licenses[i] + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		} finally {
			writer.println("</licenses>"); //$NON-NLS-1$
			writer.flush();
			writer.close();
			if (osw != null)
				osw.close();
		}
	}

	private String getLicenseId(IInstallableUnit iu) {
		return iu.getProperty(IInstallableUnit.PROP_LICENSE_ID);
	}

	private void processRoot(Node root, Set licenses) {
		if (root.getNodeName().equals("licenses")) { //$NON-NLS-1$
			NodeList children = root.getChildNodes();
			processChildren(children, licenses);
		}
	}

	private void processChildren(NodeList children, Set licenses) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("license")) { //$NON-NLS-1$
					NamedNodeMap atts = child.getAttributes();
					Node att = atts.getNamedItem("id"); //$NON-NLS-1$
					if (att != null)
						licenses.add(att.getNodeValue());
				}
			}
		}
	}
}
