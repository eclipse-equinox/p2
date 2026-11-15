/****************************************************************************
 * Copyright (c) 2008 Composent, Inc. and others.
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

package org.eclipse.ecf.provider.filetransfer.retrieve;

import java.util.StringTokenizer;

/**
 *
 */
public class HttpHelper {

	public static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition"; //$NON-NLS-1$

	public static String getRemoteFileNameFromContentDispositionHeader(String headerValue) {
		if (headerValue != null) {
			StringTokenizer tokens = new StringTokenizer(headerValue, " \t\n\r\f=;,"); //$NON-NLS-1$
			while (tokens.hasMoreTokens()) {
				String token = tokens.nextToken();
				if (token.equals("filename") && tokens.hasMoreTokens()) { //$NON-NLS-1$
					// Expect next token to be the filename
					String fileName = tokens.nextToken();
					if (fileName.startsWith("\"") && fileName.endsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
						fileName = fileName.substring(1, fileName.length() - 1);
					return fileName;
				}
			}
		}
		return null;
	}
}
