/****************************************************************************
 * Copyright (c) 2007 Composent, Inc. and others.
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

package org.eclipse.ecf.filetransfer;

/**
 * An object that describes a file range specification.  Object implementations of this
 * class can be 
 */
public interface IFileRangeSpecification {

	/**
	 * Get the start position to start from.  The position is in bytes, and byte 0 is the first byte
	 * of the file, N-1 is the last position in the file, where N is the length of the file in bytes.  
	 * @return the position in the file (in bytes) to start from.  If the returned start position is
	 * less than 0, or equal to or greater than N, then it is an invalid range specification and
	 * when used in {@link IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID, IFileRangeSpecification, IFileTransferListener, java.util.Map)} will result in a 
	 * {@link InvalidFileRangeSpecificationException}.
	 * 
	 * @see IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID, IFileRangeSpecification, IFileTransferListener, java.util.Map)
	 * @see #getEndPosition()
	 */
	public long getStartPosition();

	/**
	 * Get the end position of transfer range.  The position is in bytes, and byte 0 is the first byte
	 * of the file, N-1 is the last position in the file, where N is the length of the file in bytes.  
	 * @return the position in the file (in bytes) to indicate the end of range to retrieve.  If equal to -1,
	 * then this means that no end position is specified, and the download will continue to the end of file.  If gt or eq 0,
	 * but less than the {@link #getStartPosition()} then this range specification is invalid.  If greater than or
	 * equal to N (where N is length of the file in bytes), then the remaining part of the given file will
	 * be downloaded.  If both {@link #getStartPosition()} and {@link #getEndPosition()} are valid, then
	 * the number of bytes downloaded will be <code>(endPosition - startPosition) + 1</code>. So, for example:
	 * <pre>
	 * For a fileLength = 20
	 * 
	 * startPosition = 10
	 * endPosition = 19
	 * bytesDownloaded = 10
	 * 
	 * startPosition = 0
	 * endPosition = -1
	 * bytesDownloaded = 20
	 * 
	 * startPosition = 5
	 * endPosition = 3
	 * invalid range
	 * 
	 * startPosition = 5
	 * endPosition = 6
	 * bytesDownloaded = 2
	 * 
	 * startPosition = 5
	 * endPosition = -1
	 * bytesDownloaded = 15
	 * 
	 * </pre>
	 * 
	 * @see IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID, IFileRangeSpecification, IFileTransferListener, java.util.Map)
	 * @see #getStartPosition()
	 */
	public long getEndPosition();

}
