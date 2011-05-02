/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.persistence;

import org.eclipse.equinox.p2.metadata.Version;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import org.eclipse.core.runtime.URIUtil;

/*
 * Class used to persist a composite repository.
 */
public class CompositeWriter extends XMLWriter implements XMLConstants {

	private static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$
	private static final Version CURRENT_VERSION = Version.createOSGi(1, 0, 0);

	public CompositeWriter(OutputStream output, String type) throws UnsupportedEncodingException {
		super(output, new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeTargetVersionInstruction(type, CURRENT_VERSION)});
		// TODO: add a processing instruction for the metadata version
	}

	/**
	 * Writes a list of URIs referring to sub repositories
	 */
	protected void writeChildren(URI[] children) {
		if (children == null || children.length == 0)
			return;
		start(CHILDREN_ELEMENT);
		attribute(COLLECTION_SIZE_ATTRIBUTE, children.length);
		for (int i = 0; i < children.length; i++)
			writeChild(children[i]);
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
	public void write(CompositeRepositoryState repository) {
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
