/****************************************************************************
 * Copyright (c) 2019 IBM, Composent Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *    Thomas Joiner - HttpClient 4 implementation
 *    Yatta Solutions - HttpClient 4.5 implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.provider.filetransfer.httpclient5;

import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.Activator;

public class HttpClientRetrieveFileTransferFactory implements IRetrieveFileTransferFactory {

	@Override
	public IRetrieveFileTransfer newInstance() {
		return new HttpClientRetrieveFileTransfer(Activator.getDefault().getRetrieveHttpClient());
	}
}
