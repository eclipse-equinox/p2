/*******************************************************************************
 * Copyright (c) 2011, 2016 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.persistence;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.equinox.internal.p2.importexport.IUDetail;
import org.eclipse.equinox.internal.p2.importexport.VersionIncompatibleException;
import org.eclipse.equinox.internal.p2.importexport.internal.Messages;
import org.eclipse.equinox.internal.p2.persistence.XMLParser;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.xml.sax.*;

public class P2FParser extends XMLParser implements P2FConstants {
	static final VersionRange XML_TOLERANCE = new VersionRange(Version.createOSGi(1, 0, 0), true, Version.createOSGi(2, 0, 0), false);

	List<IUDetail> iusListed;

	protected class RepositoryHandler extends AbstractHandler {

		private final String[] required = new String[] {LOCATION_ELEMENT};
		private final String[] optional = new String[] {};
		private URI referredRepo;
		private List<URI> uri;

		public RepositoryHandler(AbstractHandler parentHandler, Attributes attributes, List<URI> uri) {
			super(parentHandler, REPOSITORY_ELEMENT);
			String[] values = parseAttributes(attributes, required, optional);
			//skip entire subrepository if the location is missing
			if (values[0] == null)
				return;
			this.uri = uri;
			referredRepo = checkURI(REPOSITORY_ELEMENT, LOCATION_ELEMENT, values[0]);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			checkCancel();
		}

		@Override
		protected void finished() {
			if (referredRepo != null)
				uri.add(referredRepo);
		}
	}

	protected class RepositoriesHandler extends AbstractHandler {

		List<URI> uris;

		public RepositoriesHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, REPOSITORIES_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			uris = (size == null) ? new ArrayList<URI>() : new ArrayList<URI>(Integer.parseInt(size));
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(REPOSITORY_ELEMENT)) {
				new RepositoryHandler(this, attributes, uris);
			}
		}

		public List<URI> getRepositories() {
			return uris;
		}
	}

	protected class IUHandler extends AbstractHandler {
		private final String[] required = new String[] {ID_ATTRIBUTE, NAME_ATTRIBUTE, VERSION_ATTRIBUTE};
		private final String[] optional = new String[] {};

		IInstallableUnit iu = null;
		private RepositoriesHandler repositoriesHandler;
		private List<IUDetail> ius;

		public IUHandler(AbstractHandler parentHandler, Attributes attributes, List<IUDetail> ius) {
			super(parentHandler, IU_ELEMENT);
			String[] values = parseAttributes(attributes, required, optional);
			//skip entire record if the id is missing
			if (values[0] == null)
				return;
			MetadataFactory.InstallableUnitDescription desc = new MetadataFactory.InstallableUnitDescription();
			desc.setId(values[0]);
			desc.setProperty(IInstallableUnit.PROP_NAME, values[1]);
			desc.setVersion(Version.create(values[2]));
			iu = MetadataFactory.createInstallableUnit(desc);
			this.ius = ius;
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(REPOSITORIES_ELEMENT)) {
				repositoriesHandler = new RepositoriesHandler(this, attributes);
			}
		}

		@Override
		protected void finished() {
			if (isValidXML()) {
				ius.add(new IUDetail(iu, repositoriesHandler.getRepositories()));
			}
		}
	}

	protected class IUsHandler extends AbstractHandler {

		private final List<IUDetail> ius;

		public IUsHandler(ContentHandler parentHandler, Attributes attributes) {
			super(parentHandler, IUS_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			ius = (size != null ? new ArrayList<IUDetail>(Integer.parseInt(size)) : new ArrayList<IUDetail>());
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (name.equals(IU_ELEMENT)) {
				new IUHandler(this, attributes, ius);
			} else {
				invalidElement(name, attributes);
			}
		}

		public List<IUDetail> getIUs() {
			return ius;
		}
	}

	private final class P2FDocHandler extends DocHandler {

		public P2FDocHandler(String rootName, RootHandler rootHandler) {
			super(rootName, rootHandler);
		}

		@Override
		public void processingInstruction(String target, String data) {
			if (P2F_ELEMENT.equals(target)) {
				Version repositoryVersion = extractPIVersion(target, data);
				if (!XML_TOLERANCE.isIncluded(repositoryVersion)) {
					throw new VersionIncompatibleException(NLS.bind(Messages.io_IncompatibleVersion, repositoryVersion, XML_TOLERANCE));
				}
			}
		}
	}

	final class P2FHandler extends RootHandler {
		private final String[] required = new String[] {};
		private final String[] optional = new String[] {};
		private IUsHandler iusHandler;

		@Override
		protected void handleRootAttributes(Attributes attributes) {
			parseAttributes(attributes, required, optional);
		}

		@Override
		public void startElement(String name, Attributes attributes) {
			if (IUS_ELEMENT.equals(name)) {
				if (iusHandler == null) {
					iusHandler = new IUsHandler(this, attributes);
				} else {
					duplicateElement(this, name, attributes);
				}
			} else {
				invalidElement(name, attributes);
			}
		}

		@Override
		protected void finished() {
			if (isValidXML()) {
				if (iusHandler != null)
					iusListed = iusHandler.getIUs();
			}
		}
	}

	public P2FParser(BundleContext context, String pluginId) {
		super(context, pluginId);
	}

	public void parse(File file) throws IOException {
		// don't overwrite if we already have a filename/location
		if (errorContext == null)
			setErrorContext(file.getAbsolutePath());
		parse(new FileInputStream(file));
	}

	public synchronized void parse(InputStream stream) throws IOException {
		this.status = null;
		try {
			// TODO: currently not caching the parser since we make no assumptions
			//		 or restrictions on concurrent parsing
			getParser();
			P2FHandler p2fHandler = new P2FHandler();
			xmlReader.setContentHandler(new P2FDocHandler(P2F_ELEMENT, p2fHandler));
			xmlReader.parse(new InputSource(stream));
		} catch (SAXException e) {
			IOException ioException = new IOException(e.getMessage());
			ioException.initCause(e);
			throw ioException;
		} catch (ParserConfigurationException e) {
			IOException ioException = new IOException(e.getMessage());
			ioException.initCause(e);
			throw ioException;
		} finally {
			stream.close();
		}
	}

	public List<IUDetail> getIUs() {
		if (iusListed == null) {
			iusListed = new ArrayList<IUDetail>();
		}
		return iusListed;
	}

	@Override
	protected Object getRootObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getErrorMessage() {
		return Messages.io_parseError;
	}

}
