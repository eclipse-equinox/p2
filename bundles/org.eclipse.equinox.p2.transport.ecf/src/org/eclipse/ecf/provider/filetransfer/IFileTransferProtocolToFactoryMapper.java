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
package org.eclipse.ecf.provider.filetransfer;

import org.eclipse.ecf.filetransfer.service.IRemoteFileSystemBrowserFactory;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.ecf.filetransfer.service.ISendFileTransferFactory;

/**
 * 
 * @since 3.0.1
 *
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
	 * @param uri if <code>true</code> the factory is added as a URI rather than a URL, meaning that <b>no</b> URLStreamHandler is
	 * registered for the given protocol.  This is in contrast to the {@link #setRetrieveFileTransferFactory(String, String, IRetrieveFileTransferFactory, int)},
	 * which automatically registers an URLStreamHandler for the given protocol.  If false, URLs will be used and an URLStreamHandler will be registered for the 
	 * given protocol factory.  NOTE:  If this flag is true, providers that attempt to access IFileID.getURL() may be unable to do so, since the
	 * URI may not be successfully parsed as a URL.
	 * 
	 * @return <code>true</code> if the given factory was set for this protocol, <code>false</code> if not
	 */
	public boolean setRetrieveFileTransferFactory(String protocol, String id, IRetrieveFileTransferFactory factory, int priority, boolean uri);

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
	 * @param uri if <code>true</code> the factory is added as a URI rather than a URL, meaning that <b>no</b> URLStreamHandler is
	 * registered for the given protocol.  This is in contrast to the {@link #setRetrieveFileTransferFactory(String, String, IRetrieveFileTransferFactory, int)},
	 * which automatically registers an URLStreamHandler for the given protocol.  If false, URLs will be used and an URLStreamHandler will be registered for the 
	 * given protocol factory.  NOTE:  If this flag is true, providers that attempt to access IFileID.getURL() may be unable to do so, since the
	 * URI may not be successfully parsed as a URL.
	 *
	 * @return <code>true</code> if the given factory was set for this protocol, <code>false</code> if not
	 */
	public boolean setBrowseFileTransferFactory(String protocol, String id, IRemoteFileSystemBrowserFactory factory, int priority, boolean uri);

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
	public boolean setSendFileTransferFactory(String protocol, String id, ISendFileTransferFactory factory, int priority);

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
	 * @param uri if <code>true</code> the factory is added as a URI rather than a URL, meaning that <b>no</b> URLStreamHandler is
	 * registered for the given protocol.  This is in contrast to the {@link #setRetrieveFileTransferFactory(String, String, IRetrieveFileTransferFactory, int)},
	 * which automatically registers an URLStreamHandler for the given protocol.  If false, URLs will be used and an URLStreamHandler will be registered for the 
	 * given protocol factory.  NOTE:  If this flag is true, providers that attempt to access IFileID.getURL() may be unable to do so, since the
	 * URI may not be successfully parsed as a URL.
	 *
	 * @return <code>true</code> if the given factory was set for this protocol, <code>false</code> if not
	 */
	public boolean setSendFileTransferFactory(String protocol, String id, ISendFileTransferFactory factory, int priority, boolean uri);

	/**
	 * Get the factory id of the active factory for the given protocol.  If the given protocol does not have an
	 * active factory, returns <code>null</code>.
	 * 
	 * @param protocol the protocol to get the id for (e.g. http/https)
	 * 
	 * @return id of the factory associated with the given protocol
	 */
	public String getSendFileTransferFactoryId(String protocol);

	/**
	 * Get the priority of the active factory for the given protocol.  If the given protocol does not have an active factory, returns -1.
	 * 
	 * @param protocol the protocol to get the priority for (e.g. http/https)
	 * 
	 * @return int priority for the given protocol
	 */
	public int getSendFileTransferPriority(String protocol);

	/**
	 * Remove the factory with the given id.
	 * @param id the id of the factory to remove.
	 * @return <code>true</code> if a factory was removed.  <code>false</code> otherwise.
	 */
	public boolean removeSendFileTransferFactory(String id);

	/**
	 * Reinitialized protocol to factory mapping defined via extension registry/extension points.
	 * @return true if reinitialization succeeds, false if not
	 */
	public boolean reinitialize();

}
