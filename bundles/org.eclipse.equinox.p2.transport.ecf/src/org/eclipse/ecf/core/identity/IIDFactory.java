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

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Contract for {@link IDFactory}
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IIDFactory {
	/**
	 * Add the given Namespace to our table of available Namespaces
	 * 
	 * @param n
	 *            the Namespace to add
	 * @return Namespace the namespace already in table (null if Namespace not
	 *         previously in table)
	 * @exception SecurityException
	 *                thrown if caller does not have appropriate
	 *                NamespacePermission for given namespace
	 */
	public Namespace addNamespace(Namespace n) throws SecurityException;

	/**
	 * Check whether table contains given Namespace instance
	 * 
	 * @param n
	 *            the Namespace to look for
	 * @return true if table does contain given Namespace, false otherwise
	 * @exception SecurityException
	 *                thrown if caller does not have appropriate
	 *                NamespacePermission for given namespace
	 */
	public boolean containsNamespace(Namespace n) throws SecurityException;

	/**
	 * Get a list of the current Namespace instances exposed by this factory.
	 * 
	 * @return List<Namespace> of Namespace instances
	 * @exception SecurityException
	 *                thrown if caller does not have appropriate
	 *                NamespacePermission for given namespace
	 */
	public List<Namespace> getNamespaces() throws SecurityException;

	/**
	 * Get the given Namespace instance from table
	 * 
	 * @param n
	 *            the Namespace to look for
	 * @return Namespace
	 * @exception SecurityException
	 *                thrown if caller does not have appropriate
	 *                NamespacePermission for given namespace
	 */
	public Namespace getNamespace(Namespace n) throws SecurityException;

	/**
	 * Get a Namespace instance by its string name.
	 * 
	 * @param name
	 *            the name to use for lookup
	 * @return Namespace instance. Null if not found.
	 * @exception SecurityException
	 *                thrown if caller does not have appropriate
	 *                NamespacePermission for given namespace
	 */
	public Namespace getNamespaceByName(String name) throws SecurityException;

	/**
	 * Make a GUID using SHA-1 hash algorithm and a default of 16bits of data
	 * length. The value is Base64 encoded to allow for easy display.
	 * 
	 * @return new ID instance
	 * @throws IDCreateException
	 *             if ID cannot be constructed
	 */
	public ID createGUID() throws IDCreateException;

	/**
	 * Make a GUID using SHA-1 hash algorithm and a default of 16bits of data
	 * length. The value is Base64 encoded to allow for easy display.
	 * 
	 * @param length
	 *            the byte-length of data used to create a GUID
	 * @return new ID instance
	 * @throws IDCreateException
	 *             if ID cannot be constructed
	 */
	public ID createGUID(int length) throws IDCreateException;

	/**
	 * Make a new identity. Given a Namespace, and an array of instance
	 * constructor parameters, return a new instance of an ID belonging to the
	 * given Namespace
	 * 
	 * @param n
	 *            the Namespace to which the ID will belong
	 * @param args
	 *            an Object [] of the parameters for the ID instance constructor
	 * @exception IDCreateException
	 *                thrown if class for instantiator or instance can't be
	 *                loaded, if something goes wrong during instance
	 *                construction
	 */
	public ID createID(Namespace n, Object[] args) throws IDCreateException;

	/**
	 * Make a new identity. Given a Namespace name, and an array of instance
	 * constructor parameters, return a new instance of an ID belonging to the
	 * given Namespace
	 * 
	 * @param namespaceName
	 *            the name of the Namespace to which the ID will belong
	 * @param args
	 *            an Object [] of the parameters for the ID instance constructor
	 * @exception IDCreateException
	 *                thrown if class for instantiator or ID instance can't be
	 *                loaded, if something goes wrong during instance
	 *                construction
	 */
	public ID createID(String namespaceName, Object[] args)
			throws IDCreateException;

	/**
	 * Make a new identity instance from a namespace and String.
	 * 
	 * @param namespace
	 *            the namespace to use to create the ID
	 * @param uri
	 *            the String uri to use to create the ID
	 * @exception IDCreateException
	 *                thrown if class for instantiator or ID instance can't be
	 *                loaded, if something goes wrong during instance
	 *                construction
	 */
	public ID createID(Namespace namespace, String uri)
			throws IDCreateException;

	/**
	 * Make a new identity instance from a namespaceName and idValue. The
	 * namespaceName is first used to lookup the namespace with
	 * {@link #getNamespaceByName(String)}, and then the result is passed into
	 * {@link #createID(Namespace,String)}.
	 * 
	 * @param namespaceName
	 *            the name of the namespace that should be used to create the ID
	 * @param idValue
	 *            the String value to use to create the ID
	 * @exception IDCreateException
	 *                thrown if class for instantiator or ID instance can't be
	 *                loaded, if something goes wrong during instance
	 *                construction
	 */
	public ID createID(String namespaceName, String idValue)
			throws IDCreateException;

	/**
	 * Make a an ID from a String
	 * 
	 * @param idString
	 *            the String to use as this ID's unique value. Note: It is
	 *            incumbent upon the caller of this method to be sure that the
	 *            given string allows the resulting ID to satisfy the ID
	 *            contract for global uniqueness within the associated
	 *            Namespace.
	 * 
	 * @return valid ID instance
	 * @throws IDCreateException
	 *             thrown if class for instantiator or ID instance can't be
	 *             loaded, if something goes wrong during instance construction
	 */
	public ID createStringID(String idString) throws IDCreateException;

	/**
	 * Make a an ID from a long
	 * 
	 * @param l
	 *            the long to use as this ID's unique value. Note: It is
	 *            incumbent upon the caller of this method to be sure that the
	 *            given long allows the resulting ID to satisfy the ID contract
	 *            for global uniqueness within the associated Namespace.
	 * 
	 * @return valid ID instance
	 * @throws IDCreateException
	 *             thrown if class for instantiator or ID instance can't be
	 *             loaded, if something goes wrong during instance construction
	 */
	public ID createLongID(long l) throws IDCreateException;

	/**
	 * Create a UuID from String
	 * 
	 * @param uuid
	 *            the String to use. Must be in UUID format as returned from
	 *            UUID.toString(). Must not be null.
	 * @return valid ID instance
	 * 
	 * @since 3.5
	 */
	public ID createUuID(String uuid) throws IDCreateException;

	/**
	 * Create a UuID from UUID
	 * 
	 * @param uuid
	 *            the UUID to use. Must not be null.
	 * @return valid ID instance
	 * 
	 * @since 3.5
	 */
	public ID createUuID(UUID uuid) throws IDCreateException;

	/**
	 * Create a random UuID
	 * 
	 * @return valid ID instance from UUID.randomUUID()
	 * 
	 * @since 3.5
	 */
	public ID createUuID() throws IDCreateException;

	/**
	 * Create a UuID from URI.
	 * 
	 * @param uuidURI
	 *            the URI. Must not be null and must be in valid uuid syntax
	 *            form as specified by rfc4122 see
	 *            http://tools.ietf.org/html/rfc4122. Example:
	 *            'uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6'
	 * 
	 * @return valid ID instance
	 * 
	 * @since 3.5
	 */
	public ID createUuID(URI uuidURI) throws IDCreateException;

	/**
	 * Create a URIID from URI.
	 * 
	 * @param uri
	 *            the URI to use for the URIID. Must not be null.
	 * 
	 * @return valid ID instance
	 * 
	 * @since 3.5
	 */
	public ID createURIID(URI uri) throws IDCreateException;

	/**
	 * Create a URIID from String.
	 * 
	 * @param uri
	 *            the String to use for the URIID. Must not be null, and must be
	 *            valid URI format as per URI.toString().
	 * 
	 * @return valid ID instance
	 * 
	 * @since 3.5
	 */
	public ID createURIID(String uri) throws IDCreateException;

	/**
	 * Remove the given Namespace from our table of available Namespaces
	 * 
	 * @param n
	 *            the Namespace to remove
	 * @return Namespace the namespace already in table (null if Namespace not
	 *         previously in table)
	 * @exception SecurityException
	 *                thrown if caller does not have appropriate
	 *                NamespacePermission for given namespace
	 */
	public Namespace removeNamespace(Namespace n) throws SecurityException;
}