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
package org.eclipse.equinox.p2.ui.operations;

import java.net.URL;

/**
 * Abstract class representing provisioning repository operations
 * 
 * @since 3.4
 */
abstract class RepositoryOperation extends UndoableProvisioningOperation {

	URL[] urls;

	RepositoryOperation(String label, URL[] urls) {
		super(label);
		this.urls = urls;
	}

	public boolean canExecute() {
		return urls != null;
	}

	public boolean canUndo() {
		return urls != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation#getAffectedObjects()
	 */
	public Object[] getAffectedObjects() {
		return urls;
	}
}
