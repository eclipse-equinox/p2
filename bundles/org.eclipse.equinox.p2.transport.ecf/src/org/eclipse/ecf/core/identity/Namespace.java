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

import java.io.Serializable;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.internal.core.identity.Activator;

/**
 * Namespace base class.
 * <p>
 * This class and subclasses define a namespace for the creation and management
 * of ID instances. Creation of ID instances is accomplished via the
 * {@link #createInstance(Object[])} method, implemented by subclasses of this
 * Namespace superclass.
 * <p>
 * All Namespace instances must have a unique name passed to the Namespace upon
 * construction.
 * <p>
 * Typically Namespace instances are created via plugins that define extensions
 * of the org.eclipse.ecf.namespace extension point. For example, to define a
 * new Namespace subclass XMPPNamespace with name "ecf.xmpp" and add it to the
 * ECF extension registry:
 * 
 * <pre>
 *        &lt;extension
 *             point=&quot;org.eclipse.ecf.namespace&quot;&gt;
 *          &lt;namespace
 *                class=&quot;XMPPNamespace&quot;
 *                name=&quot;ecf.xmpp&quot;/&gt;
 *        &lt;/extension&gt;
 * </pre>
 * 
 * @see ID
 */
public abstract class Namespace implements Serializable, IAdaptable {

	private static final long serialVersionUID = 3976740272094720312L;

	public static final String SCHEME_SEPARATOR = ":"; //$NON-NLS-1$

	private String name;

	private String description;

	private transient int hashCode;

	private transient boolean isInitialized = false;

	public Namespace() {
		// public null constructor
	}

	public final boolean initialize(String n, String desc) {
		Assert.isNotNull(n, "Namespace<init> name cannot be null"); //$NON-NLS-1$
		if (!isInitialized) {
			this.name = n;
			this.description = desc;
			this.hashCode = name.hashCode();
			this.isInitialized = true;
			return true;
		}
		return false;
	}

	public Namespace(String name, String desc) {
		initialize(name, desc);
	}

	/**
	 * Override of Object.equals. This equals method returns true if the provided
	 * Object is also a Namespace instance, and the names of the two instances
	 * match.
	 * 
	 * @param other
	 *            the Object to test for equality
	 */
	public boolean equals(Object other) {
		if (!(other instanceof Namespace))
			return false;
		return ((Namespace) other).name.equals(name);
	}

	/**
	 * Hashcode implementation. Subclasses should not override.
	 * 
	 * @return int hashCode for this Namespace. Should be unique.
	 */
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Test whether two IDs are equal to one another.
	 * 
	 * @param first
	 *            the first ID. Must not be <code>null</code>.
	 * @param second
	 *            the second ID. Must not be <code>null</code>.
	 * @return <code>true</code> if this ID is equal to the given ID.
	 *         <code>false</code> otherwise.
	 */
	protected boolean testIDEquals(BaseID first, BaseID second) {
		// First check that namespaces are the same and non-null
		Namespace sn = second.getNamespace();
		if (sn == null || !this.equals(sn))
			return false;
		return first.namespaceEquals(second);
	}

	/**
	 * The default implementation of this method is to call id.namespaceGetName().
	 * Subclasses may override.
	 * 
	 * @param id
	 *            the ID to get the name for. Must not be <code>null</code>.
	 * @return String that is the unique name for the given id within this
	 *         Namespace.
	 */
	protected String getNameForID(BaseID id) {
		return id.namespaceGetName();
	}

	/**
	 * The default implementation of this method is to call
	 * first.namespaceCompareTo(second). Subclasses may override.
	 * 
	 * @param first
	 *            the first id to compare. Must not be <code>null</code>.
	 * @param second
	 *            the second id to compare. Must not be <code>null</code>.
	 * @return int as specified by {@link Comparable}.
	 */
	protected int getCompareToForObject(BaseID first, BaseID second) {
		return first.namespaceCompareTo(second);
	}

	/**
	 * The default implementation of this method is to call id.namespaceHashCode().
	 * Subclasses may override.
	 * 
	 * @param id
	 *            the id in this Namespace to get the hashcode for. Must not be
	 *            <code>null</code>.
	 * @return the hashcode for the given id. Returned value must be unique within
	 *         this process.
	 */
	protected int getHashCodeForID(BaseID id) {
		return id.namespaceHashCode();
	}

	/**
	 * The default implementation of this method is to call
	 * id.namespaceToExternalForm(). Subclasses may override.
	 * 
	 * @param id
	 *            the id in this Namespace to convert to external form.
	 * @return String that represents the given id in an external form. Note that
	 *         this external form may at some later time be passed to
	 *         {@link #createInstance(Object[])} as a single String parameter, and
	 *         should result in a valid ID instance of the appropriate Namespace.
	 */
	protected String toExternalForm(BaseID id) {
		return id.namespaceToExternalForm();
	}

