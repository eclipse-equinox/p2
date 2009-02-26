/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.net.URI;

/**
 * Abstract class representing an operation that removes repositories.
 * 
 * @since 3.5
 */
public abstract class RemoveRepositoryOperation extends RepositoryOperation {

	public RemoveRepositoryOperation(String label, URI[] locations) {
		super(label, locations);
	}

}
