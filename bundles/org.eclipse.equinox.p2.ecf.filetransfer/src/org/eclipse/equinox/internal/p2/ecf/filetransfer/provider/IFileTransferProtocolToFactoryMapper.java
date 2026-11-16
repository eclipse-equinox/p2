/****************************************************************************
 * Copyright (c) 2009 EclipseSource and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.equinox.internal.p2.ecf.filetransfer.provider;

import org.eclipse.equinox.internal.p2.ecf.filetransfer.service.IRemoteFileSystemBrowserFactory;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.service.IRetrieveFileTransferFactory;

/**
 * Allows to query and change the factories used for the protocols.
 */
public interface IFileTransferProtocolToFactoryMapper {

	/**
	 * <p>
	 * For the given protocol, set the given factory to be used for retrieve file transfer.  If successful, subsequent retrieve
	 * requests for the given protocol will use the given factory.  </p>
	 * <p>For this method to be successful the protocol has to be
	 * non-null, the id has to be non-null and unique (should probably be set to the bundle symbolic name of the bundle calling this
	 * method), the factory must be non-null, and the priority must be higher (a *smaller number*) than any existing factory for the
	 * given protocol.  The default priority is 100, and the highest priority is 0.
	 * </p>
	 * @param protocol the protocol (e.g. http/https) to map the factory to.
	 * @param id a unique id for the factory (should be bundle symbolic name of bundle calling method)
	 * @param factory the factory to associate with the given protocol
	 * @param priority priority (highest = 0) to use for this factory relative to any existing factories.
	 * @return <code>true</code> if the given factory was set for this protocol, <code>false</code> if not
	 */
	public boolean setRetrieveFileTransferFactory(String protocol, String id, IRetrieveFileTransferFactory factory, int priority);

	/**
	 * Get the factory id of the active factory for the given protocol.  If the given protocol does not have an
	 * active factory, returns <code>null</code>.
	 * 
	 * @param protocol the protocol to get the id for (e.g. http/https)
	 * 
	 * @return id of the factory associated with the given protocol
	 */
	public String getRetrieveFileTransferFactoryId(String protocol);

	/**
	 * Get the priority of the active factory for the given protocol.  If the given protocol does not have an active factory, returns -1.
	 * 
	 * @param protocol the protocol to get the priority for (e.g. http/https)
	 * 
	 * @return int priority for the given protocol
	 */
	public int getRetrieveFileTransferPriority(String protocol);

	/**
	 * Remove the factory with the given id.
	 * @param id the id of the factory to remove.
	 * @return <code>true</code> if a factory was removed.  <code>false</code> otherwise.
	 */
	public boolean removeRetrieveFileTransferFactory(String id);

	/**
	 * <p>
	 * For the given protocol, set the given factory to be used for retrieve file transfer.  If successful, subsequent retrieve
	 * requests for the given protocol will use the given factory.  </p>
	 * <p>For this method to be successful the protocol has to be
	 * non-null, the id has to be non-null and unique (should probably be set to the bundle symbolic name of the bundle calling this
	 * method), the factory must be non-null, and the priority must be higher (a *smaller number*) than any existing factory for the
	 * given protocol.  The default priority is 100, and the highest priority is 0.
	 * </p>
	 * @param protocol the protocol (e.g. http/https) to map the factory to.
	 * @param id a unique id for the factory (should be bundle symbolic name of bundle calling method)
	 * @param factory the factory to associate with the given protocol
	 * @param priority priority (highest = 0) to use for this factory relative to any existing factories.
	 * @return <code>true</code> if the given factory was set for this protocol, <code>false</code> if not
	 */
	public boolean setBrowseFileTransferFactory(String protocol, String id, IRemoteFileSystemBrowserFactory factory, int priority);

	/**
	 * Get the factory id of the active factory for the given protocol.  If the given protocol does not have an
	 * active factory, returns <code>null</code>.
	 * 
	 * @param protocol the protocol to get the id for (e.g. http/https)
	 * 
	 * @return id of the factory associated with the given protocol
	 */
	public String getBrowseFileTransferFactoryId(String protocol);

	/**
	 * Get the priority of the active factory for the given protocol.  If the given protocol does not have an active factory, returns -1.
	 * 
	 * @param protocol the protocol to get the priority for (e.g. http/https)
	 * 
	 * @return int priority for the given protocol
	 */
	public int getBrowseFileTransferPriority(String protocol);

	/**
	 * Remove the factory with the given id.
	 * @param id the id of the factory to remove.
	 * @return <code>true</code> if a factory was removed.  <code>false</code> otherwise.
	 */
	public boolean removeBrowseFileTransferFactory(String id);


}
