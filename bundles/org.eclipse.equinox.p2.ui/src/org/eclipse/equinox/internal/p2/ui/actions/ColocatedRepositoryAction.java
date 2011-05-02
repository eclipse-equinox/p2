/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.actions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;

public abstract class ColocatedRepositoryAction extends ProvisioningAction {

	public ColocatedRepositoryAction(ProvisioningUI ui, String label, String tooltipText, ISelectionProvider selectionProvider) {
		super(ui, label, selectionProvider);
		setToolTipText(tooltipText);
		init();
	}

	protected URI[] getSelectedLocations(Object[] selectionArray) {
		List<URI> urls = new ArrayList<URI>();
		for (int i = 0; i < selectionArray.length; i++) {
			if (selectionArray[i] instanceof MetadataRepositoryElement)
				urls.add(((MetadataRepositoryElement) selectionArray[i]).getLocation());
		}
		return urls.toArray(new URI[urls.size()]);
	}

	protected void checkEnablement(Object[] selectionArray) {
		setEnabled(getSelectedLocations(selectionArray).length > 0);
	}
}
