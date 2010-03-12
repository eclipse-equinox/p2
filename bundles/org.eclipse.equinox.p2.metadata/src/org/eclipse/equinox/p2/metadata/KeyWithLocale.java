/*******************************************************************************
 * Copyright (c) 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.io.Serializable;
import java.util.Locale;

/**
 * A key that can be used to extract a localized property for a specified Locale
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public final class KeyWithLocale implements Serializable {
	private static final long serialVersionUID = 8818242663547645188L;
	private final String key;
	private final Locale locale;

	public KeyWithLocale(String key, Locale locale) {
		this.key = key;
		this.locale = locale;
	}

	public String getKey() {
		return key;
	}

	public Locale getLocale() {
		return locale;
	}
}
