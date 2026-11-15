/****************************************************************************
 * Copyright (c) 2004 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.filetransfer;

import java.util.Map;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.events.*;
import org.eclipse.ecf.filetransfer.identity.IFileID;

/**
 * Entry point retrieval file transfer adapter. This adapter interface allows
 * providers to expose file retrieval semantics to clients in a transport
 * independent manner. To be used, a non-null adapter reference must be returned
 * from a call to {@link IContainer#getAdapter(Class)}. Once a non-null
 * reference is retrieved, then it may be used to send a retrieve request.
 * Events will then be asynchronously delivered to the provided listener to
 * complete file transfer.
 * <p>
 * For example, to retrieve a remote file and store it in a local file:
 * 
 * <pre>
 * // Get IRetrieveFileTransferContainerAdapter adapter
 * IRetrieveFileTransferContainerAdapter ftc = (IRetrieveFileTransferContainerAdapter) container
 * 		.getAdapter(IRetrieveFileTransferContainerAdapter.class);
 * if (ftc != null) {
 * 	// Create listener for receiving/responding to asynchronous file transfer events
 * 	IFileTransferListener listener = new IFileTransferListener() {
 * 		public void handleTransferEvent(IFileTransferEvent event) {
 * 			// If incoming receive start event, respond by specifying local file to save to
 * 			if (event instanceof IIncomingFileTransferReceiveStartEvent) {
 * 				IIncomingFileTransferReceiveStartEvent rse = (IIncomingFileTransferReceiveStartEvent) event;
 * 				try {
 * 					rse.receive(new File(&quot;composent.main.page.html&quot;));
 * 				} catch (IOException e) {
 * 					// Handle exception appropriately 
 * 				}
 * 			}
 * 		}
 * 	};
 * 	// Identify file to retrieve and create ID
 * 	IFileID remoteFileID = FileIDFactory.getDefault().createID(
 * 			ftc.getRetrieveNamespace(), &quot;http://www.composent.com/index.html&quot;);
 * 	// Actually make request to start retrieval.  The listener provided will then be notified asynchronously 
 * 	// as file transfer events occur
 * 	ftc.sendRetrieveRequest(remoteFileID, listener, null);
 * }
 * </pre>
 * 
 * Where the IFileTransferEvent subtypes <b>for the receiver</b> will be:
 * <ul>
 * <li>{@link IIncomingFileTransferReceiveStartEvent}</li>
 * <li>{@link IIncomingFileTransferReceiveDataEvent}</li>
 * <li>{@link IIncomingFileTransferReceiveDoneEvent}</li>
 * </ul>
 */
public interface IRetrieveFileTransferContainerAdapter extends IAdaptable {

	/**
	 * Send request for transfer of a remote file to local file storage. This
	 * method is used to initiate a file retrieve for a remoteFileID (first
	 * parameter). File transfer events are asynchronously delivered a file
	 * transfer listener (second parameter). The given remoteFileID and
	 * transferListener must not be null.
	 * <p>
	 * <b>NOTE</b>: if this method completes successfully, the given transferListener 
	 * will be asynchronously notified via an IIncomingFileTransferReceiveDoneEvent 
	 * (along with other possible events).  All implementations are required to 
	 * issue this event whether successful or failed.  Listeners
	 * can consult {@link IIncomingFileTransferReceiveDoneEvent#getException()} to 
	 * determine whether the transfer operation completed successfully.
	 * </p>
	 * @param remoteFileID
	 *            reference to the remote target file (e.g.
	 *            http://www.eclipse.org/index.html) or a reference to a
	 *            resource that specifies the location of a target file.
	 *            Implementing providers will determine what protocol schemes
	 *            are supported (e.g. ftp, http, torrent, file, etc) and the
	 *            required format of the scheme-specific information. If a
	 *            protocol is specified that is not supported, or the
	 *            scheme-specific information is not well-formed, then an
	 *            IncomingFileTransferException will be thrown. Typically,
	 *            callers will create IFileID instances via calls such as:
	 * 
	 * <pre>
	 * IFileID remoteFileID = FileIDFactory.getDefault().createID(
	 * 		ftc.getRetrieveNamespace(), &quot;http://www.composent.com/index.html&quot;);
	 * </pre>
	 * 
	 * Must not be <code>null</code>.
	 * @param transferListener
	 *            a listener for file transfer events. Must not be null.  Must not be null.  See <b>Note</b> above.
	 * @param options
	 *            a Map of options associated with sendRetrieveRequest. The
	 *            particular name/value pairs will be unique to the individual
	 *            providers. May be <code>null</code>.
	 * @throws IncomingFileTransferException
	 *             if the provider is not connected or is not in the correct
	 *             state for initiating file transfer
	 */
	public void sendRetrieveRequest(IFileID remoteFileID, IFileTransferListener transferListener, Map options) throws IncomingFileTransferException;

