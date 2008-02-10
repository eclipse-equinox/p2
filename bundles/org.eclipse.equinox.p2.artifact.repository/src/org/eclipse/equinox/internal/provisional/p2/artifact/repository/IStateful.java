/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.artifact.repository;

import org.eclipse.core.runtime.IStatus;

/**
 * Implementing <code>IStateful</code> adds the ability to store state information.
 */
public interface IStateful {

	void setStatus(IStatus status);

	public IStatus getStatus();

}