	/**
	 * Get the name of this namespace. Must not return <code>null</code>.
	 * 
	 * @return String name of Namespace instance. Must not return <code>null</code>,
	 *         and the returned value should be a globally unique name for this
	 *         Namespace subclass.
	 * 
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the description, associated with this Namespace. The returned value may
	 * be <code>null</code>.
	 * 
	 * @return the description associated with this Namespace. May be
	 *         <code>null</code>.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Make an instance of this namespace. Namespace subclasses, provided by plugins
	 * must implement this method to construct ID instances for the given namespace.
	 * <p>
	 * </p>
	 * See {@link #getSupportedParameterTypes()} to get information relevant to
	 * deciding what parameter types are expected by this method.
	 * <p>
	 * </p>
	 * 
	 * @param parameters
	 *            an Object[] of parameters for creating ID instances. May be null.
	 * 
	 * @return a non-null ID instance. The class used may extend BaseID or may
	 *         implement the ID interface directly
	 * @throws IDCreateException
	 *             if construction fails
	 */
	public abstract ID createInstance(Object[] parameters) throws IDCreateException;

	/**
	 * Get the primary scheme associated with this namespace. Subclasses must
	 * provide an implementation that returns a non-<code>null</code> scheme
	 * identifier. Note that the returned scheme should <b>not</b> contain the
	 * Namespace.SCHEME_SEPARATOR (\":\").
	 * 
	 * @return a String scheme identifier. Must not be <code>null</code>.
	 */
	public abstract String getScheme();

	/**
	 * Get an array of schemes supported by this Namespace instance. Subclasses may
	 * override to support multiple schemes.
	 * 
	 * @return String[] of schemes supported by this Namespace. Will not be
	 *         <code>null</code>, but returned array may be of length 0.
	 */
	public String[] getSupportedSchemes() {
		return new String[0];
	}

	/**
	 * Get the supported parameter types for IDs created via subsequent calls to
	 * {@link #createInstance(Object[])}. Callers may use this method to determine
	 * the available parameter types, and then create and pass in conforming Object
	 * arrays to to {@link #createInstance(Object[])}.
	 * <p>
	 * </p>
	 * An empty two-dimensional array (new Class[0][0]) is the default returned by
	 * this abstract superclass. This means that the Object [] passed to
	 * {@link #createInstance(Object[])} will be ignored.
	 * <p>
	 * </p>
	 * Subsclasses should override this method to specify the parameters that they
	 * will accept in calls to {@link #createInstance(Object[])}. The rows of the
	 * returned Class array are the acceptable types for a given invocation of
	 * createInstance.
	 * <p>
	 * </p>
	 * Consider the following example:
	 * <p>
	 * </p>
	 * 
	 * <pre>
	 * public Class[][] getSupportedParameterTypes() {
	 * 	return new Class[][] { { String.class }, { String.class, String.class } };
	 * }
	 * </pre>
	 * 
	 * The above means that there are two acceptable values for the Object [] passed
	 * into {@link #createInstance(Object[])}: 1) a single String, and 2) two
	 * Strings. These would therefore be acceptable as input to createInstance:
	 * 
	 * <pre>
	 *        ID newID1 = namespace.createInstance(new Object[] { &quot;Hello&quot; });
	 *        ID newID2 = namespace.createInstance(new Object[] { &quot;Hello&quot;, &quot;There&quot;}};
	 * </pre>
	 * 
	 * @return Class [][] an array of class []s. Rows of the returned
	 *         two-dimensional array define the acceptable parameter types for a
	 *         single call to {@link #createInstance(Object[])}. If zero-length
	 *         Class arrays are returned (i.e. Class[0][0]), then Object []
	 *         parameters to {@link #createInstance(Object[])} will be ignored.
	 */
	public Class<?>[][] getSupportedParameterTypes() {
		return new Class[][] { {} };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter.isInstance(this)) {
			return this;
		}
		IAdapterManager manager = Activator.getDefault().getAdapterManager();
		if (manager == null)
			return null;
		return manager.loadAdapter(this, adapter.getName());
	}

	/**
	 * @since 3.1
	 */
	protected String getInitStringFromExternalForm(Object[] args) {
		if (args == null || args.length < 1 || args[0] == null)
			return null;
		if (args[0] instanceof String) {
			final String arg = (String) args[0];
			if (arg.startsWith(getScheme() + SCHEME_SEPARATOR)) {
				final int index = arg.indexOf(SCHEME_SEPARATOR);
				if (index >= arg.length())
					return null;
				return arg.substring(index + 1);
			}
		}
		return null;
	}

	public String toString() {
		StringBuilder b = new StringBuilder("Namespace["); //$NON-NLS-1$
		b.append("name=").append(name).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		b.append("scheme=").append(getScheme()).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		b.append("description=").append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		return b.toString();
	}
}