	/**
	 * Send request for transfer of a remote file to local file storage. This
	 * method is used to initiate a file retrieve for a remoteFileID (first
	 * parameter). File transfer events are asynchronously delivered a file
	 * transfer listener (third parameter). The given remoteFileID and
	 * transferListener must not be null.
	 * <p>
	 * <b>NOTE</b>: if this method completes successfully, the given transferListener 
	 * will be asynchronously notified via an IIncomingFileTransferReceiveDoneEvent 
	 * (along with other possible events).  All implementations are required to 
	 * issue this event whether successful or failed.  Listeners
	 * can consult {@link IIncomingFileTransferReceiveDoneEvent#getException()} to 
	 * determine whether the transfer operation completed successfully.
	 * </p>
	 * @param remoteFileID
	 *            reference to the remote target file (e.g.
	 *            http://www.eclipse.org/index.html) or a reference to a
	 *            resource that specifies the location of a target file.
	 *            Implementing providers will determine what protocol schemes
	 *            are supported (e.g. ftp, http, torrent, file, etc) and the
	 *            required format of the scheme-specific information. If a
	 *            protocol is specified that is not supported, or the
	 *            scheme-specific information is not well-formed, then an
	 *            IncomingFileTransferException will be thrown. Typically,
	 *            callers will create IFileID instances via calls such as:
	 * 
	 * <pre>
	 * IFileID remoteFileID = FileIDFactory.getDefault().createID(
	 * 		ftc.getRetrieveNamespace(), &quot;http://www.composent.com/index.html&quot;);
	 * </pre>
	 * 
	 * Must not be <code>null</code>.
	 * @param rangeSpecification a range specification for retrieving a portion of the given
	 * remote file.  If <code>null</code> the entire file will be retrieved (as per {@link #sendRetrieveRequest(IFileID, IFileTransferListener, Map)}.
	 * If non-<code>null</code> the given file range will be used to retrieve the given file.  For example, if the
	 * rangeSpecification has a start value of 1 and end value of 3, and the total length of the file is
	 * 5 bytes with content [a, b, c, d, e], a successful retrieve request would transfer bytes 'b', 'c', and 'd', but not 'a', and 'e'.
	 * @param transferListener
	 *            a listener for file transfer events. Must not be null.  See <b>Note</b> above.
	 * @param options
	 *            a Map of options associated with sendRetrieveRequest. The
	 *            particular name/value pairs will be unique to the individual
	 *            providers. May be <code>null</code>.
	 * @throws IncomingFileTransferException
	 *             if the provider is not connected or is not in the correct
	 *             state for initiating file transfer
	 */
	public void sendRetrieveRequest(IFileID remoteFileID, IFileRangeSpecification rangeSpecification, IFileTransferListener transferListener, Map options) throws IncomingFileTransferException;

	/**
	 * Get namespace to be used for creation of remoteFileID for retrieve
	 * request. Result typically used as first parameter for
	 * {@link IDFactory#createID(Namespace, String)} to be used as first in
	 * {@link #sendRetrieveRequest(IFileID, IFileTransferListener, Map)}
	 * 
	 * @return Namespace to use for ID creation via
	 *         {@link IDFactory#createID(Namespace, String)}. Will not be
	 *         <code>null</code>.
	 */
	public Namespace getRetrieveNamespace();

	/**
	 * Set connect context for authentication upon subsequent
	 * {@link #sendRetrieveRequest(IFileID, IFileTransferListener, Map)}. This
	 * method should be called with a non-null connectContext in order to allow
	 * authentication to occur during call to
	 * {@link #sendRetrieveRequest(IFileID, IFileTransferListener, Map)}.
	 * 
	 * @param connectContext
	 *            the connect context to use for authenticating during
	 *            subsequent call to
	 *            {@link #sendRetrieveRequest(IFileID, IFileTransferListener, Map)}.
	 *            If <code>null</code>, then no authentication will be
	 *            attempted.
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext);

	/**
	 * Set proxy for use upon subsequent
	 * {@link #sendRetrieveRequest(IFileID, IFileTransferListener, Map)}. This
	 * method should be called with proxy to allow the given proxy to
	 * be used in subsequent calls to
	 * {@link #sendRetrieveRequest(IFileID, IFileTransferListener, Map)}.
	 * <p>
	 * When proxy is <code>null</code> or has not been called providers must use
	 * the <code>org.eclipse.core.net</code> proxy API to obtain proxy information 
	 * and proxy credentials if they support proxies of the type(s) supported by
	 * that API. The API is provided by an OSGi service of type
	 * <code>org.eclipse.core.net.proxy.IProxyService</code>.
	 * </p><p>
	 * If no information is available via <code>IProxyService</code> 
	 * providers may use other defaults. 
	 * </p>
	 * 
	 * @param proxy
	 *            the proxy to use for subsequent calls to
	 *            {@link #sendRetrieveRequest(IFileID, IFileTransferListener, Map)}.
	 *            If <code>null</code>, then proxy information is obtained from
	 *            <code>IProxyService</code> if available. Otherwise provider
	 *            defined defaults may be used. 
	 */
	public void setProxy(Proxy proxy);
}
