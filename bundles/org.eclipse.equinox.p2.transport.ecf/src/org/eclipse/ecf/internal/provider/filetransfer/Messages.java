/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.internal.provider.filetransfer;

import org.eclipse.osgi.util.NLS;

/**
 * 
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ecf.internal.provider.filetransfer.messages"; //$NON-NLS-1$
	public static String AbstractOutgoingFileTransfer_EXCEPTION_FILE_TRANSFER_INFO_NOT_NULL;
	public static String AbstractOutgoingFileTransfer_EXCEPTION_IN_FINALLY;
	public static String AbstractRetrieveFileTransfer_TransferRateFormat;
	public static String AbstractRetrieveFileTransfer_MalformedURLException;
	public static String AbstractRetrieveFileTransfer_Progress_Data;
	public static String AbstractRetrieveFileTransfer_EXCEPTION_IN_FINALLY;
	public static String AbstractRetrieveFileTransfer_Exception_User_Cancelled;
	public static String AbstractRetrieveFileTransfer_InfoTransferRate;
	public static String AbstractRetrieveFileTransfer_RemoteFileID_Not_Null;
	public static String AbstractRetrieveFileTransfer_SizeUnitBytes;
	public static String AbstractRetrieveFileTransfer_SizeUnitGB;
	public static String AbstractRetrieveFileTransfer_SizeUnitKB;
	public static String AbstractRetrieveFileTransfer_SizeUnitMB;
	public static String AbstractRetrieveFileTransfer_Status_Transfer_Completed_OK;
	public static String AbstractRetrieveFileTransfer_Status_Transfer_Exception;
	public static String AbstractRetrieveFileTransfer_TransferListener_Not_Null;
	public static String AbstractOutgoingFileTransfer_MalformedURLException;
	public static String AbstractOutgoingFileTransfer_Progress_Data;
	public static String AbstractOutgoingFileTransfer_Exception_User_Cancelled;
	public static String AbstractOutgoingFileTransfer_RemoteFileID_Not_Null;
	public static String AbstractOutgoingFileTransfer_Status_Transfer_Completed_OK;
	public static String AbstractOutgoingFileTransfer_Status_Transfer_Exception;
	public static String AbstractOutgoingFileTransfer_TransferListener_Not_Null;
	public static String UrlConnectionRetrieveFileTransfer_RESUME_START_ERROR;
	public static String UrlConnectionRetrieveFileTransfer_INVALID_SERVER_RESPONSE_TO_PARTIAL_RANGE_REQUEST;
	public static String UrlConnectionRetrieveFileTransfer_RESUME_ERROR_END_POSITION_LESS_THAN_START;
	public static String UrlConnectionRetrieveFileTransfer_CONNECT_EXCEPTION_NOT_CONNECTED;
	public static String UrlConnectionRetrieveFileTransfer_EXCEPTION_FILE_MODIFIED_SINCE_LAST_ACCESS;
	public static String UrlConnectionRetrieveFileTransfer_EXCEPTION_COULD_NOT_CONNECT;
	public static String UrlConnectionRetrieveFileTransfer_EXCEPTION_INVALID_SERVER_RESPONSE;
	public static String UrlConnectionRetrieveFileTransfer_RESUME_START_POSITION_LESS_THAN_ZERO;
	public static String UrlConnectionRetrieveFileTransfer_UnsupportedCallbackException;
	public static String UrlConnectionRetrieveFileTransfer_USERNAME_PROMPT;
	public static String UrlConnectionOutgoingFileTransfer_EXCEPTION_COULD_NOT_CONNECT;
	public static String FileSystemBrowser_EXCEPTION_DIRECTORY_DOES_NOT_EXIST;
	public static String FileTransferNamespace_Exception_Create_Instance;
	public static String FileTransferNamespace_Exception_Create_Instance_Failed;
	public static String FileTransferNamespace_File_Protocol;
	public static String FileTransferNamespace_Ftp_Protocol;
	public static String FileTransferNamespace_Http_Protocol;
	public static String FileTransferNamespace_Https_Protocol;
	public static String FileTransferNamespace_Jar_Protocol;
	public static String FileTransferNamespace_Mailto_Protocol;
	public static String FileTransferNamespace_Gopher_Protocol;
	public static String FileTransferNamespace_Namespace_Protocol;
	public static String FileTransferID_Exception_Url_Not_Null;
	public static String LocalFileOutgoingFileTransfer_EXCEPTION_OPENING_FOR_INPUT;
	public static String LocalFileOutgoingFileTransfer_EXCEPTION_OPENING_FOR_OUTPUT;
	public static String MultiProtocolOutgoingAdapter_EXCEPTION_NO_PROTOCOL_HANDER;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//
	}
}
