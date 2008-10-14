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

package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.ILayoutConstants;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.IUColumnConfig;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.about.IInstallationPageContainer;
import org.eclipse.ui.about.InstallationPage;
import org.eclipse.ui.menus.*;
import org.eclipse.ui.services.IServiceLocator;

/**
 * @since 3.4
 *
 */
public class InstalledSoftwarePage extends InstallationPage {

	private static final int DEFAULT_WIDTH = 300;
	private static final int DEFAULT_COLUMN_WIDTH = 150;
	IMenuService menuService;
	IInstallationPageContainer pageContainer;
	AbstractContributionFactory factory;
	Text detailsArea;
	InstalledIUGroup installedIUGroup;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.about.InstallationPage#init(org.eclipse.ui.services.IServiceLocator)
	 */
	public void init(IServiceLocator locator) {
		pageContainer = (IInstallationPageContainer) locator.getService(IInstallationPageContainer.class);
		menuService = (IMenuService) locator.getService(IMenuService.class);
		// this assumes that the control is created before init
		contributeButtonActions();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IProvHelpContextIds.INSTALLED_SOFTWARE);

		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = convertHorizontalDLUsToPixels(DEFAULT_WIDTH);
		composite.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);

		// Table of installed IU's
		installedIUGroup = new InstalledIUGroup(Policy.getDefault(), composite, JFaceResources.getDialogFont(), Policy.getDefault().getProfileChooser().getProfileId(ProvUI.getDefaultParentShell()), getColumnConfig());
		installedIUGroup.getStructuredViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateDetailsArea();
			}

		});

		Group group = new Group(composite, SWT.NONE);
		group.setText(ProvUIMessages.ProfileModificationWizardPage_DetailsLabel);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		group.setLayout(layout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.verticalIndent = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		gd.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_DESCRIPTION_HEIGHT);
		gd.widthHint = convertHorizontalDLUsToPixels(DEFAULT_WIDTH);

		detailsArea = new Text(group, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		detailsArea.setLayoutData(gd);

		setControl(composite);
	}

	private void contributeButtonActions() {
		if (pageContainer == null || menuService == null)
			return;

		final String profileId = Policy.getDefault().getProfileChooser().getProfileId(getShell());

		factory = new AbstractContributionFactory(pageContainer.getButtonBarURI(), null) {

			public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
				// For the update action, we create a custom selection provider that will interpret no
				// selection as checking for updates to everything.
				// We also override the run method to close the containing dialog
				// if we successfully try to resolve.  This is done to ensure that progress
				// is shown properly.
				// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=236495
				Action action = new UpdateAction(Policy.getDefault(), new ISelectionProvider() {
					public void addSelectionChangedListener(ISelectionChangedListener listener) {
						installedIUGroup.getStructuredViewer().addSelectionChangedListener(listener);
					}

					public ISelection getSelection() {
						StructuredViewer viewer = installedIUGroup.getStructuredViewer();
						ISelection selection = viewer.getSelection();
						if (selection.isEmpty()) {
							final Object[] all = ((IStructuredContentProvider) viewer.getContentProvider()).getElements(viewer.getInput());
							return new StructuredSelection(all);
						}
						return selection;
					}

					public void removeSelectionChangedListener(ISelectionChangedListener listener) {
						installedIUGroup.getStructuredViewer().removeSelectionChangedListener(listener);
					}

					public void setSelection(ISelection selection) {
						installedIUGroup.getStructuredViewer().setSelection(selection);
					}
				}, profileId, true) {
					public void run() {
						super.run();
						if (getReturnCode() == Window.OK)
							pageContainer.close();
					}
				};
				additions.addContributionItem(new ActionContributionItem(action), null);

				// Uninstall action
				action = new UninstallAction(Policy.getDefault(), installedIUGroup.getStructuredViewer(), profileId) {
					public void run() {
						super.run();
						if (getReturnCode() == Window.OK)
							pageContainer.close();
					}
				};
				additions.addContributionItem(new ActionContributionItem(action), null);

				// Properties action
				action = new PropertyDialogAction(new SameShellProvider(getShell()), installedIUGroup.getStructuredViewer());
				additions.addContributionItem(new ActionContributionItem(action), null);

				// Revert action
				// This might go away if the revert view becomes just another installation page
				action = new Action() {
					public void run() {
						RevertWizard wizard = new RevertWizard(profileId);
						WizardDialog dialog = new WizardDialog(getShell(), wizard);
						dialog.create();
						dialog.getShell().setSize(600, 500);
						PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IProvHelpContextIds.REVERT_CONFIGURATION_WIZARD);

						dialog.open();
					}
				};
				action.setText(ProvUIMessages.InstalledSoftwarePage_RevertLabel);
				action.setToolTipText(ProvUIMessages.InstalledSoftwarePage_RevertTooltip);
				additions.addContributionItem(new ActionContributionItem(action), null);
			}
		};
		menuService.addContributionFactory(factory);
	}

	void updateDetailsArea() {
		IInstallableUnit[] selected = installedIUGroup.getSelectedIUs();
		if (selected.length == 1) {
			String description = IUPropertyUtils.getIUProperty(selected[0], IInstallableUnit.PROP_DESCRIPTION);
			if (description != null) {
				detailsArea.setText(description);
				return;
			}
		}
		detailsArea.setText(""); //$NON-NLS-1$
	}

	public void dispose() {
		super.dispose();
		menuService.removeContributionFactory(factory);
	}

	private IUColumnConfig[] getColumnConfig() {
		int pixels = convertHorizontalDLUsToPixels(DEFAULT_COLUMN_WIDTH);
		return new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, pixels), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, pixels / 3), new IUColumnConfig(ProvUIMessages.ProvUI_IdColumnTitle, IUColumnConfig.COLUMN_ID, pixels * 2 / 3)};

	}
}
