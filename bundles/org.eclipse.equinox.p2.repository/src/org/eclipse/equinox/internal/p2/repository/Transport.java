/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.io.OutputStream;
import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public abstract class Transport {

	/**
	 * @param toDownload
	 * @param target
	 * @param pm
	 * @return IStatus describing outcome of the download
	 */
	public abstract IStatus download(URI toDownload, OutputStream target, IProgressMonitor pm);
}
