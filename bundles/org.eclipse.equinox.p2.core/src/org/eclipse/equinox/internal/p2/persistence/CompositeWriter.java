/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import org.eclipse.core.runtime.URIUtil;

public abstract class CompositeWriter extends XMLWriter implements XMLConstants {

	public CompositeWriter(OutputStream output, ProcessingInstruction[] piElements) throws UnsupportedEncodingException {
		super(output, piElements);
		// TODO: add a processing instruction for the metadata version
	}

	/**
	 * Writes a list of URIs referring to sub repositories
	 */
	protected void writeChildren(Iterator children, int size) {
		if (size == 0)
			return;
		start(CHILDREN_ELEMENT);
		attribute(COLLECTION_SIZE_ATTRIBUTE, size);
		while (children.hasNext())
			writeChild((URI) children.next());
		end(CHILDREN_ELEMENT);
	}

	protected void writeChild(URI encodedURI) {
		String unencodedString = URIUtil.toUnencodedString(encodedURI);
		start(CHILD_ELEMENT);
		attribute(LOCATION_ELEMENT, unencodedString);
		end(CHILD_ELEMENT);
	}
}
