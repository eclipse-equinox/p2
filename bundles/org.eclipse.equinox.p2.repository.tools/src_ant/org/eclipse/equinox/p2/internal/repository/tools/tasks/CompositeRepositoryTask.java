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

import java.util.Iterator;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.CompositeRepositoryApplication;

public class CompositeRepositoryTask extends AbstractRepositoryTask {

	public CompositeRepositoryTask() {
		application = new CompositeRepositoryApplication();
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			IStatus result = application.run(null);
			if (result.matches(IStatus.ERROR)) {
				throw new BuildException(TaskHelper.statusToString(result, null).toString());
			}
		} catch (ProvisionException e) {
			throw new BuildException(e);
		}
	}

	/*
	 * Add the listed repositories to the composite repository
	 */
	public void addConfiguredAdd(RepositoryList list) {
		for (Iterator iter = list.getRepositoryList().iterator(); iter.hasNext();) {
			DestinationRepository repo = (DestinationRepository) iter.next();
			((CompositeRepositoryApplication) application).addChild(repo.getDescriptor());
		}
	}

	/*	
	 * Remove the listed repositories from the composite repository
	 */
	public void addConfiguredRemove(RepositoryList list) {
		for (Iterator iter = list.getRepositoryList().iterator(); iter.hasNext();) {
			DestinationRepository repo = (DestinationRepository) iter.next();
			((CompositeRepositoryApplication) application).removeChild(repo.getDescriptor());
		}
	}

	/*
	 * Set whether the task should fail if the repository already exists
	 */
	public void setFailOnExists(boolean value) {
		((CompositeRepositoryApplication) application).setFailOnExists(value);
	}
}
