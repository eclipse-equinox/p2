/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.ui.query.ElementQueryDescriptor;
import org.eclipse.equinox.p2.ui.query.IQueryProvider;
import org.eclipse.equinox.p2.updatechecker.UpdateEvent;

public class UpdateWizardPage extends UpdateOrInstallWizardPage {

	IQueryProvider queryProvider;
	Object[] initialSelections = new Object[0];

	public UpdateWizardPage(IInstallableUnit[] ius, String profileId, IQueryProvider queryProvider, UpdateOrInstallWizard wizard) {
		super("UpdateWizardPage", ius, profileId, wizard); //$NON-NLS-1$
		this.queryProvider = queryProvider;
		setTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		setDescription(ProvUIMessages.UpdateAction_UpdatesAvailableMessage);
	}

	protected void makeElements(IInstallableUnit[] ius, List elements) {
		HashMap uniqueIds = new HashMap();
		for (int i = 0; i < ius.length; i++) {
			UpdateEvent event = new UpdateEvent(getProfileId(), ius);
			ElementQueryDescriptor descriptor = queryProvider.getQueryDescriptor(event, IQueryProvider.AVAILABLE_UPDATES);
			Iterator iter = descriptor.queryable.query(descriptor.query, descriptor.collector, null).iterator();
			ArrayList queryIUs = new ArrayList();
			while (iter.hasNext()) {
				IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
				if (iu != null)
					queryIUs.add(iu);
			}
			IInstallableUnit[] replacements = (IInstallableUnit[]) queryIUs.toArray(new IInstallableUnit[queryIUs.size()]);
			for (int j = 0; j < replacements.length; j++) {
				AvailableUpdateElement element = new AvailableUpdateElement(replacements[j], ius[i], getProfileId());
				elements.add(element);
				AvailableUpdateElement latestElement = (AvailableUpdateElement) uniqueIds.get(replacements[j].getId());
				if (latestElement == null || replacements[j].getVersion().compareTo(latestElement.getIU().getVersion()) > 0)
					uniqueIds.put(replacements[j].getId(), element);

			}
		}
		initialSelections = uniqueIds.values().toArray();
	}

	private IInstallableUnit[] getIUsToReplace(Object[] replacementElements) {
		Set iusToReplace = new HashSet();
		for (int i = 0; i < replacementElements.length; i++) {
			if (replacementElements[i] instanceof AvailableUpdateElement) {
				iusToReplace.add(((AvailableUpdateElement) replacementElements[i]).getIUToBeUpdated());
			}
		}
		return (IInstallableUnit[]) iusToReplace.toArray(new IInstallableUnit[iusToReplace.size()]);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.UpdateIUOperationLabel;
	}

	protected ProfileModificationOperation createProfileModificationOperation(Object[] selectedElements, IProgressMonitor monitor) {
		try {
			ProfileChangeRequest request = new ProfileChangeRequest(getProfileId());
			request.removeInstallableUnits(getIUsToReplace(selectedElements));
			request.addInstallableUnits(elementsToIUs(selectedElements));
			ProvisioningPlan plan = ProvisioningUtil.getProvisioningPlan(request, monitor);
			IStatus status = plan.getStatus();
			if (status.isOK())
				return new ProfileModificationOperation(getOperationLabel(), getProfile().getProfileId(), plan);
			ProvUI.reportStatus(status);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}
		return null;
	}

	protected void setInitialSelections() {
		listViewer.setCheckedElements(initialSelections);
	}
}