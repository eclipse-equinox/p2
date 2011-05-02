/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Vector;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Utility methods involving compatibility with the Eclipse Update Manager.
 * 
 * @since 3.4
 *
 */
public class UpdateManagerCompatibility {

	// This value was copied from MetadataGeneratorHelper.  Must be the same.
	private static final String ECLIPSE_INSTALL_HANDLER_PROP = "org.eclipse.update.installHandler"; //$NON-NLS-1$
	private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

	private static void parse(String fileName, Vector<MetadataRepositoryElement> bookmarks) {
		File file = new File(fileName);
		if (!file.exists())
			return;

		try {
			documentBuilderFactory.setNamespaceAware(true);
			DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();
			Document doc = parser.parse(file);
			Node root = doc.getDocumentElement();
			processRoot(root, bookmarks);
		} catch (ParserConfigurationException e) {
			logFail(e);
		} catch (SAXException e) {
			logFail(e);
		} catch (IOException e) {
			logFail(e);
		}
	}

	private static MetadataRepositoryElement[] getSites(Vector<MetadataRepositoryElement> bookmarks) {
		return bookmarks.toArray(new MetadataRepositoryElement[bookmarks.size()]);
	}

	private static void processRoot(Node root, Vector<MetadataRepositoryElement> bookmarks) {
		if (root.getNodeName().equals("bookmarks")) { //$NON-NLS-1$
			NodeList children = root.getChildNodes();
			processChildren(children, bookmarks);
		}
	}

