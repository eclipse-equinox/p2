/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.model;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
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
					int visibilityFlags = Policy.getDefault().getQueryContext().getMetadataRepositoryFlags();
					URI[] currentlyEnabled = ProvisioningUtil.getMetadataRepositories(visibilityFlags);
					URI[] currentlyDisabled = ProvisioningUtil.getMetadataRepositories(IRepositoryManager.REPOSITORIES_DISABLED | visibilityFlags);
					for (int i = 0; i < elements.length; i++) {
						URI location = elements[i].getLocation();
						if (elements[i].isEnabled()) {
							if (containsURI(currentlyDisabled, location))
								// It should be enabled and is not currently
								ProvisioningUtil.setColocatedRepositoryEnablement(location, true);
							else if (!containsURI(currentlyEnabled, location)) {
								// It is not known as enabled or disabled.  Add it.
								ProvisioningUtil.addMetadataRepository(location, false);
								ProvisioningUtil.addArtifactRepository(location, false);
							}
						} else {
							if (containsURI(currentlyEnabled, location))
								// It should be disabled, and is currently enabled
								ProvisioningUtil.setColocatedRepositoryEnablement(location, false);
							else if (!containsURI(currentlyDisabled, location)) {
								// It is not known as enabled or disabled.  Add it and then disable it.
								ProvisioningUtil.addMetadataRepository(location, false);
								ProvisioningUtil.addArtifactRepository(location, false);
								ProvisioningUtil.setColocatedRepositoryEnablement(location, false);
							}
						}
						String name = elements[i].getName();
						if (name != null && name.length() > 0) {
							ProvisioningUtil.setMetadataRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
							ProvisioningUtil.setArtifactRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
						}
					}
					// Are there any elements that need to be deleted?  Go over the original state
					// and remove any elements that weren't in the elements we were given
					Set nowKnown = new HashSet();
					for (int i = 0; i < elements.length; i++)
						nowKnown.add(URIUtil.toUnencodedString(elements[i].getLocation()));
					for (int i = 0; i < currentlyEnabled.length; i++) {
						if (!nowKnown.contains(URIUtil.toUnencodedString(currentlyEnabled[i]))) {
							ProvisioningUtil.removeMetadataRepository(currentlyEnabled[i]);
							ProvisioningUtil.removeArtifactRepository(currentlyEnabled[i]);
						}
					}
					for (int i = 0; i < currentlyDisabled.length; i++) {
						if (!nowKnown.contains(URIUtil.toUnencodedString(currentlyDisabled[i]))) {
							ProvisioningUtil.removeMetadataRepository(currentlyDisabled[i]);
							ProvisioningUtil.removeArtifactRepository(currentlyDisabled[i]);
						}
					}
				} catch (ProvisionException e) {
					return e.getStatus();
				} finally {
					ProvUI.endBatchOperation(true);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	public static IInstallableUnit getIU(Object element) {
		if (element instanceof IInstallableUnit)
			return (IInstallableUnit) element;
		if (element instanceof IIUElement)
			return ((IIUElement) element).getIU();
		return (IInstallableUnit) ProvUI.getAdapter(element, IInstallableUnit.class);
	}

	public static IInstallableUnit[] elementsToIUs(Object[] elements) {
		ArrayList theIUs = new ArrayList(elements.length);
		for (int i = 0; i < elements.length; i++) {
			IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(elements[i], IInstallableUnit.class);
			if (iu != null)
				theIUs.add(iu);
		}
		return (IInstallableUnit[]) theIUs.toArray(new IInstallableUnit[theIUs.size()]);
	}

	static boolean containsURI(URI[] locations, URI url) {
		for (int i = 0; i < locations.length; i++)
			if (locations[i].equals(url))
				return true;
		return false;
	}
}
