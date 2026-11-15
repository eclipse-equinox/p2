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
package org.eclipse.ecf.core.identity;

/**
 * Defines implementing classes as being identifiable with an ECF ID.
 * 
 */
public interface IIdentifiable {
	/**
	 * Return the ID for this 'identifiable' object. The returned ID should be
	 * unique within its namespace.  May return <code>null</code>.
	 * 
	 * @return the ID for this identifiable object.  May return <code>null</code>.
	 */
	public ID getID();
}
