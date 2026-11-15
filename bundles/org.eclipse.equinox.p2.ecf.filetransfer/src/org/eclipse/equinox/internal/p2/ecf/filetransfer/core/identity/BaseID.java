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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.ecf.internal.core.identity.Activator;

/**
 * Base class for ID implementation classes
 * 
 * Extensions for the <b>org.eclipse.ecf.namespace</b> extension point that
 * expose new Namespace subclasses and their own ID implementations are
 * recommended (but not required) to use this class as a superclass.
 * 
 */
public abstract class BaseID implements ID {

	private static final long serialVersionUID = -6242599410460002514L;

	protected Namespace namespace;

	/**
	 * @since 3.9
	 */
	public BaseID() {
		//
	}

	protected BaseID(Namespace namespace) {
		Assert.isNotNull(namespace, "namespace cannot be null"); //$NON-NLS-1$
		this.namespace = namespace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(T)
	 */
	public int compareTo(Object o) {
		Assert.isTrue(o != null && o instanceof BaseID, "incompatible types for compare"); //$NON-NLS-1$
		return namespace.getCompareToForObject(this, (BaseID) o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || !(o instanceof BaseID)) {
			return false;
		}
		return namespace.testIDEquals(this, (BaseID) o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.identity.ID#getName()
	 */
	public String getName() {
		return namespace.getNameForID(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.identity.ID#getNamespace()
	 */
	public Namespace getNamespace() {
		return namespace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return namespace.getHashCodeForID(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.identity.ID#toExternalForm()
	 */
	public String toExternalForm() {
		return namespace.toExternalForm(this);
	}

	/**
	 * Called by {@link Namespace#getCompareToForObject(BaseID, BaseID)}.
	 * 
	 * @param o
	 *            the other ID to compare to. Will not be <code>null</code>.
	 * @return the appropriate value as per {@link Comparable} contract.
	 */
	protected abstract int namespaceCompareTo(BaseID o);

	/**
	 * Called by {@link Namespace#testIDEquals(BaseID, BaseID)}.
	 * 
	 * @param o
	 *            the other ID to test against. May be <code>null</code>.
	 * @return <code>true</code> if this ID is equal to the given ID.
	 *         <code>false</code> otherwise.
	 */
	protected abstract boolean namespaceEquals(BaseID o);

	/**
	 * Called by {@link Namespace#getNameForID(BaseID)}.
	 * 
	 * @return String name for this ID. Must not be <code>null</code>. Value
	 *         returned should be unique within this Namespace.
	 */
	protected abstract String namespaceGetName();

	/**
	 * Called by {@link Namespace#getHashCodeForID(BaseID)}.
	 * 
	 * @return int hashCode for this ID. Returned value must be unique within this
	 *         process.
	 */
	protected abstract int namespaceHashCode();

	/**
	 * Called by {@link Namespace#toExternalForm(BaseID)}.
	 * 
	 * @return String that represents this ID. Default implementation is to return
	 * 
	 *         <pre>
	 * namespace.getScheme() + Namespace.SCHEME_SEPARATOR + namespaceGetName();
	 *         </pre>
	 */
	protected String namespaceToExternalForm() {
		return namespace.getScheme() + Namespace.SCHEME_SEPARATOR + namespaceGetName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public Object getAdapter(@SuppressWarnings("rawtypes") Class clazz) {
		IAdapterManager adapterManager = Activator.getDefault().getAdapterManager();
		if (adapterManager == null)
			return null;
		return adapterManager.getAdapter(this, clazz.getName());
	}
}