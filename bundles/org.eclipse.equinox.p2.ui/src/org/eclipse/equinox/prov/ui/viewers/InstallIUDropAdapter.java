/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.prov.ui.viewers;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.ui.ProvUIActivator;
import org.eclipse.equinox.prov.ui.actions.InstallAction;
import org.eclipse.equinox.prov.ui.internal.ProvUIMessages;
import org.eclipse.equinox.prov.ui.model.InstalledIUElement;
import org.eclipse.equinox.prov.ui.operations.IOperationConfirmer;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Shell;

/**
 * Defines drop behavior for selected IUs to mean install the IU on the target
 * profile.
 * 
 * @since 3.4
 * 
 */
public class InstallIUDropAdapter extends ViewerDropAdapter {

	static boolean DEBUG = false;
	private IOperationConfirmer confirmer;
	private int entryPointStrategy = InstallAction.ENTRYPOINT_FORCE;

	/**
	 * Constructs a new drop adapter.
	 * 
	 * @param viewer
	 *            the navigator's viewer
	 * @param confirmer
	 *            a confirmer that should be used to confirm the operation, or
	 *            <code>null</code> if no confirmation is necessary.
	 */
	public InstallIUDropAdapter(StructuredViewer viewer, IOperationConfirmer confirmer) {
		super(viewer);
		this.confirmer = confirmer;
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
	 * Returns the target profile for the drop. If the drop is positioned on an
	 * IU, return its parent profile.
	 */
	private Profile getProfileTarget(Object mouseTarget) {
		if (mouseTarget instanceof Profile) {
			return (Profile) mouseTarget;
		}

		if (mouseTarget instanceof InstalledIUElement) {
			return ((InstalledIUElement) mouseTarget).getProfile();
		}
		return null;
	}

	/**
	 * Returns the shell
	 */
	private Shell getShell() {
		return getViewer().getControl().getShell();
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
	public boolean performDrop(final Object data) {
		if (DEBUG) {
			System.out.println("Perform drop on target: " + getCurrentTarget() + " with data: " + data); //$NON-NLS-1$//$NON-NLS-2$
		}
		if (getCurrentTarget() == null || data == null)
			return false;

		ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
		if (!(selection instanceof IStructuredSelection) || selection.isEmpty())
			return false;

		Profile profile = getProfileTarget(getCurrentTarget());
		if (getCurrentOperation() == DND.DROP_COPY && profile != null) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			ISelectionProvider selectionProvider = new ISelectionProvider() {

				/* (non-Javadoc)
				 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
				 */
				public void addSelectionChangedListener(ISelectionChangedListener listener) {
					// Ignore because the selection won't change 
				}

				/* (non-Javadoc)
				 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
				 */
				public ISelection getSelection() {
					if (DEBUG) {
						System.out.println("Selection was queried by action"); //$NON-NLS-1$
						System.out.println(structuredSelection.toString());
					}
					return structuredSelection;
				}

				/* (non-Javadoc)
				 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
				 */
				public void removeSelectionChangedListener(ISelectionChangedListener listener) {
					// ignore because the selection is static
				}

				/* (non-Javadoc)
				 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
				 */
				public void setSelection(ISelection sel) {
					throw new UnsupportedOperationException("This ISelectionProvider is static, and cannot be modified."); //$NON-NLS-1$
				}
			};
			InstallAction action = new InstallAction(ProvUIMessages.Ops_InstallIUOperationLabel, selectionProvider, confirmer, profile, null, getShell());
			action.setEntryPointStrategy(entryPointStrategy);
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
	 * 
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#dragEnter(org.eclipse.swt.dnd.DropTargetEvent)
	 */
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
		List ius = new ArrayList();

		if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
			return null;
		}
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;

		Iterator iter = structuredSelection.iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = getIU(iter.next());
			if (iu != null) {
				ius.add(iu);
			}
		}
		return (IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]);
	}

	private IInstallableUnit getIU(Object obj) {
		if (obj instanceof IInstallableUnit) {
			return (IInstallableUnit) obj;
		}
		if (obj instanceof IAdaptable) {
			return (IInstallableUnit) ((IAdaptable) obj).getAdapter(IInstallableUnit.class);
		}
		return null;
	}

	public void setEntryPointStrategy(int strategy) {
		entryPointStrategy = strategy;
	}
}
