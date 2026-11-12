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
package org.eclipse.ecf.core.security;

/**
 * A connect context for passing in information to the
 * {@link org.eclipse.ecf.core.IContainer#connect(org.eclipse.ecf.core.identity.ID, IConnectContext)}
 * call.
 * 
 * @see org.eclipse.ecf.core.IContainer#connect(org.eclipse.ecf.core.identity.ID,
 *      IConnectContext)
 * @see ConnectContextFactory
 */
public interface IConnectContext {
	/**
	 * Get the callbackhandler instance used by the provider to callback into
	 * application code. The provider will typically use the callback handler to
	 * provide a set of callbacks for getting/retrieving authorization info
	 * 
	 * @return CallbackHandler
	 */
	public CallbackHandler getCallbackHandler();

}
