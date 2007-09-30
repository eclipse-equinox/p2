/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.prov.ui.operations;

import java.net.URL;

/**
 * Abstract class representing provisioning repository operations
 * 
 * @since 3.4
 */
public abstract class RepositoryOperation extends ProvisioningOperation {

	URL[] urls;
	String[] names;

	RepositoryOperation(String label, URL[] urls, String[] names) {
		super(label);
		this.urls = urls;
		this.names = names;
	}

	public boolean canExecute() {
		return urls != null;
	}

	public boolean canUndo() {
		return urls != null;
	}
}
