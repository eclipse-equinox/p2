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

import java.net.URI;
import java.util.*;
import org.osgi.framework.BundleContext;
import org.xml.sax.Attributes;

public abstract class CompositeParser extends XMLParser implements XMLConstants {

	public static final String REQUIRED_CAPABILITY_ELEMENT = "required"; //$NON-NLS-1$

	public CompositeParser(BundleContext context, String bundleId) {
		super(context, bundleId);
	}

	protected class ChildrenHandler extends AbstractHandler {
		private ArrayList children;

		public ChildrenHandler(AbstractHandler parentHandler, Attributes attributes) {
			super(parentHandler, CHILDREN_ELEMENT);
			String size = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
			children = (size != null ? new ArrayList(new Integer(size).intValue()) : new ArrayList(4));
		}

		public URI[] getChildren() {
			int size = children.size();
			URI[] result = new URI[size];
			int i = 0;
			for (Iterator it = children.iterator(); it.hasNext(); i++) {
				result[i] = (URI) it.next();
			}
			return result;
		}

		public void startElement(String name, Attributes attributes) {
			if (name.equals(CHILD_ELEMENT)) {
				new ChildHandler(this, attributes, children);
			} else {
				invalidElement(name, attributes);
			}
		}
	}

	protected class ChildHandler extends AbstractHandler {
		private final String[] required = new String[] {LOCATION_ELEMENT};
		private final String[] optional = new String[] {};

		URI currentRepo = null;

		private List repos;

		public ChildHandler(AbstractHandler parentHandler, Attributes attributes, List repos) {
			super(parentHandler, CHILD_ELEMENT);
			String[] values = parseAttributes(attributes, required, optional);
			this.repos = repos;
			//skip entire subrepository if the location is missing
			if (values[0] == null)
				return;
			currentRepo = checkURI(REQUIRED_CAPABILITY_ELEMENT, URI_ATTRIBUTE, values[0]);

		}

		public void startElement(String name, Attributes attributes) {
			checkCancel();
		}

		protected void finished() {
			if (currentRepo != null)
				repos.add(currentRepo);
		}
	}
}
