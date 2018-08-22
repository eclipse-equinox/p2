/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.dnd.*;

/**
 * Defines drop behavior for selected IUs to mean install the IU on the target
 * profile.
 * 
 * @since 3.4
 * 
 */
public class InstallIUDropAdapter extends ViewerDropAdapter {

	static boolean DEBUG = false;

	/**
	 * Constructs a new drop adapter.
	 * 
	 * @param viewer
	 *            the navigator's viewer
	 */
	public InstallIUDropAdapter(StructuredViewer viewer) {
		super(viewer);
	}

	/**
	 * Returns an error status with the given info.
	 */
	IStatus error(String message) {
		return error(message, null);
	}

	/**
	 * Returns an error status with the given info.
	 */
	IStatus error(String message, Throwable exception) {
		return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, message, exception);
	}

	/**
	 * Returns the target profile id for the drop. If the drop is positioned on an
	 * IU, return its parent profile id.
	 */
	private String getProfileTarget(Object mouseTarget) {
		IProfile profile = ProvUI.getAdapter(mouseTarget, IProfile.class);
		if (profile != null) {
			return profile.getProfileId();
		}

		if (mouseTarget instanceof InstalledIUElement) {
			return ((InstalledIUElement) mouseTarget).getProfileId();
		}
		return null;
	}

	/**
	 * Returns an status indicating success.
	 */
	private IStatus ok() {
		return new Status(IStatus.OK, ProvUIActivator.PLUGIN_ID, 0, null, null);
	}

	/**
	 * Perform the drop.
	 * 
	 * @see org.eclipse.swt.dnd.DropTargetListener#drop(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	@Override
	public boolean performDrop(final Object data) {
		if (DEBUG) {
			System.out.println("Perform drop on target: " + getCurrentTarget() + " with data: " + data); //$NON-NLS-1$//$NON-NLS-2$
		}
		if (getCurrentTarget() == null || data == null)
			return false;

		ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
		if (!(selection instanceof IStructuredSelection) || selection.isEmpty())
			return false;

		String profileId = getProfileTarget(getCurrentTarget());
		if (getCurrentOperation() == DND.DROP_COPY && profileId != null) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			ISelectionProvider selectionProvider = new ISelectionProvider() {

				@Override
				public void addSelectionChangedListener(ISelectionChangedListener listener) {
					// Ignore because the selection won't change 
				}

				@Override
				public ISelection getSelection() {
					if (DEBUG) {
						System.out.println("Selection was queried by action"); //$NON-NLS-1$
						System.out.println(structuredSelection.toString());
					}
					return structuredSelection;
				}

				@Override
				public void removeSelectionChangedListener(ISelectionChangedListener listener) {
					// ignore because the selection is static
				}

				@Override
				public void setSelection(ISelection sel) {
					throw new UnsupportedOperationException("This ISelectionProvider is static, and cannot be modified."); //$NON-NLS-1$
				}
			};
			InstallAction action = new InstallAction(ProvAdminUIActivator.getDefault().getProvisioningUI(profileId), selectionProvider);
			if (DEBUG)
				System.out.println("Running install action"); //$NON-NLS-1$
			action.run();
			return true;
		}
		return false;
	}

	/**
	 * Validate whether the drop is valid for the target
	 */
	@Override
	public boolean validateDrop(Object target, int dragOperation, TransferData transferType) {

		if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
			IStatus status = validateTarget(target, transferType);
			if (DEBUG) {
				System.out.println("Validate target: " + status); //$NON-NLS-1$
			}
			return status.isOK();
		}
		return false;
	}

	/*
	 * Overridden to force a copy when the drag is valid.
	 */
	@Override
	public void dragEnter(DropTargetEvent event) {
		event.detail = DND.DROP_COPY;
		super.dragEnter(event);
	}

	/**
	 * Ensures that the drop target meets certain criteria
	 */
	private IStatus validateTarget(Object target, TransferData transferType) {
		if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
			IInstallableUnit[] ius = getSelectedIUs();

			if (ius.length == 0) {
				return error(ProvUIMessages.ProvDropAdapter_NoIUsToDrop);
			}
			if (getProfileTarget(target) != null) {
				return ok();
			}
			return error(ProvUIMessages.ProvDropAdapter_InvalidDropTarget);
		}
		return error(ProvUIMessages.ProvDropAdapter_UnsupportedDropOperation);
	}

	/**
	 * Returns the resource selection from the LocalSelectionTransfer.
	 * 
	 * @return the resource selection from the LocalSelectionTransfer
	 */
	private IInstallableUnit[] getSelectedIUs() {
		ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
		List<IInstallableUnit> ius = new ArrayList<>();

		if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
			return null;
		}
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;

		Iterator<?> iter = structuredSelection.iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
			if (iu != null) {
				ius.add(iu);
			}
		}
		return ius.toArray(new IInstallableUnit[ius.size()]);
	}
}
