/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Red Hat,Inc. - filter installed softwares
 *******************************************************************************/

package org.eclipse.equinox.p2.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.actions.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.misc.StringMatcher;
import org.eclipse.equinox.internal.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.about.InstallationPage;
import org.eclipse.ui.menus.AbstractContributionFactory;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * InstalledSoftwarePage displays a profile's IInstallableUnits in
 * an Installation Page.  Clients can use this class as the implementation
 * class for an installationPages extension.
 * 
 * @see InstallationPage
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.0
 *
 */
public class InstalledSoftwarePage extends InstallationPage implements ICopyable {

	private static final int UPDATE_ID = IDialogConstants.CLIENT_ID;
	private static final int UNINSTALL_ID = IDialogConstants.CLIENT_ID + 1;
	private static final int PROPERTIES_ID = IDialogConstants.CLIENT_ID + 2;
	private static final String BUTTON_ACTION = "org.eclipse.equinox.p2.ui.buttonAction"; //$NON-NLS-1$

	AbstractContributionFactory factory;
	Text detailsArea;
	InstalledIUGroup installedIUGroup;
	String profileId;
	Button updateButton, uninstallButton, propertiesButton;
	ProvisioningUI ui;

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IProvHelpContextIds.INSTALLED_SOFTWARE);

		profileId = getProvisioningUI().getProfileId();
		if (profileId == null) {
			IStatus status = getProvisioningUI().getPolicy().getNoProfileChosenStatus();
			if (status != null)
				ProvUI.reportStatus(status, StatusManager.LOG);
			Text text = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			text.setText(ProvUIMessages.InstalledSoftwarePage_NoProfile);
			setControl(text);
			return;
		}

		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		int width = getDefaultWidth(composite);
		gd.widthHint = width;
		composite.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		// Table of installed IU's
		installedIUGroup = new InstalledIUGroup(getProvisioningUI(), composite, JFaceResources.getDialogFont(), profileId, getColumnConfig());
		// we hook selection listeners on the viewer in createPageButtons because we
		// rely on the actions we create there getting selection events before we use
		// them to update button enablement.

		CopyUtils.activateCopy(this, installedIUGroup.getStructuredViewer().getControl());

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_DESCRIPTION_HEIGHT);
		gd.widthHint = width;

		detailsArea = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		detailsArea.setBackground(detailsArea.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		detailsArea.setLayoutData(gd);

		setControl(composite);
	}

	@Override
	public void createPageButtons(Composite parent) {
		if (profileId == null)
			return;
		// For the update action, we create a custom selection provider that will interpret no
		// selection as checking for updates to everything.
		// We also override the run method to close the containing dialog
		// if we successfully try to resolve.  This is done to ensure that progress
		// is shown properly.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=236495
		UpdateAction updateAction = new UpdateAction(getProvisioningUI(), new ISelectionProvider() {
			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				installedIUGroup.getStructuredViewer().addSelectionChangedListener(listener);
			}

			@Override
			public ISelection getSelection() {
				StructuredViewer viewer = installedIUGroup.getStructuredViewer();
				ISelection selection = viewer.getSelection();
				if (selection.isEmpty()) {
					final Object[] all = ((IStructuredContentProvider) viewer.getContentProvider()).getElements(viewer.getInput());
					return new StructuredSelection(all);
				}
				return selection;
			}

			@Override
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				installedIUGroup.getStructuredViewer().removeSelectionChangedListener(listener);
			}

			@Override
			public void setSelection(ISelection selection) {
				installedIUGroup.getStructuredViewer().setSelection(selection);
			}
		}, profileId, true) {
			@Override
			public void run() {
				super.run();
				if (getReturnCode() == Window.OK)
					getPageContainer().closeModalContainers();
			}
		};
		updateAction.setSkipSelectionPage(true);
		updateButton = createButton(parent, UPDATE_ID, updateAction.getText());
		updateButton.setData(BUTTON_ACTION, updateAction);
		// Uninstall action
		Action uninstallAction = new UninstallAction(getProvisioningUI(), installedIUGroup.getStructuredViewer(), profileId) {
			@Override
			public void run() {
				super.run();
				if (getReturnCode() == Window.OK)
					getPageContainer().closeModalContainers();
			}
		};
		uninstallButton = createButton(parent, UNINSTALL_ID, uninstallAction.getText());
		uninstallButton.setData(BUTTON_ACTION, uninstallAction);

		// Properties action
		PropertyDialogAction action = new PropertyDialogAction(new SameShellProvider(getShell()), installedIUGroup.getStructuredViewer());
		propertiesButton = createButton(parent, PROPERTIES_ID, action.getText());
		propertiesButton.setData(BUTTON_ACTION, action);

		// We rely on the actions getting selection events before we do, because
		// we rely on the enablement state of the action.  So we don't hook
		// the selection listener on our table until after actions are created.
		installedIUGroup.getStructuredViewer().addSelectionChangedListener(event -> {
			updateDetailsArea();
			updateEnablement();
		});

		final IUPatternFilter searchFilter = new IUPatternFilter(getColumnConfig());
		installedIUGroup.getStructuredViewer().addFilter(searchFilter);

		updateEnablement();
	}

	void updateDetailsArea() {
		java.util.List<IInstallableUnit> selected = installedIUGroup.getSelectedIUs();
		if (selected.size() == 1) {
			String description = selected.get(0).getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
			if (description != null) {
				detailsArea.setText(description);
				return;
			}
		}
		detailsArea.setText(""); //$NON-NLS-1$
	}

	void updateEnablement() {
		if (updateButton == null || updateButton.isDisposed())
			return;
		Button[] buttons = {updateButton, uninstallButton, propertiesButton};
		for (Button button : buttons) {
			Action action = (Action) button.getData(BUTTON_ACTION);
			if (action == null || !action.isEnabled())
				button.setEnabled(false);
			else
				button.setEnabled(true);
		}
	}

	private IUColumnConfig[] getColumnConfig() {
		return new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_IdColumnTitle, IUColumnConfig.COLUMN_ID, ILayoutConstants.DEFAULT_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_ProviderColumnTitle, IUColumnConfig.COLUMN_PROVIDER, ILayoutConstants.DEFAULT_COLUMN_WIDTH)};
	}

	private int getDefaultWidth(Control control) {
		IUColumnConfig[] columns = getColumnConfig();
		int totalWidth = 0;
		for (IUColumnConfig column : columns) {
			totalWidth += column.getWidthInPixels(control);
		}
		return totalWidth + 20; // buffer for surrounding composites
	}

	@Override
	public void copyToClipboard(Control activeControl) {
		Object[] elements = installedIUGroup.getSelectedIUElements();
		if (elements.length == 0)
			return;
		String text = CopyUtils.getIndentedClipboardText(elements, new IUDetailsLabelProvider(null, getColumnConfig(), null));
		Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
		clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
			case UPDATE_ID :
				((Action) updateButton.getData(BUTTON_ACTION)).run();
				break;
			case UNINSTALL_ID :
				((Action) uninstallButton.getData(BUTTON_ACTION)).run();
				break;
			case PROPERTIES_ID :
				((Action) propertiesButton.getData(BUTTON_ACTION)).run();
				break;
			default :
				super.buttonPressed(buttonId);
				break;
		}
	}

	ProvisioningUI getProvisioningUI() {
		// if a UI has not been set then assume that the current default UI is the right thing
		if (ui == null)
			return ui = ProvisioningUI.getDefaultUI();
		return ui;
	}

	/**
	 * Set the provisioning UI to use with this page
	 * 
	 * @param value the provisioning ui to use
	 * @since 2.1
	 */
	public void setProvisioningUI(ProvisioningUI value) {
		ui = value;
	}

	/**
	 * Filters {@link InstalledIUElement}s from a viewer using a String pattern. 
	 * Filtering is dependent on a given array of {@link IUColumnConfig}s :
	 * <ul>
	 * <li>if {@link IUColumnConfig#COLUMN_ID} is present, filters on the Installable Unit's id;</li>
	 * <li>if {@link IUColumnConfig#COLUMN_NAME} is present, filters on the Installable Unit's  name;</li>
	 * <li>if {@link IUColumnConfig#COLUMN_PROVIDER} is present, filters on the Installable Unit's provider;</li>
	 * </ul>   
	 * 
	 * @since 2.3
	 */
	class IUPatternFilter extends ViewerFilter {

		private StringMatcher matcher;

		private boolean filterOnId;

		private boolean filterOnName;

		private boolean filterOnProvider;

		public IUPatternFilter() {
			this(null);
		}

		public IUPatternFilter(IUColumnConfig[] columnConfig) {
			if (columnConfig == null) {
				columnConfig = ProvUI.getIUColumnConfig();
			}
			for (IUColumnConfig colConfig : columnConfig) {
				switch (colConfig.getColumnType()) {
					case IUColumnConfig.COLUMN_ID :
						filterOnId = true;
						break;
					case IUColumnConfig.COLUMN_NAME :
						filterOnName = true;
						break;
					case IUColumnConfig.COLUMN_PROVIDER :
						filterOnProvider = true;
						break;
					default :
						break;
				}
				if (filterOnId && filterOnName && filterOnProvider) {
					break;
				}
			}
		}

		public void setPattern(String searchPattern) {
			if (searchPattern == null || searchPattern.length() == 0) {
				this.matcher = null;
			} else {
				String pattern = "*" + searchPattern + "*"; //$NON-NLS-1$//$NON-NLS-2$
				this.matcher = new StringMatcher(pattern, true, false);
			}
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (matcher == null || !(filterOnName || filterOnId || filterOnProvider)) {
				return true;
			}

			if (element instanceof InstalledIUElement) {
				InstalledIUElement data = (InstalledIUElement) element;
				IInstallableUnit iu = data.getIU();
				boolean match = false;
				if (iu != null) {
					if (filterOnName) {
						String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
						match = matcher.match(name);
					}
					if (!match && filterOnId) {
						match = matcher.match(iu.getId());
					}
					if (!match && filterOnProvider) {
						String provider = iu.getProperty(IInstallableUnit.PROP_PROVIDER, null);
						match = matcher.match(provider);
					}
				}
				return match;
			}
			return true;
		}
	}
}
