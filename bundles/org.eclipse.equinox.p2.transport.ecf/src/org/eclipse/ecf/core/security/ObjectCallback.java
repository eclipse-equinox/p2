/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.security;

/**
 * Callback that handles arbitrary Objects
 * 
 */
public class ObjectCallback implements Callback {
	Object data;

	public ObjectCallback() {
		data = null;
	}

	public ObjectCallback(Object val) {
		this.data = val;
	}

	public void setObject(Object val) {
		this.data = val;
	}

	public Object getObject() {
		return this.data;
	}
}
