/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.model;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.swt.widgets.Shell;

/**
 * Utility methods for manipulating model elements.
 * 
 * @since 3.4
 *
 */
public class ElementUtils {

	public static void updateRepositoryUsingElements(final ProvisioningUI ui, final MetadataRepositoryElement[] elements, final Shell shell) {
		ui.signalRepositoryOperationStart();
		IMetadataRepositoryManager metaManager = ProvUI.getMetadataRepositoryManager(ui.getSession());
		IArtifactRepositoryManager artManager = ProvUI.getArtifactRepositoryManager(ui.getSession());
		try {
			int visibilityFlags = ui.getRepositoryTracker().getMetadataRepositoryFlags();
			URI[] currentlyEnabled = metaManager.getKnownRepositories(visibilityFlags);
			URI[] currentlyDisabled = metaManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED | visibilityFlags);
			for (int i = 0; i < elements.length; i++) {
				URI location = elements[i].getLocation();
				if (elements[i].isEnabled()) {
					if (containsURI(currentlyDisabled, location))
						// It should be enabled and is not currently
						setColocatedRepositoryEnablement(ui, location, true);
					else if (!containsURI(currentlyEnabled, location)) {
						// It is not known as enabled or disabled.  Add it.
						metaManager.addRepository(location);
						artManager.addRepository(location);
					}
				} else {
					if (containsURI(currentlyEnabled, location))
						// It should be disabled, and is currently enabled
						setColocatedRepositoryEnablement(ui, location, false);
					else if (!containsURI(currentlyDisabled, location)) {
						// It is not known as enabled or disabled.  Add it and then disable it.
						metaManager.addRepository(location);
						artManager.addRepository(location);
						setColocatedRepositoryEnablement(ui, location, false);
					}
				}
				String name = elements[i].getName();
				if (name != null && name.length() > 0) {
					metaManager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
					artManager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
				}
			}
			// Are there any elements that need to be deleted?  Go over the original state
			// and remove any elements that weren't in the elements we were given
			Set<String> nowKnown = new HashSet<String>();
			for (int i = 0; i < elements.length; i++)
				nowKnown.add(URIUtil.toUnencodedString(elements[i].getLocation()));
			for (int i = 0; i < currentlyEnabled.length; i++) {
				if (!nowKnown.contains(URIUtil.toUnencodedString(currentlyEnabled[i]))) {
					metaManager.removeRepository(currentlyEnabled[i]);
					artManager.removeRepository(currentlyEnabled[i]);
				}
			}
			for (int i = 0; i < currentlyDisabled.length; i++) {
				if (!nowKnown.contains(URIUtil.toUnencodedString(currentlyDisabled[i]))) {
					metaManager.removeRepository(currentlyDisabled[i]);
					artManager.removeRepository(currentlyDisabled[i]);
				}
			}
		} finally {
			ui.signalRepositoryOperationComplete(null, true);
		}
	}

	private static void setColocatedRepositoryEnablement(ProvisioningUI ui, URI location, boolean enable) {
		ProvUI.getArtifactRepositoryManager(ui.getSession()).setEnabled(location, enable);
		ProvUI.getMetadataRepositoryManager(ui.getSession()).setEnabled(location, enable);
	}

	public static IInstallableUnit getIU(Object element) {
		if (element instanceof IInstallableUnit)
			return (IInstallableUnit) element;
		if (element instanceof IIUElement)
			return ((IIUElement) element).getIU();
		return ProvUI.getAdapter(element, IInstallableUnit.class);
	}

	public static List<IInstallableUnit> elementsToIUs(Object[] elements) {
		ArrayList<IInstallableUnit> theIUs = new ArrayList<IInstallableUnit>(elements.length);
		for (int i = 0; i < elements.length; i++) {
			IInstallableUnit iu = ProvUI.getAdapter(elements[i], IInstallableUnit.class);
			if (iu != null)
				theIUs.add(iu);
		}
		return theIUs;
	}

	public static IInstallableUnit elementToIU(Object selectedElement) {
		return ProvUI.getAdapter(selectedElement, IInstallableUnit.class);
	}

	static boolean containsURI(URI[] locations, URI url) {
		for (int i = 0; i < locations.length; i++)
			if (locations[i].equals(url))
				return true;
		return false;
	}
}