	private static void processChildren(NodeList children, Vector<MetadataRepositoryElement> bookmarks) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("site")) { //$NON-NLS-1$
					createSite(child, bookmarks);
				} else if (child.getNodeName().equals("folder")) { //$NON-NLS-1$
					createFolder(child, bookmarks);
				}
			}
		}
	}

	private static void createSite(Node child, Vector<MetadataRepositoryElement> bookmarks) {
		URI uri = null;
		try {
			uri = URIUtil.fromString((getAttribute(child, "url"))); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			logFail(e);
			return;
		}

		String sel = getAttribute(child, "selected"); //$NON-NLS-1$
		boolean selected = (sel != null && sel.equals("true")); //$NON-NLS-1$

		MetadataRepositoryElement element = new MetadataRepositoryElement(null, uri, selected);
		String nickname = getAttribute(child, "name"); //$NON-NLS-1$
		if (nickname != null && nickname.length() > 0)
			element.setNickname(nickname);
		bookmarks.add(element);
	}

	private static void createFolder(Node child, Vector<MetadataRepositoryElement> bookmarks) {
		if (child.hasChildNodes())
			processChildren(child.getChildNodes(), bookmarks);
	}

	private static String getAttribute(Node node, String name) {
		NamedNodeMap atts = node.getAttributes();
		Node att = atts.getNamedItem(name);
		if (att != null) {
			return att.getNodeValue();
		}
		return ""; //$NON-NLS-1$
	}

	private static void store(String fileName, Vector<MetadataRepositoryElement> bookmarks) {
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		PrintWriter writer = null;
		try {
			fos = new FileOutputStream(fileName);
			osw = new OutputStreamWriter(fos, "UTF8"); //$NON-NLS-1$
			writer = new PrintWriter(osw);
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			writer.println("<bookmarks>"); //$NON-NLS-1$
			for (int i = 0; i < bookmarks.size(); i++) {
				Object obj = bookmarks.get(i);
				writeObject("   ", obj, writer); //$NON-NLS-1$
			}
		} catch (IOException e) {
			logFail(e);
		} finally {
			if (writer != null) {
				writer.println("</bookmarks>"); //$NON-NLS-1$
				writer.flush();
				writer.close();
			}
			try {
				if (osw != null)
					osw.close();
			} catch (IOException e1) {
				logFail(e1);
			}
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e2) {
				logFail(e2);
			}
		}
	}

	private static void writeObject(String indent, Object obj, PrintWriter writer) {
		if (obj instanceof MetadataRepositoryElement) {
			MetadataRepositoryElement element = (MetadataRepositoryElement) obj;
			String sel = element.isEnabled() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
			String name = element.getName();
			writer.print(indent + "<site url=\"" + URIUtil.toUnencodedString(element.getLocation()) + "\" selected=\"" + sel + "\" name=\"" + getWritableXMLString(name) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			writer.println("/>"); //$NON-NLS-1$
		}
	}

	public static IStatus getInstallHandlerStatus(IProvisioningPlan plan) {
		IQueryResult<IInstallableUnit> result = plan.getAdditions().query(QueryUtil.createIUAnyQuery(), null);
		for (Iterator<IInstallableUnit> iterator = result.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			if (iu != null && iu.getProperty(ECLIPSE_INSTALL_HANDLER_PROP) != null)
				return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, NLS.bind(ProvUIMessages.UpdateManagerCompatibility_ItemRequiresUpdateManager, iu.getId()));
		}
		return Status.OK_STATUS;
	}

	/**
	 * Prompt the user for a file and import the sites specified in that
	 * file.  Return the collection of repo elements in the import.
	 * @param shell the shell used to parent any dialogs used.
	 */
	public static MetadataRepositoryElement[] importSites(Shell shell) {
		FileDialog dialog = new FileDialog(shell);
		dialog.setText(ProvUIMessages.UpdateManagerCompatibility_ImportSitesTitle);
		dialog.setFilterExtensions(new String[] {"*.xml", "*"}); //$NON-NLS-1$ //$NON-NLS-2$

		MetadataRepositoryElement[] sites = null;

		String bookmarksFile = dialog.open();
		while (bookmarksFile != null && sites == null) {
			File file = new File(bookmarksFile);
			sites = readBookmarkFile(file);
			if (sites == null || sites.length == 0) {
				MessageDialog.openInformation(shell, ProvUIMessages.UpdateManagerCompatibility_InvalidSitesTitle, ProvUIMessages.UpdateManagerCompatibility_InvalidSiteFileMessage);
				bookmarksFile = dialog.open();
			}
		}
		return sites == null ? new MetadataRepositoryElement[0] : sites;
	}

	public static MetadataRepositoryElement[] readBookmarkFile(File file) {
		Vector<MetadataRepositoryElement> bookmarks = new Vector<MetadataRepositoryElement>();
		parse(file.getAbsolutePath(), bookmarks);
		return getSites(bookmarks);
	}

	public static void writeBookmarkFile(String filename, MetadataRepositoryElement[] sites) {
		Vector<MetadataRepositoryElement> bookmarks = new Vector<MetadataRepositoryElement>(sites.length);
		for (int i = 0; i < sites.length; i++)
			bookmarks.add(sites[i]);
		store(filename, bookmarks);

	}

	/**
	 * Export the specified list of sites to a bookmarks file that
	 * can be read later.
	 * 
	 * @param shell the shell used to parent the export dialog
	 * @param sites the sites to export
	 */

	public static void exportSites(Shell shell, MetadataRepositoryElement[] sites) {
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setText(ProvUIMessages.UpdateManagerCompatibility_ExportSitesTitle);
		dialog.setFileName("bookmarks.xml"); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] {"*.xml", "*"}); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setOverwrite(true);

		String bookmarksFile = dialog.open();
		if (bookmarksFile == null)
			return;

		writeBookmarkFile(bookmarksFile, sites);
	}

	/**
	 * Open the old UpdateManager installer UI using the specified shell. 
	 * We do not call the UpdateManagerUI class directly because we want to be able to be configured 
	 * without requiring those plug-ins.  Instead, we invoke a known command.
	 */
	public static void openInstaller() {
		ProvUI.openUpdateManagerInstaller(null);
	}

	/**
	 * Open the old UpdateManager configuration manager UI using the specified shell. 
	 * We do not call the UpdateManagerUI class directly because we want to be able to be configured 
	 * without requiring those plug-ins.  Instead, we invoke a known command.
	 */
	public static void openConfigurationManager() {
		ProvUI.openUpdateManagerConfigurationManager(null);
	}

	private static void logFail(Throwable t) {
		Status failStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, t.getLocalizedMessage(), t);
		ProvUI.reportStatus(failStatus, StatusManager.LOG);
	}

	private static String getWritableXMLString(String value) {
		StringBuffer buf = new StringBuffer();
		if (value == null)
			return buf.toString();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}
}