/****************************************************************************
 * Copyright (c) 2004, 2007 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.provider.filetransfer.identity;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDCreateException;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.internal.provider.filetransfer.Activator;
import org.eclipse.ecf.internal.provider.filetransfer.Messages;

/**
 * URL file namespace class. This defines a namespace that understands how to
 * create IFileID instances from arbitary URLs
 */
public class FileTransferNamespace extends Namespace {

	private static final long serialVersionUID = 8204058147686930765L;

	public static final String PROTOCOL = Messages.FileTransferNamespace_Namespace_Protocol;

	private String getInitFromExternalForm(Object[] args) {
		if (args == null || args.length < 1 || args[0] == null)
			return null;
		if (args[0] instanceof String) {
			String arg = (String) args[0];
			if (arg.startsWith(getScheme() + Namespace.SCHEME_SEPARATOR)) {
				int index = arg.indexOf(Namespace.SCHEME_SEPARATOR);
				if (index >= arg.length())
					return null;
				return arg.substring(index + 1);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.identity.Namespace#createInstance(java.lang.Object[])
	 */
	public ID createInstance(Object[] args) throws IDCreateException {
		try {
			String init = getInitFromExternalForm(args);
			if (init != null)
				return new FileTransferID(this, new URI(init));
			if (args[0] instanceof URL)
				return new FileTransferID(this, (URL) args[0]);
			if (args[0] instanceof String)
				return new FileTransferID(this, new URI((String) args[0]));
			if (args[0] instanceof URI)
				return new FileTransferID(this, (URI) args[0]);
		} catch (Exception e) {
			throw new IDCreateException(Messages.FileTransferNamespace_Exception_Create_Instance, e);
		}
		throw new IDCreateException(Messages.FileTransferNamespace_Exception_Create_Instance_Failed);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.identity.Namespace#getSupportedSchemes()
	 */
	public String[] getSupportedSchemes() {
		Set result = new HashSet();
		String[] platformSchemes = Activator.getDefault().getPlatformSupportedSchemes();
		result.addAll(Arrays.asList(platformSchemes));
		return (String[]) result.toArray(new String[] {});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.identity.Namespace#getScheme()
	 */
	public String getScheme() {
		return PROTOCOL;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.identity.Namespace#getSupportedParameterTypesForCreateInstance()
	 */
	public Class[][] getSupportedParameterTypes() {
		return new Class[][] {{URL.class}, {String.class}, {URI.class}};
	}

}
