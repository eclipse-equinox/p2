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

import java.io.File;
import java.util.Map;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.events.*;
import org.eclipse.ecf.filetransfer.identity.IFileID;

/**
 * Entry point outgoing file transfer container adapter. This adapter interface
 * allows providers to expose file sending semantics to clients in a transport
 * independent manner. To be used, a non-null adapter reference must be returned
 * from a call to {@link IContainer#getAdapter(Class)}. Once a non-null
 * reference is retrieved, then it may be used to request to send a file to a
 * remote user. Events will then be asynchronously delivered to the provided
 * listener to complete file transfer.
 * <p>
 * To request and initiate sending a local file to a remote user:
 * 
 * <pre>
 *      // Get ISendFileTransferContainerAdapter adapter
 *       ISendFileTransferContainerAdapter ftc = (ISendFileTransferContainerAdapter) container.getAdapter(ISendFileTransferContainerAdapter.class);
 *       if (ftc != null) {
 *         // Create listener for receiving/responding to asynchronous file transfer events
 *     	     IFileTransferListener listener = new IFileTransferListener() {
 *     		     public void handleTransferEvent(IFileTransferEvent event) {
 *                   // If this event is a response to the transfer request, check if file transfer rejected
 *                   if (event instanceof IOutgoingFileTransferResponseEvent) {
 *                       IOutgoingFileTransferResponseEvent oftr = (IOutgoingFileTransferResponseEvent) event;
 *                       if (!oftr.requestAccepted()) {
 *                           // request rejected...tell user
 *                       }
 *                   }
 *     		     }
 *     	     };
 *           // Specify the target file ID
 *           // This following specifies the path:  ~/path/filename.ext
 *           ID targetID = FileIDFactory.getDefault().createFileID(ftc.getOutgoingNamespace(),new URL(&quot;scp://user@host/path/filename.ext&quot;));
 *           // This following specifies the path:  /path/filename.ext
 *           // ID targetID = FileIDFactory.getDefault().createFileID(ftc.getOutgoingNamespace(),new URL(&quot;scp://user@host//path/filename.ext&quot;));
 *           // Specify the local file to send
 *           File localFileToSend = new File(&quot;filename&quot;);
 *           // Actually send outgoing file request to remote user.  
 *           ftc.sendOutgoingRequest(targetID, localFileToSend, listener, null);
 *       }
 * </pre>
 * 
 * <b>For the sender</b> the delivered events will be:
 * <ul>
 * <li>{@link IOutgoingFileTransferResponseEvent}</li>
 * <li>{@link IOutgoingFileTransferSendDataEvent}</li>
 * <li>{@link IOutgoingFileTransferSendDoneEvent}</li>
 * </ul>
 * and <b>for the {@link IIncomingFileTransferRequestListener}</b> events
 * delivered will be:
 * <ul>
 * <li>{@link IFileTransferRequestEvent}</li>
 * <li>{@link IIncomingFileTransferReceiveDataEvent}</li>
 * <li>{@link IIncomingFileTransferReceiveDoneEvent}</li>
 * </ul>
 */
public interface ISendFileTransferContainerAdapter extends IAdaptable {
	/**
	 * Send request for outgoing file transfer. This method is used to initiate
	 * a file transfer to a targetReceiver (first parameter) of the
	 * localFileToSend (second parameter). File transfer events are
	 * asynchronously delivered to the file transferListener (third parameter)
	 * 
	 * @param targetReceiver
	 *            the ID of the remote to receive the file transfer request.
	 *            Must not be should not be <code>null</code>.
	 * @param localFileToSend
	 *            the {@link IFileTransferInfo} for the local file to send. Must
	 *            not be should not be <code>null</code>.
	 * @param transferListener
	 *            a {@link IFileTransferListener} for responding to file
	 *            transfer events. Must not be should not be <code>null</code>..
	 *            If the target receiver responds then an
	 *            {@link IOutgoingFileTransferResponseEvent} will be delivered
	 *            to the listener
	 * @param options
	 *            a Map of options associated with sendOutgoingRequest. The
	 *            particular name/value pairs will be unique to the individual
	 *            providers. May be should not be <code>null</code>..
	 * @throws SendFileTransferException
	 *             if the provider is not connected or is not in the correct
	 *             state for initiating file transfer
	 */
	public void sendOutgoingRequest(IFileID targetReceiver, IFileTransferInfo localFileToSend, IFileTransferListener transferListener, Map options) throws SendFileTransferException;

