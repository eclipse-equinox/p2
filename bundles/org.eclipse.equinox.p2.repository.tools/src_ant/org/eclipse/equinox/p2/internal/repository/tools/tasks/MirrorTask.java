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
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.List;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;

public class MirrorTask extends AbstractRepositoryTask {

	public MirrorTask() {
		application = new MirrorApplication();
	}

	public void execute() throws BuildException {
		try {
			prepareSourceRepos();
			application.initializeRepos(null);
			List ius = prepareIUs();
			if (ius == null || ius.size() == 0)
				throw new BuildException("Need to specify one or more IUs to mirror.");
			application.setSourceIUs(ius);
			IStatus result = application.run(null);
			if (result.matches(IStatus.ERROR)) {
				throw new BuildException(TaskHelper.statusToString(result, null).toString());
			}
		} catch (ProvisionException e) {
			throw new BuildException(e);
		}
	}

	public SlicingOption createSlicingOptions() {
		SlicingOption options = new SlicingOption();
		((MirrorApplication) application).setSlicingOptions(options.getOptions());
		return options;
	}
}
