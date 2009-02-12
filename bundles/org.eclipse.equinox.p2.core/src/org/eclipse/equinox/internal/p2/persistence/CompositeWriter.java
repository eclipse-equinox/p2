/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.persistence;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.repository.ICompositeRepository;

/*
 * Class used to persist a composite repository.
 */
public class CompositeWriter extends XMLWriter implements XMLConstants {

	private static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$
	private static final Version CURRENT_VERSION = new Version(1, 0, 0);

	public CompositeWriter(OutputStream output, String type) throws UnsupportedEncodingException {
		super(output, new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeClassVersionInstruction(type, ICompositeRepository.class, CURRENT_VERSION)});
		// TODO: add a processing instruction for the metadata version
	}

	/**
	 * Writes a list of URIs referring to sub repositories
	 */
	protected void writeChildren(List children) {
		if (children.size() == 0)
			return;
		start(CHILDREN_ELEMENT);
		attribute(COLLECTION_SIZE_ATTRIBUTE, children.size());
		for (Iterator iter = children.iterator(); iter.hasNext();)
			writeChild((URI) iter.next());
		end(CHILDREN_ELEMENT);
	}

	protected void writeChild(URI encodedURI) {
		String unencodedString = URIUtil.toUnencodedString(encodedURI);
		start(CHILD_ELEMENT);
		attribute(LOCATION_ELEMENT, unencodedString);
		end(CHILD_ELEMENT);
	}

	/**
	 * Write the given composite repository to the output stream.
	 */
	public void write(ICompositeRepository repository) {
		start(REPOSITORY_ELEMENT);
		attribute(NAME_ATTRIBUTE, repository.getName());
		attribute(TYPE_ATTRIBUTE, repository.getType());
		attribute(VERSION_ATTRIBUTE, repository.getVersion());
		attributeOptional(PROVIDER_ATTRIBUTE, repository.getProvider());
		attributeOptional(DESCRIPTION_ATTRIBUTE, repository.getDescription()); // TODO: could be cdata?
		writeProperties(repository.getProperties());
		writeChildren(repository.getChildren());
		end(REPOSITORY_ELEMENT);
		flush();
	}

}
