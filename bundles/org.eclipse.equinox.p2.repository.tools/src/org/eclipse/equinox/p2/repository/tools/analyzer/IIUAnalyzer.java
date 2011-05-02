/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.repository.tools.analyzer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * The IUAnalaysis Interface.  
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * 
 * Clients are encouraged to extend {@link IUAnalyzer}, an abstract class that implements
 * this interface.
 * 
 * @since 2.0
 * 
 */
public interface IIUAnalyzer {

	public static final String ID = "org.eclipse.equinox.p2.repository.tools.verifier"; //$NON-NLS-1$

	public void preAnalysis(IMetadataRepository repository);

	public IStatus postAnalysis();

	public void analyzeIU(IInstallableUnit iu);

}
