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
package org.eclipse.equinox.p2.metadata;

import java.util.Dictionary;
import org.eclipse.equinox.internal.p2.metadata.MetadataActivator;
import org.osgi.framework.InvalidSyntaxException;

public class Selector {
	private String id;
	private String filter;
	private boolean internal;

	public Selector(String id, String filter, boolean internal) {
		this.id = id;
		this.filter = filter;
		this.internal = internal;
	}

	public String getId() {
		return id;
	}

	public boolean eval(Dictionary context) {
		try {
			return MetadataActivator.context.createFilter(filter).match(context);
		} catch (InvalidSyntaxException e) {
			//TODO log something. though that should not happen
			return false;
		}
	}

	public boolean isInternal() {
		return internal;
	}
}
