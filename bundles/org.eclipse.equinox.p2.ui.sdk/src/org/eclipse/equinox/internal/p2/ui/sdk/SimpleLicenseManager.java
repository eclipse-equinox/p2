/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.p2.metadata.ILicense;
import org.eclipse.equinox.p2.common.LicenseManager;
import org.eclipse.ui.statushandlers.StatusManager;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * SimpleLicenseManager is a license manager that keeps track of 
 * IInstallableUnit licenses by using the digests of the IU's licenses.
 * It can read and write its accepted list to a stream.
 * 
 * @since 3.4
 */
public class SimpleLicenseManager extends LicenseManager {
	java.util.Set accepted = new HashSet();

	public boolean accept(ILicense license) {
		accepted.add(license.getDigest());
		return true;
	}

	public boolean reject(ILicense license) {
		accepted.remove(license.getDigest());
		return true;
	}

	public boolean isAccepted(ILicense license) {
		return accepted.contains(license.getDigest());
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
			handleException(e, ProvSDKMessages.ProvUILicenseManager_ParsingError, StatusManager.LOG);
		} catch (SAXException e) {
			handleException(e, ProvSDKMessages.ProvUILicenseManager_ParsingError, StatusManager.LOG);
		}
	}

	private void handleException(Throwable t, String message, int style) {
		if (message == null && t != null) {
			message = t.getMessage();
		}
		IStatus status = new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, 0, message, t);
		StatusManager.getManager().handle(status, style);
	}

	public void write(OutputStream stream) throws IOException {
		OutputStreamWriter osw = null;
		PrintWriter writer = null;
		try {
			osw = new OutputStreamWriter(stream, "UTF8"); //$NON-NLS-1$
			writer = new PrintWriter(osw);
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			writer.println("<licenses>"); //$NON-NLS-1$
			for (Iterator i = accepted.iterator(); i.hasNext();) {
				BigInteger digest = (BigInteger) i.next();
				writer.print("    " + "<license digest=\"" + digest.toString(16) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
			}
		} finally {
			writer.println("</licenses>"); //$NON-NLS-1$
			writer.flush();
			writer.close();
			if (osw != null)
				osw.close();
		}
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
					Node digestAtt = atts.getNamedItem("digest"); //$NON-NLS-1$
					if (digestAtt != null) {
						BigInteger digest = new BigInteger(digestAtt.getNodeValue(), 16);
						licenses.add(digest);
					}
				}
			}
		}
	}
}
