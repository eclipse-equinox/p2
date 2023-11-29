/******************************************************************************* 
* Copyright (c) 2009, 2017 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.repository.tools.analyzer;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.internal.repository.tools.Activator;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * An abstract base class for the Analyzer.  Clients are encouraged to extends this 
 * class when defining IU Analysis extension points.
 * @since 2.0
 */
public abstract class IUAnalyzer implements IIUAnalyzer {

	private List<IStatus> errors = null;
	private String analyzerName;

	public void setName(String name) {
		this.analyzerName = name;
	}

	protected void error(IInstallableUnit iu, String error) {
		if (errors == null)
			errors = new ArrayList<>();
		errors.add(new Status(IStatus.ERROR, Activator.ID, error));
	}

	@Override
	public IStatus postAnalysis() {
		if (errors == null || errors.size() == 0)
			return Status.OK_STATUS;
		return new MultiStatus(Activator.ID, IStatus.ERROR, errors.toArray(new IStatus[errors.size()]), analyzerName, null);
	}
}
