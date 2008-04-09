/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.core;

public interface IServiceUI {

	/**
	 * Opens a UI prompt for a username and password.
	 * 
	 * @param location - the location requiring login details, may be <code>null</code>.
	 * @return A two element array containing the username and password, in that orders.
	 * Returns <code>null</code> if the prompt was cancelled.
	 */
	public String[] getUsernamePassword(String location);
}