	/**
	 * Send request for outgoing file transfer. This method is used to initiate
	 * a file transfer to a targetReceiver (first parameter) of the
	 * localFileToSend (second parameter). File transfer events are
	 * asynchronously delivered to the file transferListener (third parameter)
	 * 
	 * @param targetReceiver
	 *            the ID of the remote to receive the file transfer request.
	 *            Must not be <code>null</code>.
	 * @param localFileToSend
	 *            the {@link File} for the local file to send. Must not be
	 *            <code>null</code>.
	 * @param transferListener
	 *            a {@link IFileTransferListener} for responding to file
	 *            transfer events. Must not be <code>null</code>. If the
	 *            target receiver responds then an IOutgoingFileTransfer will be
	 *            delivered to the listener
	 * @param options
	 *            a Map of options associated with sendOutgoingRequest. The
	 *            particular name/value pairs will be unique to the individual
	 *            providers. May be <code>null</code>.
	 * @throws SendFileTransferException
	 *             if the provider is not connected or is not in the correct
	 *             state for initiating file transfer
	 */
	public void sendOutgoingRequest(IFileID targetReceiver, File localFileToSend, IFileTransferListener transferListener, Map options) throws SendFileTransferException;

	/**
	 * Add incoming file transfer listener. If the underlying provider supports
	 * receiving file transfer requests
	 * 
	 * @param listener
	 *            to receive incoming file transfer request events. Must not be
	 *            <code>null</code>.
	 */
	public void addListener(IIncomingFileTransferRequestListener listener);

	/**
	 * Remove incoming file transfer listener
	 * 
	 * @param listener
	 *            the listener to remove. Must not be <code>null</code>.
	 * @return true if listener actually removed, false otherwise
	 */
	public boolean removeListener(IIncomingFileTransferRequestListener listener);

	/**
	 * Get namespace for outgoing file transfer.
	 * @return Namespace for outgoing IFileID instances.  Will not return <code>null</code>.
	 */
	public Namespace getOutgoingNamespace();

	/**
	 * Set connect context for authentication upon subsequent
	 * {@link #sendOutgoingRequest(IFileID, IFileTransferInfo, IFileTransferListener, Map)}. This
	 * method should be called with a non-null connectContext in order to allow
	 * authentication to occur during call to
	 * {@link #sendOutgoingRequest(IFileID, IFileTransferInfo, IFileTransferListener, Map)}.
	 * 
	 * @param connectContext
	 *            the connect context to use for authenticating during
	 *            subsequent call to
	 *            {@link #sendOutgoingRequest(IFileID, IFileTransferInfo, IFileTransferListener, Map)}.
	 *            If <code>null</code>, then no authentication will be
	 *            attempted.
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext);

	/**
	 * Set proxy for use upon subsequent
	 * {@link #sendOutgoingRequest(IFileID, IFileTransferInfo, IFileTransferListener, Map)}. This
	 * method should be called with a non-null proxy to allow the given proxy to
	 * be used in subsequent calls to
	 * {@link #sendOutgoingRequest(IFileID, IFileTransferInfo, IFileTransferListener, Map)}.
	 * 
	 * @param proxy
	 *            the proxy to use for subsequent calls to
	 *            {@link #sendOutgoingRequest(IFileID, IFileTransferInfo, IFileTransferListener, Map)}.
	 *            If <code>null</code>, then no proxy will be used.
	 */
	public void setProxy(Proxy proxy);

}
