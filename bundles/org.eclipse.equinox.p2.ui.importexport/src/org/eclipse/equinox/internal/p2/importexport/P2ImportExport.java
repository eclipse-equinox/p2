/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - Bug 387115 - Allow to export everything
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport;

import java.io.*;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public interface P2ImportExport {
	/**
	 * 
	 * @param input
	 * @return iu listed in the file
	 * @throws IOException
	 * @throws VersionIncompatibleException if the given file version is not supported
	 */
	List<IUDetail> importP2F(InputStream input) throws IOException;

	IStatus exportP2F(OutputStream output, IInstallableUnit[] ius, boolean allowEntriesWithoutRepo, IProgressMonitor monitor);

	IStatus exportP2F(OutputStream output, List<IUDetail> ius, IProgressMonitor monitor);
}
