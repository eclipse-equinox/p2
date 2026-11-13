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

import org.eclipse.core.runtime.IAdaptable;

/**
 * Contract for ECF identity
 * <p>
 * ECF IDs are immutable once constructed, and unique within the containing
 * {@link Namespace}.
 * <p>
 * ID instances are created via the Namespace.createInstance(...) method. This
 * method is called by the IDFactory.createID(...) methods for the given
 * Namespace. So, for example, to create an ID instance with the name "slewis":
 * 
 * <pre>
 * ID id = IDFactory.getDefault().createID(namespace, &quot;slewis&quot;);
 * </pre>
 * 
 * <p>
 * 
 * @see Namespace
 * 
 */
@SuppressWarnings("rawtypes")
public interface ID extends java.io.Serializable, java.lang.Comparable,
		java.security.Principal, IAdaptable {

	public boolean equals(Object obj);

	public int hashCode();

	/**
	 * Get the unique name of this identity.
	 * 
	 * @return String unique name for this identity. Will not be null, and must
	 *         be a unique String within the Namespace returned by
	 *         getNamespace()
	 */
	public String getName();

	/**
	 * Get the Namespace instance associated with this identity
	 * 
	 * @return Namespace the Namespace corresponding to this identity. Will not
	 *         return null.
	 */
	public Namespace getNamespace();

	/**
	 * Get this ID instance in String form. Will not return null.
	 * 
	 * @return String that is external representation of this ID
	 */
	public String toExternalForm();
}