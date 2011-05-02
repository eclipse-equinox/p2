/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import org.eclipse.core.runtime.IStatus;

public interface ILog {

	// Log a status
	public void log(IStatus status);

	public void log(String message);

	// Notify that logging is completed & cleanup resources 
	public void close();
}
