/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.model;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.swt.widgets.Shell;

/**
 * Utility methods for manipulating model elements.
 * 
 * @since 3.4
 *
 */
public class ElementUtils {

	public static void updateRepositoryUsingElements(final MetadataRepositoryElement[] elements, final Shell shell) {
		Job job = new Job(ProvUIMessages.ElementUtils_UpdateJobTitle) {
			public IStatus run(IProgressMonitor monitor) {
				ProvUI.startBatchOperation();
				try {
					URL[] currentlyEnabled = ProvisioningUtil.getMetadataRepositories(IRepositoryManager.REPOSITORIES_ALL);
					URL[] currentlyDisabled = ProvisioningUtil.getMetadataRepositories(IRepositoryManager.REPOSITORIES_DISABLED);
					for (int i = 0; i < elements.length; i++) {
						URL location = elements[i].getLocation();
						if (elements[i].isEnabled()) {
							if (containsURL(currentlyDisabled, location))
								// It should be enabled and is not currently
								ProvisioningUtil.setColocatedRepositoryEnablement(location, true);
							else if (!containsURL(currentlyEnabled, location)) {
								// It is not known as enabled or disabled.  Add it.
								ProvisioningUtil.addMetadataRepository(location);
								ProvisioningUtil.addArtifactRepository(location);
							}
						} else {
							if (containsURL(currentlyEnabled, location))
								// It should be disabled, and is currently enabled
								ProvisioningUtil.setColocatedRepositoryEnablement(location, false);
							else if (!containsURL(currentlyDisabled, location)) {
								// It is not known as enabled or disabled.  Add it and then disable it.
								ProvisioningUtil.addMetadataRepository(location);
								ProvisioningUtil.addArtifactRepository(location);
								ProvisioningUtil.setColocatedRepositoryEnablement(location, false);
							}
						}
					}
				} catch (ProvisionException e) {
					return e.getStatus();
				} finally {
					ProvUI.endBatchOperation();
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	static boolean containsURL(URL[] locations, URL url) {
		for (int i = 0; i < locations.length; i++)
			if (locations[i].toExternalForm().equals(url.toExternalForm()))
				return true;
		return false;
	}
}
