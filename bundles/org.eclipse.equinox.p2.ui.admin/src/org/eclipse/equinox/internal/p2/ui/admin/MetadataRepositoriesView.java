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
package org.eclipse.equinox.internal.p2.ui.admin;

import java.util.ArrayList;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddMetadataRepositoryDialog;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddProfileDialog;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.ui.IProfileChooser;
import org.eclipse.equinox.p2.ui.actions.BecomeAction;
import org.eclipse.equinox.p2.ui.actions.InstallAction;
import org.eclipse.equinox.p2.ui.model.*;
import org.eclipse.equinox.p2.ui.operations.RemoveMetadataRepositoryOperation;
import org.eclipse.equinox.p2.ui.viewers.IUDragAdapter;
import org.eclipse.equinox.p2.ui.viewers.ProvElementLabelProvider;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.part.PluginTransfer;

/**
 * This view allows users to interact with metadata repositories
 * 
 * @since 3.4
 */
public class MetadataRepositoriesView extends RepositoriesView {

	private InstallAction installAction;
	private BecomeAction becomeAction;

	/**
	 * The constructor.
	 */
	public MetadataRepositoriesView() {
		// constructor
	}

	protected IContentProvider getContentProvider() {
		return new MetadataRepositoryContentProvider();
	}

	protected Object getInput() {
		return new AllMetadataRepositories();
	}

	protected String getAddCommandLabel() {
		return ProvAdminUIMessages.MetadataRepositoriesView_AddRepositoryLabel;
	}

	protected String getAddCommandTooltip() {
		return ProvAdminUIMessages.MetadataRepositoriesView_AddRepositoryTooltip;
	}

	protected String getRemoveCommandLabel() {
		return ProvAdminUIMessages.MetadataRepositoriesView_RemoveRepositoryLabel;
	}

	protected String getRemoveCommandTooltip() {
		return ProvAdminUIMessages.MetadataRepositoriesView_RemoveRepositoryTooltip;
	}

	protected int openAddRepositoryDialog(Shell shell, Object[] elements) {
		return new AddMetadataRepositoryDialog(shell, (IMetadataRepository[]) elements).open();
	}

	protected IUndoableOperation getRemoveOperation(Object[] elements) {
		ArrayList repos = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IMetadataRepository) {
				repos.add(elements[i]);
			}
		}
		return new RemoveMetadataRepositoryOperation(ProvAdminUIMessages.MetadataRepositoriesView_RemoveRepositoryOperationLabel, (IMetadataRepository[]) repos.toArray(new IMetadataRepository[repos.size()]));
	}

	protected void makeActions() {
		super.makeActions();
		installAction = new InstallAction(viewer, null, getProfileChooser(), getShell());
		becomeAction = new BecomeAction(viewer, null, getProfileChooser(), getShell());
	}

	private IProfileChooser getProfileChooser() {
		return new IProfileChooser() {

			public Profile getProfile() {
				// TODO would be nice if the profile chooser dialog let you
				// create a new profile
				ProfileContentProvider provider = new ProfileContentProvider();
				if (provider.getElements(null).length == 0) {
					AddProfileDialog dialog = new AddProfileDialog(getShell(), new Profile[0]);
					if (dialog.open() == Window.OK) {
						return dialog.getAddedProfile();
					}
					return null;
				}

				ListDialog dialog = new ListDialog(getShell());
				dialog.setTitle(ProvAdminUIMessages.MetadataRepositoriesView_ChooseProfileDialogTitle);
				dialog.setLabelProvider(new ProvElementLabelProvider());
				dialog.setInput(new AllProfiles());
				dialog.setContentProvider(provider);
				dialog.open();
				Object[] result = dialog.getResult();
				if (result != null && result.length > 0 && result[0] instanceof Profile) {
					return (Profile) result[0];
				}
				return null;
			}

			public String getLabel() {
				return ProvAdminUIMessages.MetadataRepositoriesView_ChooseProfileDialogTitle;
			}
		};
	}

	protected void fillContextMenu(IMenuManager manager) {
		if (installAction.isEnabled()) {
			manager.add(new Separator());
			manager.add(installAction);
			manager.add(becomeAction);
		}
		super.fillContextMenu(manager);
	}

	protected boolean isRepository(Object element) {
		return element instanceof IMetadataRepository;
	}

	protected void configureViewer(final TreeViewer treeViewer) {
		super.configureViewer(treeViewer);
		// Add drag support for IU's
		Transfer[] transfers = new Transfer[] {org.eclipse.jface.util.LocalSelectionTransfer.getTransfer(), PluginTransfer.getInstance(), TextTransfer.getInstance(),};
		treeViewer.addDragSupport(DND.DROP_COPY, transfers, new IUDragAdapter(treeViewer));
	}

}