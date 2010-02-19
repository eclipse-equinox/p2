/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ql;

import java.util.Locale;
import org.eclipse.equinox.p2.query.IQuery;

/**
 * An IQuery 'context query' implementation that is based on the p2 query language.
 */
public abstract class QLQuery<T> implements IQuery<T> {
	final Class<T> elementClass;
	private Locale locale;

	protected QLQuery(Class<T> elementClass) {
		this.elementClass = elementClass;
	}

	public Locale getLocale() {
		return locale == null ? Locale.getDefault() : locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

}
