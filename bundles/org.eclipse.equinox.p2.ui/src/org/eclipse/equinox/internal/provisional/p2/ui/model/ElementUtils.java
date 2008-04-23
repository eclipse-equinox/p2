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

package org.eclipse.equinox.internal.provisional.p2.ui.model;

import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
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
	public static IInstallableUnit[] getIUs(Object[] elements) {
		if (elements == null || elements.length == 0)
			return new IInstallableUnit[0];
		Set iuChildren = new HashSet(elements.length);
		for (int i = 0; i < elements.length; i++) {
			iuChildren.addAll(getIUs(elements[i]));
		}
		return (IInstallableUnit[]) iuChildren.toArray(new IInstallableUnit[iuChildren.size()]);
	}

	public static Set getIUs(Object element) {
		Set ius = new HashSet();
		// Check first for a container.  Elements like categories are both
		// a container and an IU, and the container aspect is what we want
		// when we want to find out which IU's to manipulate.
		if (element instanceof IUContainerElement) {
			ius.addAll(Arrays.asList(((IUContainerElement) element).getIUs()));
		} else if (element instanceof IUElement) {
			ius.add(((IUElement) element).getIU());
		} else {
			IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);
			if (iu != null)
				ius.add(iu);
		}
		return ius;
	}

	public static void updateRepositoryUsingElements(final MetadataRepositoryElement[] elements, final Shell shell) {
		Job job = new Job("Updating Repository Information") {
			public IStatus run(IProgressMonitor monitor) {
				ProvUI.startBatchOperation();
				try {
					URL[] currentlyEnabled = ProvisioningUtil.getMetadataRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);
					URL[] currentlyDisabled = ProvisioningUtil.getMetadataRepositories(IMetadataRepositoryManager.REPOSITORIES_DISABLED);
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
