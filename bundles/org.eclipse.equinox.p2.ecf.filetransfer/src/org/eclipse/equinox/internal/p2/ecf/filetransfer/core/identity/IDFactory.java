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
package org.eclipse.equinox.internal.p2.ecf.filetransfer.core.identity;

import java.util.Hashtable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.internal.Activator;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.identity.FileTransferNamespace;

/**
 * Factory class for creating ID instances. The only known namespace is the
 * {@link FileTransferNamespace}.
 */
public class IDFactory {

	private static final Hashtable<String, Namespace> namespaces = new Hashtable<>();

	private static final IDFactory instance = new IDFactory();

	static {
		Namespace fileTransferNamespace = new FileTransferNamespace();
		namespaces.put(fileTransferNamespace.getName(), fileTransferNamespace);
	}

	public static IDFactory getDefault() {
		return instance;
	}

	private IDFactory() {
	}

	/**
	 * Get a Namespace instance by its string name.
	 *
	 * @param name the name to use for lookup
	 * @return Namespace instance. Null if not found.
	 */
	public Namespace getNamespaceByName(String name) {
		if (name == null)
			return null;
		return namespaces.get(name);
	}

	private static void logAndThrow(String s) throws IDCreateException {
		IDCreateException e = new IDCreateException(s);
		Activator.getDefault().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, s, e));
		throw e;
	}

	/**
	 * Make a new identity instance from a namespace and the given arguments.
	 *
	 * @param n    the Namespace to use to create the ID
	 * @param args the arguments for the ID
	 * @return new ID instance
	 * @throws IDCreateException if the ID cannot be created
	 */
	public ID createID(Namespace n, Object[] args) throws IDCreateException {
		// Verify namespace is non-null
		if (n == null)
			logAndThrow("Namespace cannot be null"); //$NON-NLS-1$
		// Make sure that namespace is in table of known namespace. If not,
		// throw...we don't create any instances that we don't know about!
		Namespace ns = namespaces.get(n.getName());
		if (ns == null)
			logAndThrow("Namespace " + n.getName() + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
		return ns.createInstance(args);
	}
}
