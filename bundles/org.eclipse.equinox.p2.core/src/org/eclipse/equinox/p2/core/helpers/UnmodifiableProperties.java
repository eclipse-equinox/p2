/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.core.helpers;

import java.util.Iterator;
import java.util.Map;

public class UnmodifiableProperties extends OrderedProperties {

	public UnmodifiableProperties(OrderedProperties properties) {
		super();
		for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			super.put(entry.getKey(), entry.getValue());
		}
	}

	public synchronized Object setProperty(String key, String value) {
		throw new UnsupportedOperationException();
	}

	public synchronized Object put(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	public synchronized Object remove(Object key) {
		throw new UnsupportedOperationException();
	}

	public synchronized void putAll(Map t) {
		throw new UnsupportedOperationException();
	}

	public synchronized void clear() {
		throw new UnsupportedOperationException();
	}

}
